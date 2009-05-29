/*
 * PoolDAO.java
 *
 * Created on 12 August 2008, 02:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface PoolDAO extends GenericDAO<Pool> {
    public static final String ALL = "all";
    public Collection<Pool> findAll();
    
    public static final String WITH_PLAYERS = "withPlayers";
    public Collection<Pool> findWithPlayers();
    
    public static final String BY_DESCRIPTION = "byDescription";
    public Pool findByDescription(String name);
    
    public static final String WITH_LOCKED_STATUS = "withLockedStatus";
    public Collection<Pool> findWithLockedStatus(Pool.LockedStatus lockedStatus);
    
    public static final String FOR_SESSION = "forSession";
    public Collection<Pool> findForSession(int sessionID);
    
    public static final String WITHOUT_SESSION = "withoutSession";
    public Collection<Pool> findWithoutSession();
}
