/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.cache;

import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;

/**
 *
 * @author James
 */
public class PoolPlayerSequenceCache {
    private TransactionalDatabase database;

    private Map<Integer, List<PlayerPoolInfo>> cache;
    
    private DataEventListener listener = new DataEventListener() {
        @Override
        public void handleDataEvent(DataEvent event) {
            cache.remove(((PlayerPool)event.getData()).getPoolID());
        }
    };
    
    @SuppressWarnings("unchecked")
    public PoolPlayerSequenceCache(TransactionalDatabase database, TransactionNotifier notifier) {
        this.database = database;
        cache = new LRUMap(100);
        notifier.addListener(listener, PlayerPool.class);
    }
    
    public List<PlayerPoolInfo> getPlayerSequence(int poolID) {
        if(cache.containsKey(poolID))
            return cache.get(poolID);
        else {
            List<PlayerPoolInfo> ppi = PoolPlayerSequencer.getPlayerSequence(database, poolID);
            cache.put(poolID, ppi);
            return ppi;
        }
    }
}
