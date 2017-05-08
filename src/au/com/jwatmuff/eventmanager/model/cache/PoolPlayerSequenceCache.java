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
