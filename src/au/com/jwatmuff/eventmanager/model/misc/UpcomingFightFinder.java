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

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class UpcomingFightFinder {
    private static final Logger log = Logger.getLogger(UpcomingFightFinder.class);
    
    public static List<Fight> findUpcomingFights(Database database, int sessionID, int numFights) throws DatabaseStateException {
        List<Fight> fights = new ArrayList<Fight>();
        boolean isByeOrEmpty = false;
        
        Session session = database.get(Session.class, sessionID);
        if(session == null || session.getType() != Session.SessionType.MAT)
            throw new DatabaseStateException("Given session ID does not correspond to a valid Mat session in the database");
        
        SessionInfo si = new SessionInfo(database, session);
        
        Collection<Session> following = si.getFollowingMatSessions();
        Map<Integer,PlayerCodeParser> playerCodeParser = new HashMap<Integer,PlayerCodeParser>();
        while(following.size() > 0) {
            if(following.size() > 1)
                log.warn("Session " + si.getSession().getMat() + " has more than one following session");

            Session s = following.iterator().next();
            
            if(s.getLockedStatus() != Session.LockedStatus.FIGHTS_LOCKED)
                break;
            
            List<Fight> unplayed = database.findAll(Fight.class, FightDAO.UNPLAYED_IN_SESSION, s.getID());

            while(unplayed.size() > 0 && fights.size() < numFights) {
                if(!playerCodeParser.containsKey(unplayed.get(0).getPoolID())){
                    playerCodeParser.put(unplayed.get(0).getPoolID(), PlayerCodeParser.getInstance(database, unplayed.get(0).getPoolID()));
                }
                isByeOrEmpty = false;
                for(int i = 0; i < 2; i++) {
                    String code = unplayed.get(0).getPlayerCodes()[i];
                    FightPlayer fp = playerCodeParser.get(unplayed.get(0).getPoolID()).parseCode(code);
                    if(fp.type == PlayerType.BYE || fp.type == PlayerType.EMPTY){
                        isByeOrEmpty = true;
                    }
                }
                if(isByeOrEmpty) {
                    unplayed.remove(0);
                } else {
                    fights.add(unplayed.remove(0));
                }
            }
            
            if(fights.size() == numFights) break;

            si = new SessionInfo(database, s);
            following = si.getFollowingMatSessions();
        }

        return fights;
    }
}
