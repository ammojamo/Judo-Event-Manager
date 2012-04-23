/*
 * SessionInfo.java
 *
 * Created on 19 August 2008, 21:49
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionInfo {
    private static final Logger log = Logger.getLogger(SessionInfo.class);
    
    private Session session;
    private Collection<Session> followingDependentSessions;
    private Collection<Session> followingMatSessions;
    private Collection<Session> precedingDependentSessions;
    private Collection<Session> precedingMatSessions;

    private Collection<Pool> pools;

    private static final int LEVEL_UNDEFINED = -1;
    private static final int LEVEL_EVALUATING = -2;
    
    private static final String MAT_UNDEFINED = null;
    
    private int level = LEVEL_UNDEFINED;
    private String mat = MAT_UNDEFINED;
    
    /** Creates a new instance of SessionInfo */
    public SessionInfo(Database database, Session session) {
        this.session = session;
        followingDependentSessions = database.findAll(Session.class, SessionDAO.FOLLOWING, session.getID(), SessionLink.LinkType.DEPENDENT);
        followingMatSessions = database.findAll(Session.class, SessionDAO.FOLLOWING, session.getID(), SessionLink.LinkType.MAT);
        precedingDependentSessions = database.findAll(Session.class, SessionDAO.PRECEDING, session.getID(), SessionLink.LinkType.DEPENDENT);
        precedingMatSessions = database.findAll(Session.class, SessionDAO.PRECEDING, session.getID(), SessionLink.LinkType.MAT);        
        pools = database.findAll(Pool.class, PoolDAO.FOR_SESSION, session.getID());
    }
    
    public SessionInfo(Database database, int sessionID) {
        this(database, database.get(Session.class, sessionID));
    }
    
    public static Collection<SessionInfo> getAll(Database database) {
        Collection<Session> sessions = database.findAll(Session.class, SessionDAO.ALL);
        
        Collection<SessionInfo> sis = new ArrayList<SessionInfo>();
        for(Session session : sessions)
            sis.add(new SessionInfo(database, session));

        for(SessionInfo si : sis) {
            updateLevel(sis, si);
            updateMat(sis, si);
        }

        return sis;
    }
    
    private static void updateMat(Collection<SessionInfo> sis, SessionInfo si) {
        if(si.mat != MAT_UNDEFINED)
            return;

        if(si.getSession().getType() == Session.SessionType.MAT) {
            si.mat = si.getSession().getMat();
        }
        else {
            Collection<Session> preceding = si.getPrecedingMatSessions();
            if(preceding.size() > 0) {
                Session ps = preceding.iterator().next();
                SessionInfo psi = findWithID(sis, ps.getID());
                updateMat(sis, psi);
                si.mat = psi.mat;
            }
        }
    }
    
    private static void updateLevel(Collection<SessionInfo> sis, SessionInfo si) {
        if(si.level >= 0)
            return;

        // if the level of this session is being evaluated, we have a cyclic dependency
        if(si.level == LEVEL_EVALUATING)            
            throw new RuntimeException("Cyclic dependency between sessions");

        // mark this session as currently evaluating
        si.level = LEVEL_EVALUATING;

        Collection<Session> preceding = si.getPrecedingMatSessions();
        preceding.addAll(si.getPrecedingDependentSessions());
        
        int level = 0;
        for(Session ps : preceding) {
            SessionInfo psi = findWithID(sis, ps.getID());
            updateLevel(sis, psi);
            level = Math.max(level, psi.level+1);
        }
        si.level = level;
    }
    
    private static SessionInfo findWithID(Collection<SessionInfo> sis, int id) {
        for(SessionInfo si : sis)
            if(si.getSession().getID() == id)
                return si;
        return null;
    }

    public Session getSession() {
        return session;
    }

    public Collection<Pool> getPools() {
        return pools;
    }
    
    public Collection<Session> getFollowingDependentSessions() {
        return followingDependentSessions;
    }
    
    public Collection<Session> getFollowingMatSessions() {
        return followingMatSessions;
    }

    public Collection<Session> getPrecedingDependentSessions() {
        return precedingDependentSessions;
    }

    public Collection<Session> getPrecedingMatSessions() {
        return precedingMatSessions;
    }
    
    public Collection<Session> getFollowingSessions() {
        ArrayList<Session> sessions = new ArrayList<Session>();
        sessions.addAll(followingMatSessions);
        
        // remove any sessions that are both mat AND dependent sessions
        for(Session fds : followingDependentSessions)
            for(Session fms : followingMatSessions)
                if(fds.getID().equals(fms.getID()))
                    sessions.remove(fms);
        
        sessions.addAll(followingDependentSessions);
        return sessions;
    }

    public Collection<Session> getPrecedingSessions() {
        ArrayList<Session> sessions = new ArrayList<Session>();
        sessions.addAll(precedingMatSessions);
        
        // remove any sessions that are both mat AND dependent sessions
        for(Session pds : precedingDependentSessions)
            for(Session pms : precedingMatSessions)
                if(pds.getID().equals(pms.getID()))
                    sessions.remove(pms);

        sessions.addAll(precedingDependentSessions);
        return sessions;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getMat() {
        return mat;
    }
    
    public static Session getLastOnMat(Database database, Session session) {
        // ensure that we have the last session on the mat
        SessionInfo si = new SessionInfo(database, session);
        while(si.getFollowingMatSessions().size() > 0)
            si = new SessionInfo(database, si.getFollowingMatSessions().iterator().next());

        return si.getSession();
    }
    
    private static void findAllFollowingSessions(Database database, Session session, Collection<Session> sessions) {
        SessionInfo si = new SessionInfo(database, session);
        for(Session s : si.getFollowingSessions()) {
            findAllFollowingSessions(database, s, sessions);
            sessions.add(s);
        }
    }
    
    public static Collection<Session> findAllFollowingSessions(Database database, Session session) {
        Collection<Session> sessions = new ArrayList<Session>();
        findAllFollowingSessions(database, session, sessions);
        return sessions;
    }
    
    public static Collection<Session> findAllFollowingFightLockedSessions(Database database, Session session) {
        Collection<Session> sessions = findAllFollowingSessions(database, session);
        Iterator<Session> iter = sessions.iterator();
        while(iter.hasNext()) if(iter.next().getLockedStatus() != Session.LockedStatus.FIGHTS_LOCKED) iter.remove();
        return sessions;
    }

    private static boolean containsSessionID(Collection<Session> sessions, int sessionID) {
        for(Session session : sessions)
            if(session.getID().equals(sessionID))
                return true;
        return false;
    }
    
    private static void findAllPrecedingSessionsWithLockedStatus(Database database, Session session, Session.LockedStatus status, Collection<Session> sessions) {
        SessionInfo si = new SessionInfo(database, session);
        for(Session s : si.getPrecedingSessions()) {
            if(containsSessionID(sessions, s.getID()))
                continue;
            if(s.getLockedStatus() == status) {
                findAllPrecedingSessionsWithLockedStatus(database, s, status, sessions);
                sessions.add(s);
            }
        }
    }

    public static Collection<Session> findAllPrecedingUnlockedSessions(Database database, Session session) {
        Collection<Session> sessions = new ArrayList<Session>();
        findAllPrecedingSessionsWithLockedStatus(database, session, Session.LockedStatus.UNLOCKED, sessions);
        return sessions;
    }
    
    public static Collection<Session> findAllPrecedingPositionLockedSessions(Database database, Session session) {
        Collection<Session> sessions = new ArrayList<Session>();
        findAllPrecedingSessionsWithLockedStatus(database, session, Session.LockedStatus.POSITION_LOCKED, sessions);
        return sessions;        
    }
}
