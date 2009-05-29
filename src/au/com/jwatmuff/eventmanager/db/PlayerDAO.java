/*
 * PlayerDAO.java
 *
 * Created on 12 August 2008, 02:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface PlayerDAO extends GenericDAO<Player> {
    public static final String ALL = "all";
    public Collection<Player> findAll();
    
    public static final String FOR_VISIBLE_ID = "forVisibleID";
    public Player findForVisibleID(String id);
    
    public static final String FOR_POOL = "forPool";
    public Collection<Player> findForPool(int poolID, boolean approved);
    
    public static final String WITHOUT_POOL = "withoutPool";
    public Collection<Player> findWithoutPool();
}
