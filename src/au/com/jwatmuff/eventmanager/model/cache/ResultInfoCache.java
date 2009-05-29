/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
