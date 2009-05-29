/*
 * SessionDAO.java
 *
 * Created on 19 August 2008, 00:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionDAO extends GenericDAO<Session> {
    public static final String ALL = "all";
    public Collection<Session> findAll();
    
    public static final String FOLLOWING = "following";
    public Collection<Session> findFollowing(int sessionID, SessionLink.LinkType linkType);
    
    public static final String PRECEDING = "preceding";
    public Collection<Session> findPreceding(int sessionID, SessionLink.LinkType linkType);
    
    public static String ALL_MATS = "allMats";
    public Collection<Session> findAllMats();

    public static String ALL_NORMAL = "allNormal";
    public Collection<Session> findAllNormal();

    public static String WITH_LOCKED_STATUS = "withLockedStatus";
    public Collection<Session> findWithLockedStatus(Session.LockedStatus lockedStatus);

    public static String FOR_FIGHT = "forFight";
    public Session findForFight(int fightID);
}
