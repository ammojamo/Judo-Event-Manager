/*
 * FightDAO.java
 *
 * Created on 14 August 2008, 21:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface FightDAO extends GenericDAO<Fight> {
    public static final String FOR_POOL = "forPool";
    public Collection<Fight> findForPool(int poolID);
    
    public static final String UNPLAYED_IN_SESSION = "unplayedInSession";
    public Collection<Fight> findUnplayedInSession(int sessionID);

    public static final String IN_LOCKED_SESSION = "inLockedSession";
    public Collection<Fight> findInLockedSession();

    public static final String WITH_RESULT = "withResult";
    public Collection<Fight> findWithResult();
}
