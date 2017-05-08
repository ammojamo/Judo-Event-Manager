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

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ResultRecorder {
    private static final Logger log = Logger.getLogger(ResultRecorder.class);
    
    /**
     * Records the given result into the database
     * 
     * @param database  The database to update
     * @param r         The result to be recorded
     */
    public static void recordResult(Database database, Result r) {
        log.debug("Recording result for player IDs " + r.getPlayerIDs()[0] + " and " + r.getPlayerIDs()[1]);

        database.add(r);
    }
}
