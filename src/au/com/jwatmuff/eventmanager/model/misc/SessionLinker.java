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

import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.db.SessionLinkDAO;
import au.com.jwatmuff.eventmanager.db.SessionPoolDAO;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionLinker {
    private static final Logger log = Logger.getLogger(SessionLinker.class);
    
    /** Creates a new instance of SessionLinker */
    private SessionLinker() {
    }
    
    /**
     * Inserts a session into the database, with the given pools and preceding
     * dependent and mat sessions. For a description of the session data
     * structure see (TODO: document session data structure)
     * 
     * @param database                   The database to be updated
     * @param session                    The session to be added
     * @param precedingMatSession        The preceding mat session
     * @param precedingDependentSessions The preceding dependent sessions
     * @param pools                      The pools assigned to the session
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the database is not in a valid state to insert the session as requested.
     */
    public static void insertSession(Database database, Session session, Session precedingMatSession, Collection<Session> precedingDependentSessions, Collection<Pool> pools) throws DatabaseStateException {
        assert session != null;
        assert precedingMatSession != null;
        assert precedingDependentSessions != null;        

        database.add(session);
        
        linkMatSessions(database, precedingMatSession.getID(), session.getID());
        for(Session preceding : precedingDependentSessions)
            linkDependentSessions(database, preceding.getID(), session.getID());
        
        for(Pool pool : pools) {
            SessionPool sp = new SessionPool();
            sp.setID(new SessionPool.Key(session.getID(), pool.getID()));
            database.add(sp);
        }
    }
    
    private static void linkMatSessions(Database database, int sessionID, int followingID) throws DatabaseStateException {
        SessionInfo si = new SessionInfo(database, sessionID);
        SessionInfo fsi = new SessionInfo(database, followingID);
        
        if(si.getFollowingMatSessions().size() > 0)
            throw new DatabaseStateException("Could not link sessions: session " + si.getSession().getName() + " already has a following session");
        
        if(fsi.getPrecedingMatSessions().size() > 0)
            throw new DatabaseStateException("Could not link sessions: session " + fsi.getSession().getName() + " already has a preceding session");
        
        if(fsi.getSession().getLockedStatus().compareTo(Session.LockedStatus.POSITION_LOCKED) >= 0)
            throw new DatabaseStateException("Could not link sessions: session " + fsi.getSession().getName() + " is locked");
        
        SessionLink link = new SessionLink();
        link.setSessionID(sessionID);
        link.setFollowingID(followingID);
        link.setLinkType(SessionLink.LinkType.MAT);
        
        log.debug("Adding mat link from " + si.getSession().getName() + " to " + fsi.getSession().getName());
        
        database.add(link);
    }
    
    private static void linkDependentSessions(Database database, int sessionID, int followingID) throws DatabaseStateException {
        SessionInfo si = new SessionInfo(database, sessionID);
        SessionInfo fsi = new SessionInfo(database, followingID);
        
        if(si.getFollowingDependentSessions().size() > 0)
            throw new DatabaseStateException("Could not link sessions: session " + si.getSession().getName() + " already has a following dependent session.");
        
        if(fsi.getSession().getLockedStatus().compareTo(Session.LockedStatus.POSITION_LOCKED) >= 0)
            throw new DatabaseStateException("Could not link sessions: session " + fsi.getSession().getName() + " is locked.");
        
        SessionLink link = new SessionLink();
        link.setSessionID(sessionID);
        link.setFollowingID(followingID);
        link.setLinkType(SessionLink.LinkType.DEPENDENT);
        
        database.add(link); 
    }

    /**
     * Deletes (marks invalid) the given session, removing any links to other
     * sessions or pools. IMPORTANT: any sessions following the given session
     * will also be deleted!
     * 
     * @param database  The database to be updated
     * @param session   The session to be deleted
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the database is not a valid state to delete the session, for example if
     * the session has been locked.
     */
    public static void deleteSession(TransactionalDatabase database, Session session) throws DatabaseStateException {
        SessionInfo si = new SessionInfo(database, session);
        
        for(Session following : si.getFollowingSessions())
            deleteSession(database, following);
        
        if(session.getLockedStatus() != Session.LockedStatus.UNLOCKED)
            SessionFightSequencer.undeferAllFights(database, session);

        for(SessionLink sl : database.findAll(SessionLink.class, SessionLinkDAO.FOR_SESSION, session.getID()))
            database.delete(sl);

        for(SessionPool sp : database.findAll(SessionPool.class, SessionPoolDAO.FOR_SESSION, session.getID()))
            database.delete(sp);
        
        database.delete(session);
    }
    
    public static boolean mayContainPool(Database database, int sessionID, int poolID) {
        List<SessionPool> sps = database.findAll(SessionPool.class, SessionPoolDAO.FOR_SESSION, sessionID);
        
        for(SessionPool sp : sps)
            if(sp.getPoolID() == poolID)
                return true;
        
        List<Session> preceding = database.findAll(Session.class, SessionDAO.PRECEDING, sessionID, SessionLink.LinkType.DEPENDENT);
        for(Session s : preceding)
            if(mayContainPool(database, s.getID(), poolID))
                return true;
        
        return false;
    }

    public static List<Session> getMatSessions(Database database, Session mat) {
        List<Session> sessions = new ArrayList<Session>();

        SessionInfo si = new SessionInfo(database, mat);
        while(si.getFollowingMatSessions().size() > 0) {
            Session session = si.getFollowingMatSessions().iterator().next();
            sessions.add(session);
            si = new SessionInfo(database, session);
        }

        return sessions;
    }
}
