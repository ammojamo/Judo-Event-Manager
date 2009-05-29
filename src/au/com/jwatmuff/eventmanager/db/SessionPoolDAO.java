/*
 * SessionPoolDAO.java
 *
 * Created on 19 August 2008, 04:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionPoolDAO extends GenericDAO<SessionPool> {
    public static final String ALL = "all";
    public Collection<SessionPool> findAll();
    
    public static final String FOR_SESSION = "forSession";
    public Collection<SessionPool> findForSession(int sessionID);
    
    public static final String FOR_POOL = "forPool";
    public Collection<SessionPool> findForPool(int poolID);
}
