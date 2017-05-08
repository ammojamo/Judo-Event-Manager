/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.jwatmuff.eventmanager;

import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.p2p.Update;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 * java -cp dist/EventManager.jar au.com.jwatmuff.eventmanager.DumpUpdates  "[update file]" "[output file]"
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
            
            Map<DataEvent,UUID> uuidMap = new HashMap<>();
            
            for(DataEvent event : update.getAllEventsOrdered(uuidMap)) {
                UUID id = uuidMap.get(event);
                out.println(event.getTimestamp().getTime() + ": " + id.toString().substring(0, 5) + " " + event.getTransactionStatus() + " " + event.getType() + " " + event.getDataClass() + " " + event.getData().getID());
            }
//            out.print(update.dumpTable());
        } catch(Exception e) {
            log.error("Error writing to file", e);
        }
    }

}
