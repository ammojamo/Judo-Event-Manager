/*
 * ResultRecorder.java
 *
 * Created on 28 August 2008, 20:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
