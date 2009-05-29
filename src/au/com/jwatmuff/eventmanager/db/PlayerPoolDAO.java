/*
 * PlayerPoolDAO.java
 *
 * Created on 12 August 2008, 02:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface PlayerPoolDAO extends GenericDAO<PlayerPool> {
    public static final String FOR_PLAYER = "forPlayer";
    public Collection<PlayerPool> findForPlayer(int playerID);
    
    public static final String FOR_POOL = "forPool";
    public Collection<PlayerPool> findForPool(int poolID);
}
