/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.model.info.SessionFightInfo;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionFightSequencer {
    private static final Logger log = Logger.getLogger(SessionFightSequencer.class);

    private SessionFightSequencer() {}
    
    public static void nPassAutoOrder(Database database, List<SessionFightInfo> fights, int spacing) {
        while(spacing >= 1) {
            autoOrder(database, fights, spacing, false);
            autoOrder(database, fights, spacing, true);
            autoOrder(database, fights, spacing, false);
            spacing--;
        }
    }
    
    public static void autoOrder(Database database, List<SessionFightInfo> fights, int spacing, boolean up) {
        Collection<Integer> checkedPools = new ArrayList<Integer>();

        log.debug("Auto-order: " + (up?"UP":"DOWN") + ", spacing " + spacing);
        int n = fights.size();
        for(int i = 1; i < n; i++) {

            checkedPools.clear();

            for(int j = i; j < n; j++) {

                Fight f1 = fights.get(up?n-j-1:j).getFight();
                if(checkedPools.contains(f1.getPoolID()))
                    continue;

                boolean positionOk = true;

                for(int k = 1; k <= spacing && i-k >= 0; k++) {

                    Fight f2 = fights.get(up?n-(i-k)-1:i-k).getFight();

                    if(f1.getPoolID() == f2.getPoolID() &&
                       PoolPlayerSequencer.hasCommonPlayers(database, f1.getPoolID(), new int[] { f1.getPosition(), f2.getPosition() })) {
                        // means that fight f1 is NOT in an ok position
                        positionOk = false;
                        break;
                    }

                }

                if(positionOk) {
                    if(j != i) {
                        if(!up)
                            fights.add(i, fights.remove(j));
                        else
                            fights.add((n-i-1), fights.remove(n-j-1));
                    }
                    break;
                }

                checkedPools.add(f1.getPoolID());
            }
        }
        
        fixPositions(fights);
    }
    
    public static void resetOrder(List<SessionFightInfo> fights) {
        Collections.sort(fights, new Comparator<SessionFightInfo>() {
            @Override
            public int compare(SessionFightInfo sf1, SessionFightInfo sf2) {
                Fight f1 = sf1.getFight();
                Fight f2 = sf2.getFight();
                
                if(f1.getPoolID() == f2.getPoolID())
                    return f1.getPosition() - f2.getPosition();
                else
                    return f1.getPoolID() - f2.getPoolID();
            }            
        });
        
        fixPositions(fights);
    }
    
    /**
     * Moves the fight at the specified index up, ensuring that pool fight order
     * is preserved. Moving a fight into adjacent spaces occupied by fights of
     * the same pool will cause the adjacent fight(s) to be moved also, if
     * possible.
     * 
     * @param fights    The list of fights for a session
     * @param index     The index of the fight to be moed
     * @return          The number of fights moved
     */
    public static int moveFightUp(List<SessionFightInfo> fights, int index) {
        // range of fights to move
        int rangeStart = index;
        int rangeEnd = index;
        
        while(rangeStart > 0) {
            
            if(fights.get(rangeStart).getFight().getPoolID() == fights.get(rangeStart-1).getFight().getPoolID())
                rangeStart--;
            else {
                fights.add(rangeEnd, fights.remove(rangeStart-1));
                fixPositions(fights);
                return rangeEnd - rangeStart + 1;
            }
        }
        
        return 0; // cannot move up, already at top
    }

    public static int moveFightDown(List<SessionFightInfo> fights, int index) {
        // range of fights to move
        int rangeStart = index;
        int rangeEnd = index;
        
        while(rangeEnd < fights.size()-1) {
            
            if(fights.get(rangeEnd).getFight().getPoolID() == fights.get(rangeEnd+1).getFight().getPoolID())
                rangeEnd++;
            else {
                fights.add(rangeStart, fights.remove(rangeEnd+1));
                fixPositions(fights);
                return rangeEnd - rangeStart + 1;
            }
        }
        
        return 0; // cannot move up, already at top
    }
    
    public static void deferFight(final TransactionalDatabase database, SessionFightInfo sfi) throws DatabaseStateException {
        Session session = sfi.getSession();
        SessionInfo si = new SessionInfo(database, session);
        final SessionFight sf = sfi.getSessionFight();

        if(si.getFollowingDependentSessions().size() > 1)
            throw new DatabaseStateException("Session " + session.getName() + " has more than one following session.\nThis must be resolved before continuing");

        if(si.getFollowingDependentSessions().size() < 1)
            throw new DatabaseStateException("To defer fights from this session, a following session must be defined and LOCKED using the Sessions interface");

        final Session following = si.getFollowingDependentSessions().iterator().next();
        
        if(following.getLockedStatus() != Session.LockedStatus.POSITION_LOCKED)
            throw new DatabaseStateException("To defer fights from this session, the following session must be locked using the Sessions interface.");

        final List<SessionFightInfo> fights = SessionFightSequencer.getFightSequence(database, session.getID());
        final List<SessionFightInfo> followingFights = SessionFightSequencer.getFightSequence(database, following.getID());

        database.perform(new Transaction() {
            @Override
            public void perform() {
                int pos = sf.getPosition();
             
                SessionFightInfo sfi = fights.get(pos-1);
                int poolID = sfi.getFight().getPoolID();

                for(int i = fights.size(); i >= pos; i--) {
                    sfi = fights.get(i-1);
                    if(sfi.getFight().getPoolID() == poolID) {
                        log.debug("Deferring fight " + sfi.getFight().getPosition() + " from pool " + sfi.getFight().getPoolID() + ".");

                        fights.remove(i-1);
                        database.delete(sfi.getSessionFight());
                        sfi.getSessionFight().setID(new SessionFight.Key(following.getID(), sfi.getSessionFight().getFightID()));
                        sfi.getSessionFight().setValid(true);
                        sfi.getSessionFight().setPosition(-1);
                        database.add(sfi.getSessionFight());
                        followingFights.add(0, sfi);
                    }
                }
            }
        });

        SessionFightSequencer.saveFightSequence(database, fights, true);
        SessionFightSequencer.saveFightSequence(database, followingFights, true);
    }
    
    /**
     * Un-defers the fight specified from it's current session to the last session
     * from which it was deferred. If this fight has not been deferred, or the
     * session from which it was deferred is now locked, a DatabaseStateException
     * is thrown.
     * 
     * @param database      The database on which to operate
     * @param sfi           The fight to be un-deferred
     * @throws DatabaseStateException if the fight is not eligible to be un-deferred
     */
    public static void undeferFight(final TransactionalDatabase database, final SessionFightInfo sfi) throws DatabaseStateException {
        SessionInfo si = new SessionInfo(database, sfi.getSession());
        final int poolID = sfi.getFight().getPoolID();

        for(Pool pool : si.getPools())
            if(pool.getID() == poolID) {
                throw new DatabaseStateException("This fight has not been deferred from a previous session");
            }
        
        Session deferredSession = null;
        for(Session preceding : si.getPrecedingDependentSessions()) {
            if(SessionLinker.mayContainPool(database, preceding.getID(), poolID))
                deferredSession = preceding;
        }

        if(deferredSession == null)
            throw new DatabaseStateException("Could not find preceding session");
        
        if(deferredSession.getLockedStatus() == Session.LockedStatus.FIGHTS_LOCKED)
            throw new DatabaseStateException("The fights in the preceding session have been locked");

        final List<SessionFightInfo> fights = SessionFightSequencer.getFightSequence(database, si.getSession().getID());
        final List<SessionFightInfo> precedingFights = SessionFightSequencer.getFightSequence(database, deferredSession.getID());

        final Session ds = deferredSession;
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                int selectedFightID = sfi.getFight().getID();
                int i = 1;
                while(true) {
                    SessionFightInfo sfi = fights.get(i-1);
                    if(sfi.getFight().getPoolID() == poolID) {
                        fights.remove(i-1);
                        database.delete(sfi.getSessionFight());
                        sfi.getSessionFight().setID(new SessionFight.Key(ds.getID(), sfi.getSessionFight().getFightID()));
                        sfi.getSessionFight().setValid(true);
                        sfi.getSessionFight().setPosition(precedingFights.size()+1);
                        database.add(sfi.getSessionFight());
                        precedingFights.add(sfi);
                        if(sfi.getFight().getID() == selectedFightID) break;
                    } else i++;
                }

            }
        });
        SessionFightSequencer.saveFightSequence(database, fights, true);
                //SessionFightSequencer.saveFightSequence(database, precedingFights, true);                
    }

    
    public static List<SessionFightInfo> getFightSequence(Database database, int sessionID) {
        Session session = database.get(Session.class, sessionID);
        List<SessionFightInfo> fights = new ArrayList<SessionFightInfo>(SessionFightInfo.getForSession(database, session));

        Collections.sort(fights, new Comparator<SessionFightInfo> () {
            @Override
            public int compare(SessionFightInfo sf1, SessionFightInfo sf2) {
                return sf1.getSessionFight().getPosition() - sf2.getSessionFight().getPosition();
            }
        });
        return fights;
    }

    public static void saveFightSequence(final TransactionalDatabase database, final List<SessionFightInfo> fights, final boolean updateAll)  throws DatabaseStateException {
        if(fights.size() > 0 && fights.iterator().next().getSession().getLockedStatus() == Session.LockedStatus.FIGHTS_LOCKED)
            throw new DatabaseStateException("Cannot update fight order for a session with locked fights");
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                int pos = 0;
                for(SessionFightInfo fight : fights) {
                    pos++;
                    SessionFight sf = fight.getSessionFight();
                    if(updateAll || sf.getPosition() != pos) {
                        sf.setPosition(pos);
                        database.update(sf);
                    }
                }
            }
        });
    }
    
    private static void fixPositions(List<SessionFightInfo> fights) {
        int pos = 1;
        for(SessionFightInfo sfi : fights) {
            sfi.getSessionFight().setPosition(pos++);
        }
    }
    
    public static class FightMatInfo {
        public int fightNumber;
        public String matName;
    }
    
    public static FightMatInfo getFightMatInfo(Database database, SessionFight sf) {
        FightMatInfo info = getSessionFirstFightMatInfo(database, sf.getSessionID());
        info.fightNumber += sf.getPosition() - 1;
        /*
        Session s = database.get(Session.class, sf.getSessionID());
        SessionInfo si = new SessionInfo(database, s);
        while(!si.getPrecedingMatSessions().isEmpty()) {
            s = si.getPrecedingMatSessions().iterator().next();
            pos += getFightSequence(database, s.getID()).size();
            si = new SessionInfo(database, s);
        }

        pos += getFirstFightNumberInSession(database, sf.getSessionID());
        FightMatInfo info = new FightMatInfo();
        info.fightNumber = pos;
        info.matName = s.getMat();
         * */
        return info;
    }

    public static FightMatInfo getSessionFirstFightMatInfo(Database database, int sessionID) {
        int pos = 1;
        Session s = database.get(Session.class, sessionID);
        SessionInfo si = new SessionInfo(database, s);
        while(!si.getPrecedingMatSessions().isEmpty()) {
            s = si.getPrecedingMatSessions().iterator().next();
            pos += getFightSequence(database, s.getID()).size();
            si = new SessionInfo(database, s);
        }
        FightMatInfo info = new FightMatInfo();
        info.fightNumber = pos;
        info.matName = s.getMat();
        return info;
    }
}
