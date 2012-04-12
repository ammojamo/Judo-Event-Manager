/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.model.info.SessionFightInfo;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionFightSequencer {
    private static final Logger log = Logger.getLogger(SessionFightSequencer.class);

    public static final Comparator<SessionFightInfo> Fight_COMPARATOR = new Comparator<SessionFightInfo>() {
        @Override
        public int compare(SessionFightInfo sf1, SessionFightInfo sf2) {
            Fight f1 = sf1.getFight();
            Fight f2 = sf2.getFight();

            if(f1.getPoolID() == f2.getPoolID())
                return f1.getPosition() - f2.getPosition();
            else
                return f1.getPoolID() - f2.getPoolID();
        }
    };

    public static final Comparator<Pool> POOL_COMPARATOR = new Comparator<Pool>() {
        @Override
        public int compare(Pool p1, Pool p2) {
            if(p1.getMaximumAge() == p2.getMaximumAge()){
                if(p2.getGender().equals(p1.getGender())){
                    if(p1.getMaximumWeight() == p2.getMaximumWeight()){
                        if(p1.getMinimumWeight() == p2.getMinimumWeight()){
                            return  p1.getDescription().compareTo(p2.getDescription());
                        } else {
                            if(p1.getMinimumWeight() == 0) {
                                return 1;
                            } else if(p2.getMinimumWeight() == 0) {
                                return -1;
                            } else {
                                return -Double.compare(p1.getMinimumWeight(), p2.getMinimumWeight());
                            }
                        }
                    } else {
                        if(p1.getMaximumWeight() == 0) {
                            return 1;
                        } else if(p2.getMaximumWeight() == 0) {
                            return -1;
                        } else {
                            return Double.compare(p1.getMaximumWeight(), p2.getMaximumWeight());
                        }
                    }
                } else {
                    return  p2.getGender().compareTo(p1.getGender());
                }
            } else {
                if(p1.getMaximumAge() == 0) {
                    return 1;
                } else if(p2.getMaximumAge() == 0) {
                    return -1;
                } else {
                    return p1.getMaximumAge() - p2.getMaximumAge();
                }
            }
        }
    };

    private SessionFightSequencer() {}
    
    public static void nPassAutoOrder(Database database, List<SessionFightInfo> fights, int spacing) {

        int n = fights.size();
        Map<SessionFightInfo,ArrayList<Player>> possiblePlayersInFights = new HashMap<SessionFightInfo,ArrayList<Player>>();
        Map<Integer,PlayerCodeParser> playerCodeParsers = new HashMap<Integer,PlayerCodeParser>();
        for(int i = 0; i < n; i++) {
            SessionFightInfo sFI = fights.get(i);
            Fight f = fights.get(i).getFight();
            if(!playerCodeParsers.containsKey(f.getPoolID())){
                playerCodeParsers.put(f.getPoolID(), PlayerCodeParser.getInstance(database, f.getPoolID()));
            }
            ArrayList<Player> possiblePlayersF = playerCodeParsers.get(f.getPoolID()).getPossiblePlayers(f.getPosition());
            possiblePlayersInFights.put(sFI,possiblePlayersF);
        }

        finishedFightsReset(fights);

        while(spacing >= 1) {
            autoOrder(fights, playerCodeParsers, possiblePlayersInFights, spacing, false);
            autoOrder(fights, playerCodeParsers, possiblePlayersInFights, spacing, true);
            autoOrder(fights, playerCodeParsers, possiblePlayersInFights, spacing, false);
            spacing--;
        }
    }

    public static void finishedFightsReset(List<SessionFightInfo> fights) {

        log.debug("Auto-order: Moving fights with result to the top.");

        int n = fights.size();
        int i = 0;
        for(int j = 1; j < n; j++) {
            SessionFightInfo sFI1 = fights.get(j);
            if(sFI1.resultKnown()){
                fights.add(i, fights.remove(j));
                i++;
            }
        }
        fixPositions(fights);
    }
    
    public static void autoOrder(List<SessionFightInfo> fights, Map<Integer,PlayerCodeParser> playerCodeParsers, Map<SessionFightInfo,ArrayList<Player>> possiblePlayersInFights, int spacing, boolean up) {

        log.debug("Auto-order: " + (up?"UP":"DOWN") + ", spacing " + spacing);

        int n = fights.size();
        int spacingAfterTie = 0;
        for(int i = 1; i < n; i++) {

            for(int j = i; j < n; j++) {

                SessionFightInfo sFI1 = fights.get(up?n-j-1:j);
                Fight f1 = sFI1.getFight();

                boolean positionOk = true;
                if(!sFI1.resultKnown()){

    // Could the two fights possibly share the same player even if from a different division.
                    spacingAfterTie = 0;
                    pPCheck:
                    for(int k = 1; k <= (spacing+spacingAfterTie) && i-k >= 0; k++) {
                        SessionFightInfo sFI2 = fights.get(up?n-(i-k)-1:i-k);
                        Fight f2 = sFI2.getFight();
                        if(PlayerCodeParser.isTieBreak(f2.getPlayerCodes()[0]) || PlayerCodeParser.isTieBreak(f2.getPlayerCodes()[1]))
                            spacingAfterTie = spacingAfterTie + 1;
                        ArrayList<Player> possiblePlayersF1 = possiblePlayersInFights.get(sFI1);
                        ArrayList<Player> possiblePlayersF2 = possiblePlayersInFights.get(sFI2);
                        for(Player player1 : possiblePlayersF1){
                            for(Player player2 : possiblePlayersF2){
                                if(player1.getVisibleID().equals(player2.getVisibleID())){
                                    // means that fight at position j could have a player in it between i-spacing and i-1
                                    positionOk = false;
                                    break pPCheck;
                                }
                            }
                        }
                    }

    // If F1 is moved to i, two divisions the player is in be mixed up.
                    if(positionOk) {
                        spacingAfterTie = 0;
                        pPCheck:
                        for(int l = j-1; l >= i && l >= 0; l--) {
                            SessionFightInfo sFI2 = fights.get(up?n-l-1:l);
                            Fight f2 = sFI2.getFight();
                            if(f1.getPoolID() != f2.getPoolID()){
                                ArrayList<Player> possiblePlayersF1 = possiblePlayersInFights.get(sFI1);
                                ArrayList<Player> possiblePlayersF2 = possiblePlayersInFights.get(sFI2);
                                for(Player player1 : possiblePlayersF1){
                                    for(Player player2 : possiblePlayersF2){
                                        if(player1.getVisibleID().equals(player2.getVisibleID())){
                                            // means that fight at position j could have a player in it that is in another pool between i and j-1
                                            positionOk = false;
                                            break pPCheck;
                                        }
                                    }
                                }
                            }
                        }
                    }

    // If F1 is moved to i, will it be moved before a fight with two team mates
                    if(positionOk) {
                        ArrayList<Integer> sameTeamFights = playerCodeParsers.get(f1.getPoolID()).getSameTeamFightsFirst();
                        if(!up) {
                            if(!sameTeamFights.contains(f1.getPosition())){
                                for(int l = i; l < j; l++) {
                                    Fight f2 = fights.get(l).getFight();
                                    if(f1.getPoolID() == f2.getPoolID() && sameTeamFights.contains(f2.getPosition())) {
                                        // means that fight in position j is dependent on fight at position l
                                        positionOk = false;
                                        break;
                                    }
                                }
                            }
                        } else {
                            if(sameTeamFights.contains(f1.getPosition())){
                                for(int l = i; l < j; l++) {
                                    Fight f2 = fights.get(n-l-1).getFight();
                                    if(f1.getPoolID() == f2.getPoolID()) {
                                        if(!sameTeamFights.contains(f2.getPosition())) {
                                            // means that fight in position l is dependent on fight at position j
                                            positionOk = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

    // If F1 is moved to i, will it be moved before a fight it is dependent on.
                    if(positionOk) {
                        if(!up) {
                            ArrayList<Integer> dependentFights = playerCodeParsers.get(f1.getPoolID()).getDependentFights(f1.getPosition());
                            for(int l = i; l < j; l++) {
                                Fight f2 = fights.get(up?n-l-1:l).getFight();
                                if(f1.getPoolID() == f2.getPoolID() && dependentFights.contains(f2.getPosition())) {
                                    // means that fight in position j is dependent on fight at position l
                                    positionOk = false;
                                    break;
                                }
                            }
                        } else {
                            for(int l = i; l < j; l++) {
                                Fight f2 = fights.get(up?n-l-1:l).getFight();
                                if(f1.getPoolID() == f2.getPoolID()) {
                                    ArrayList<Integer> dependentFights = playerCodeParsers.get(f2.getPoolID()).getDependentFights(f2.getPosition());
                                    if(dependentFights.contains(f1.getPosition())) {
                                        // means that fight in position l is dependent on fight at position j
                                        positionOk = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }else if(up && sFI1.resultKnown()){
                    positionOk = false;
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
            }
        }
        fixPositions(fights);
    }

    public static void resetOrder(Database database, List<SessionFightInfo> fights) {
        ArrayList<Pool> pools = new ArrayList<Pool>();
        ArrayList<SessionFightInfo> resetFights = new ArrayList<SessionFightInfo>();
        Map<Integer,ArrayList<Integer>> sameTeamFightsInPool = new HashMap<Integer,ArrayList<Integer>>();

        Collections.sort(fights, Fight_COMPARATOR);

        for(SessionFightInfo fight : fights){
            Pool pool = database.get(Pool.class, fight.getFight().getPoolID());
            if(!pools.contains(pool)){
                pools.add(pool);
                sameTeamFightsInPool.put(fight.getFight().getPoolID(), PoolPlayerSequencer.getSameTeamFights(database, fight.getFight().getPoolID() ));
            }
        }

        for(SessionFightInfo fight : fights){
            if(!sameTeamFightsInPool.get(fight.getFight().getPoolID()).contains(fight.getFight().getPosition())){
                resetFights.add(fight);
            }
        }
        fights.removeAll(resetFights);
        fights.addAll(resetFights);

        Collections.sort(pools, POOL_COMPARATOR);

        resetFights.clear();
        for(Pool pool : pools){
            for(SessionFightInfo fight : fights){
                if(fight.getFight().getPoolID() == pool.getID())
                    resetFights.add(fight);
            }
        }
        fights.removeAll(resetFights);
        fights.addAll(resetFights);
        finishedFightsReset(fights);
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

        SessionFightSequencer.saveFightSequence(database, fights, true, false);
        SessionFightSequencer.saveFightSequence(database, followingFights, true, false);
    }
    
    public static SessionFightInfo findDeferredFight(TransactionalDatabase database, Session session) {
        SessionInfo si = new SessionInfo(database, session);

        List<SessionFightInfo> fights = getFightSequence(database, session.getID());
        Collections.reverse(fights);
        for(SessionFightInfo fight : fights) {
            boolean deferred = true;
            for(Pool pool : si.getPools()) {
                if(fight.getFight().getPoolID() == pool.getID()) {
                    deferred = false;
                    break;
                }
            }
            if(deferred) return fight;
        }
        return null;
    }

    public static void undeferAllFights(TransactionalDatabase database, Session session) throws DatabaseStateException {
        while(true) {
            SessionFightInfo fight = findDeferredFight(database, session);
            if(fight == null) return;
            undeferFight(database, fight);
        }
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
            SessionLocker.unlockFights(database, deferredSession);
            //throw new DatabaseStateException("The fights in the preceding session have been locked");

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
        SessionFightSequencer.saveFightSequence(database, fights, true, false);
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

    public static void saveFightSequence(final TransactionalDatabase database, final List<SessionFightInfo> fights, final boolean updateAll, final boolean overwriteLocked)  throws DatabaseStateException {
        if(!overwriteLocked && fights.size() > 0 && fights.iterator().next().getSession().getLockedStatus() == Session.LockedStatus.FIGHTS_LOCKED)
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
