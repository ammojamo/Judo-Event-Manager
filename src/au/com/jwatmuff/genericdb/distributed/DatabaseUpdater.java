/*
 * DatabaseUpdater.java
 *
 * Created on 12 August 2008, 01:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import au.com.jwatmuff.genericdb.Database;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DatabaseUpdater implements DataEventListener {
    private static final Logger log = Logger.getLogger(DatabaseUpdater.class);
    
    private Database database;

    /** Creates a new instance of DatabaseUpdater */
    public DatabaseUpdater(Database database) {
        this.database = database;
    }

    @Override
    public void handleDataEvent(DataEvent event) {
        Distributable data = event.getData();
        log.debug(event.getTimestamp() + ": " + event.getType().toString() + " " + event.getDataClass().getSimpleName() + " (" + data.getID() + ")");
        switch(event.getType()) {
            case DELETE:
                data.setValid(false);
            case CREATE:
            case UPDATE:
                Distributable current = (Distributable)database.get(data.getClass(), data.getID());
                if(current == null)
                    database.add(data);
                else if(current.getTimestamp().before(data.getTimestamp()))
                    database.update(data);
                break;
        }
    }
}
