/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.List;
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
        while(following.size() > 0) {
            if(following.size() > 1)
                log.warn("Session " + si.getSession().getMat() + " has more than one following session");

            Session s = following.iterator().next();
            
            if(s.getLockedStatus() != Session.LockedStatus.FIGHTS_LOCKED)
                break;
            
            List<Fight> unplayed = database.findAll(Fight.class, FightDAO.UNPLAYED_IN_SESSION, s.getID());

            while(unplayed.size() > 0 && fights.size() < numFights) {
                isByeOrEmpty = false;
                for(int i = 0; i < 2; i++) {
                    String code = unplayed.get(0).getPlayerCodes()[i];
                    FightPlayer fp = PlayerCodeParser.parseCode(database, code, unplayed.get(0).getPoolID());
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
