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

import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;



/**
 *
 * @author James
 */
public class ResultInfoCache {
    private TransactionListener listener = new TransactionListener() {
        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            for(DataEvent event : events)
                if(event.getDataClass() == Result.class)
                    records.remove( ((Result)event.getData()).getID() );
        }
    };

    private Map<Integer, ResultInfo> records;
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    
    @SuppressWarnings("unchecked")
    public ResultInfoCache(TransactionalDatabase database, TransactionNotifier notifier) {
        this.notifier = notifier;
        records = new LRUMap(100);
        this.database = database;
        notifier.addListener(listener, Result.class);
    }
    
    public ResultInfo getResultInfo(int resultID) throws DatabaseStateException {
        if(records.containsKey(resultID))
            return records.get(resultID);
        else {
            ResultInfo ri = new ResultInfo(database, resultID);
            records.put(resultID, ri);
            return ri;
        }
    }

    public void shutdown() {
        notifier.removeListener(listener);
    }
}
