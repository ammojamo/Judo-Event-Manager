/*
 * PoolListTableModel.java
 *
 * Created on 28 April 2008, 01:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.pool;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PoolListTableModel extends BeanMapperTableModel<Pool> implements TransactionListener {
    private static final Logger log = Logger.getLogger(PoolListTableModel.class);

    private Database database;
    private BeanMapper<Pool> beanMapper = new BeanMapper<Pool>() {

        @Override
        public Map<String, Object> mapBean(Pool p) {
            Map<String, Object> map = new HashMap<String, Object>();

            map.put("description", p.getDescription());
            map.put("locked", p.getLockedStatus() != Pool.LockedStatus.UNLOCKED);

            if (p.getID() != 0) {
                int playersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, p.getID(), true).size();
                int outstandingRequests = database.findAll(Player.class, PlayerDAO.FOR_POOL, p.getID(), false).size();
                map.put("approvedPlayers", playersInPool + "/" + (playersInPool+outstandingRequests));
            } else {
                int playersInPool = database.findAll(Player.class, PlayerDAO.WITHOUT_POOL).size();
                map.put("approvedPlayers", String.valueOf(playersInPool));
            }
            return map;
        }
    };

    /** Creates a new instance of PoolListTableModel */
    public PoolListTableModel(Database database, TransactionNotifier notifier) {
        super();
        this.database = database;
        setBeanMapper(beanMapper);
        addColumn("Division", "description");
        //addColumn("Outstanding Requests", "outstandingRequests");
        //addColumn("Players In Division", "playersInPool");
        addColumn("Approved Players", "approvedPlayers");
        addColumn("Players Locked", "locked");
        updateTableFromDatabase();
        notifier.addListener(this, Pool.class, PlayerPool.class, Player.class);
    }

    private void updateTableFromDatabase() {
        Collection<Pool> pools = database.findAll(Pool.class, PoolDAO.WITH_PLAYERS);

        // add 'no pool'
        Pool nopool = new Pool();
        nopool.setDescription("No Division");
        nopool.setID(0);
        pools.add(nopool);

        Collection<Pool> oldPools = getBeans();

        // add new beans and update existing ones
        for (Pool pool : pools) {
            int id = pool.getID();
            Pool oldPool = getPoolByID(id, oldPools);
            if (oldPool == null) {
                addBean(pool);
            } else {
                updateBean(oldPool, pool);
            }
        }

        // remove beans not present in the new list
        for (Pool oldPool : oldPools) {
            int id = oldPool.getID();
            if (getPoolByID(id, pools) == null) {
                removeBean(oldPool);
            }
        }

    //setBeans(pools);
    }

    private static Pool getPoolByID(int id, Collection<Pool> pools) {
        for (Pool pool : pools) {
            if (pool.getID() == id) {
                return pool;
            }
        }
        return null;
    }
    
    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateTableFromDatabase();
    }
}