/*
 * SessionPoolInfo.java
 *
 * Created on 19 August 2008, 15:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionPoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author James
 */
public class SessionPoolInfo {
    private SessionPool sp;
    private Session session;
    private Pool pool;
    
    /** Creates a new instance of SessionPoolInfo */
    public SessionPoolInfo(SessionPool sp, Session session, Pool pool) {
        this.sp = sp;
        this.session = session;
        this.pool = pool;
    }
    
    public SessionPoolInfo(Database database, SessionPool sp) {
        session = database.get(Session.class, sp.getID().sessionID);
        pool = database.get(Pool.class, sp.getID().poolID);
    }
    
    
    public static Collection<SessionPoolInfo> getForAllPools(Database database) {
        Collection<SessionPoolInfo> spis = new ArrayList<SessionPoolInfo>();
        
        Collection<Pool> pools = database.findAll(Pool.class, PoolDAO.ALL);
        for(Pool pool : pools) {            
            Collection<SessionPool> sps = database.findAll(SessionPool.class, SessionPoolDAO.FOR_POOL, pool.getID());
            if(sps.size() > 0) {
                SessionPool sp = sps.toArray(new SessionPool[0])[0];
                Session session = database.get(Session.class, sp.getID().sessionID);
                spis.add(new SessionPoolInfo(sp, session, pool));
            }
            else            
                spis.add(new SessionPoolInfo(null, null, pool));
        }
        
        return spis;
    }

    public SessionPool getSessionPool() {
        return sp;
    }

    public Session getSession() {
        return session;
    }

    public Pool getPool() {
        return pool;
    }
}
