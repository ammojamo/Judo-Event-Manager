/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager;

import au.com.jwatmuff.genericdb.p2p.Update;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DumpUpdates {
    private static final Logger log = Logger.getLogger(DumpUpdates.class);

    private static Update loadUpdatesFromFile(String updateFileName) {
        try {
            File updateFile = new File(updateFileName);
            FileInputStream fis = new FileInputStream(updateFile);
            ObjectInputStream reader = new ObjectInputStream(fis);
            Update loadedUpdate = (Update)reader.readObject();
            reader.close();
            fis.close();
            return loadedUpdate;
        } catch (ClassNotFoundException cnfe) {
            log.error("Couldn't deserialize update object - class unknown", cnfe);
        } catch (FileNotFoundException fnfe) {
            log.info("Updates file not found");
        } catch (IOException ioe) {
            log.error("Input/output error while loading updates file", ioe);
        }

        return null;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Usage: DumpUpdates <updatefile> <outputfile>");
            System.exit(0);
        }

        Update update = loadUpdatesFromFile(args[0]);
        if(update == null)
            System.exit(0);

        try {
            File f = new File(args[1]);
            PrintStream out = new PrintStream(new FileOutputStream(f));
            out.print(update.dumpTable());
        } catch(Exception e) {
            log.error("Error writing to file", e);
        }
    }

}
