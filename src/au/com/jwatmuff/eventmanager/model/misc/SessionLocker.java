/*
 * SessionLocker.java
 *
 * Created on 22 August 2008, 00:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.db.SessionLinkDAO;
import au.com.jwatmuff.eventmanager.db.SessionPoolDAO;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.Session.LockedStatus;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionLocker {
    private static final Logger log = Logger.getLogger(SessionLocker.class);
    
    /** Creates a new instance of SessionLocker */
    private SessionLocker() {
    }
    
    /**
     * Locks the given position of the given session. Any preceding unlocked
     * sessions will also be locked.
     * 
     * @param database  The database to update
     * @param session   The session to be locked
     */
    public static void lockPosition(final TransactionalDatabase database, final Session session) throws DatabaseStateException {
        assert session != null;
        assert database != null;
        
        if(session.getLockedStatus() != LockedStatus.UNLOCKED)
            throw new DatabaseStateException("Session has already been locked");
        
        /** ensure that all preceding sessions are locked **/

        SessionInfo si = new SessionInfo(database, session);
        Collection<Session> allPreceding = si.getPrecedingSessions();
        if(allPreceding.size() > 0) {
            for(Session preceding : allPreceding) {
                if(preceding.getLockedStatus().compareTo(LockedStatus.POSITION_LOCKED) < 0)
                    lockPosition(database, preceding);
            }

            si = new SessionInfo(database, session);
            allPreceding = si.getPrecedingSessions();
            for(Session preceding : allPreceding) {
                if(preceding.getLockedStatus().compareTo(LockedStatus.POSITION_LOCKED) < 0)
                    throw new DatabaseStateException("Failed to lock all preceding sessions");
            }
        }
        
        /** LOCK SESSION **/
        
        final Session lockedSession = Session.getLockedCopy(session, LockedStatus.POSITION_LOCKED);

        database.perform(new Transaction() {
            @Override
            public void perform() {
                database.add(lockedSession);

                updateLinks(database, session, lockedSession);
                updatePools(database, session, lockedSession);

                database.delete(session);                
            }
        });
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                assignFights(database, lockedSession);
            }            
        });
    }    

    private static void assignFights(Database database, Session session) {
        int pos = 1;
        boolean isBye = false;

        for(Pool pool : database.findAll(Pool.class, PoolDAO.FOR_SESSION, session.getID())) {
            for(Fight fight : database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID())) {
//                if fight isn't bye Add them to the session
                isBye = false;
                
                // I'm not sure this needs to be re-initialized ever iteration of the loop
                // Performance could be an issue
                PlayerCodeParser playerCodeParser = PlayerCodeParser.getInstance(database, pool.getID());

                for(int i = 0; i < 2; i++) {
                    String code = fight.getPlayerCodes()[i];
                    FightPlayer fp = playerCodeParser.parseCode(code);
                    if(fp.type == PlayerType.BYE){
                        isBye = true;
                        break;
                    }
                }
                if (!isBye) {
                    SessionFight sf = new SessionFight();
                    sf.setID(new SessionFight.Key(session.getID(), fight.getID()));
                    sf.setPosition(pos++);
                    database.add(sf);
                }
            }
        }
    }

    /**
     * Locks the fights for the given session. Any preceding sessions will also
     * have their fights locked.
     * 
     * @param database  The database to be updated
     * @param session   The session to lock
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the database is not in a valid state to lock the fights for this session.
     */
    public static void lockFights(final TransactionalDatabase database, final Session session) throws DatabaseStateException{
        if(session.getLockedStatus() != LockedStatus.POSITION_LOCKED)
            throw new DatabaseStateException("A session may only be fight-locked if it has been position-locked");
        
        /** ensure that all preceding sessions are locked **/

        SessionInfo si = new SessionInfo(database, session);
        Collection<Session> allPreceding = si.getPrecedingSessions();
        if(allPreceding.size() > 0) {
            for(Session preceding : allPreceding) {
                // update 'preceding' from database, since the session may have
                // been locked in an earlier iteration of this loop
                preceding = database.get(Session.class, preceding.getID());

                // only lock if 'preceding' is still valid
                if(preceding.isValid() && preceding.getLockedStatus() != LockedStatus.FIGHTS_LOCKED)
                    lockFights(database, preceding);
            }

            si = new SessionInfo(database, session);
            allPreceding = si.getPrecedingSessions();
            for(Session preceding : allPreceding)
                if(preceding.getLockedStatus() != LockedStatus.FIGHTS_LOCKED)
                    throw new DatabaseStateException("Failed to fight-lock all preceding sessions");
        }
        
        final Session lockedSession = Session.getLockedCopy(session, LockedStatus.FIGHTS_LOCKED);
        
        database.perform(new Transaction() {

            @Override
            public void perform() {
                database.add(lockedSession);

                updateLinks(database, session, lockedSession);
                updatePools(database, session, lockedSession);
                updateFights(database, session, lockedSession);

                database.delete(session);
            }
        });   
    }

    private static void updateLinks(Database database, Session oldSession, Session newSession) {
        for(SessionLink sl : database.findAll(SessionLink.class, SessionLinkDAO.FOR_SESSION, oldSession.getID())) {
            sl.setID(IDGenerator.generate());
            if(sl.getSessionID() == oldSession.getID())
                sl.setSessionID(newSession.getID());
            else if(sl.getFollowingID() == oldSession.getID())
                sl.setFollowingID(newSession.getID());
            else
                assert false : "SessionLink is not attached to desired session";
            database.add(sl);
        }
    }
    
    private static void updatePools(Database database, Session oldSession, Session newSession) {
        for(SessionPool sp : database.findAll(SessionPool.class, SessionPoolDAO.FOR_SESSION, oldSession.getID())) {
            sp.setID(new SessionPool.Key(newSession.getID(), sp.getPoolID()));
            database.add(sp);
        }
    }
    
    private static void updateFights(Database database, Session oldSession, Session newSession) {
        for(SessionFight sf : database.findAll(SessionFight.class, SessionFightDAO.FOR_SESSION, oldSession.getID())) {
            sf.setID(new SessionFight.Key(newSession.getID(), sf.getFightID()));
            database.add(sf);
        }
    }
    
}
