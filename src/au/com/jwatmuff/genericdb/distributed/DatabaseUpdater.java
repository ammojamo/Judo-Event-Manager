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

        Clock.setEarliestTime(event.getTimestamp());

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
