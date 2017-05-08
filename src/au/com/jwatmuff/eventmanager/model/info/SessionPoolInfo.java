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
