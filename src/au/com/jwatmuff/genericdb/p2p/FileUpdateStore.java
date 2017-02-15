/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericdb.p2p;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

/**
 * Proof-of-concept only - not battle tested!
 * 
 * @author james
 */
public class FileUpdateStore implements UpdateStore {
    private static final Logger log = Logger.getLogger(FileUpdateStore.class);
    private final File file;
    private ObjectOutputStream writer = null;
    private FileOutputStream fos = null;
    private UpdatePosition committedPosition = new UpdatePosition();

    public static FileUpdateStore withFile(File file) {
        return new FileUpdateStore(file);
    }
            
    private FileUpdateStore(File file) {
        this.file = file;
    }
    
    private ObjectOutputStream writer() throws IOException {
        if(writer == null) {
            // Appending to an existing file is a bit different, because we need
            // to avoid re-writing file header and reset serialization state.
            if(file.exists() && file.length() > 0) {
                fos = new FileOutputStream(file, true);
                writer = new AppendingObjectOutputStream(fos);
            } else {
                fos = new FileOutputStream(file);
                writer = new ObjectOutputStream(fos);
            }
        }
        return writer;
    }
    
    @Override
    public Update loadUpdate() throws IOException {
        Update update = new Update();

        try (
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream reader = new ObjectInputStream(fis)
        ) {
            log.debug("Loading update");
            while(true) {
                try {
                    Object object = reader.readObject();
                    if(object instanceof Update) {
                        log.debug(object);
                        update.mergeWith((Update)object);
                    }
                    if(object instanceof UpdatePosition) {
                        log.debug(object);
                        committedPosition = (UpdatePosition)object;
                    }
                } catch(EOFException e) {
                    // end of file - expected
                    break;
                } catch (ClassNotFoundException cnfe) {
                    log.error("Couldn't deserialize update object - class unknown", cnfe);
                }
            }
            log.debug("Finished update load");
        
            
        } catch (FileNotFoundException fnfe) {
            log.info("Updates file not found");
        } catch (IOException ioe) {
            log.error("Input/output error while loading updates file", ioe);
        }

        return update;
    }

    @Override
    public void writePartialUpdate(Update update) throws IOException {
        writeObjectAndSync(update);
    }

    @Override
    public void writeCommitedPosition(UpdatePosition position) throws IOException {
        committedPosition = position;
        writeObjectAndSync(position);
    }
    
    private void writeObjectAndSync(Object object) throws IOException {
        writer().writeObject(object);
        writer().flush();
        fos.getFD().sync();
    }

    @Override
    public UpdatePosition getCommittedPosition() {
        return committedPosition;
    }
}

// http://stackoverflow.com/a/1195078/165783
class AppendingObjectOutputStream extends ObjectOutputStream {
    public AppendingObjectOutputStream(OutputStream out) throws IOException {
      super(out);
    }

    @Override
    protected void writeStreamHeader() throws IOException {
      // do not write a header, but reset:
      // this line added after another question
      // showed a problem with the original
      reset();
    }
}
