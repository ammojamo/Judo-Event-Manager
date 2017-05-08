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
