/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerTimer {
    private static final Logger log = Logger.getLogger(PlayerTimer.class);

    public static Timestamp lastFightTime(int playerID, Database database) {
        Collection<Result> results = database.findAll(Result.class, ResultDAO.FOR_PLAYER, playerID);

        if(results.size() == 0) return null;
        Result r = Collections.max(results, new Comparator<Result> () {
            @Override
            public int compare(Result o1, Result o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });

        log.debug("Last fight time for player with ID " + playerID + " is " + r.getTimestamp());

        return r.getTimestamp();
    }
}
