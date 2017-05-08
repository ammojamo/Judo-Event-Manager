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

package au.com.jwatmuff.genericdb.transaction;

import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import au.com.jwatmuff.genericdb.distributed.NotifyingDatabase;
import au.com.jwatmuff.genericdb.distributed.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class NotifyingTransactionalDatabase implements TransactionalDatabase {
    private static final Logger log = Logger.getLogger(NotifyingTransactionalDatabase.class);
    
    private NotifyingDatabase ndb;
    private boolean inTransaction = false;
    private Map<Thread, List<DataEvent>> threadEvents = Collections.synchronizedMap(new HashMap<Thread, List<DataEvent>>());
    private DataEventListener listener;
    private boolean updateTimestamps;
    
    private TransactionalDatabase database;
    
    private DataEventListener ndblistener = new DataEventListener() {
        @Override
        public void handleDataEvent(DataEvent event) {
            List<DataEvent> events = getEventList();
            if(inTransaction)
                events.add(event);
            else {
                if(listener != null)
                    listener.handleDataEvent(event);
            }
        }  
    };

    private List<DataEvent> getEventList() {
        flushThreadEvents();
        Thread t = Thread.currentThread();
        if(!threadEvents.containsKey(t))
            threadEvents.put(t, new ArrayList<DataEvent>());
        return threadEvents.get(t);
    }

    private void flushThreadEvents() {
        for(Thread t : new HashSet<Thread>(threadEvents.keySet())) {
            if(!t.isAlive())
                threadEvents.remove(t);
        }
    }
    
    public NotifyingTransactionalDatabase(TransactionalDatabase database) {
        this.database = database;
        ndb = new NotifyingDatabase(database);
        ndb.setListener(ndblistener);
        ndb.setUpdateTimestamps(updateTimestamps);
    }
    
    public void setReadOnly(boolean readOnly) {
        ndb.setReadOnly(readOnly);
    }
    
    public void setUpdateTimestamps(boolean updateTimestamps) {
        ndb.setUpdateTimestamps(updateTimestamps);
        this.updateTimestamps = updateTimestamps;
    }
    
    public void setListener(DataEventListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void perform(Transaction t) {
        Timestamp ts = new Timestamp();
        List<DataEvent> events = getEventList();
        events.clear();

        inTransaction = true;
        try {
            database.perform(t);
        } catch(Exception e) {
            log.error("Exception during transaction, discarding transaction", e);
            inTransaction = false;
            events.clear();
            return;
        }
        inTransaction = false;

        fireTransactionEvents(ts);
    }
    
    private void fireTransactionEvents(Timestamp timestamp) {
        List<DataEvent> events = getEventList();
        int numEvents = events.size();
        int i = 0;

        for(DataEvent event : events) {
            i++;
            boolean first = (i == 1);
            boolean last = (i == numEvents);
            
            if(first && last)
                event.setTransactionStatus(DataEvent.TransactionStatus.NONE);
            else if(first)
                event.setTransactionStatus(DataEvent.TransactionStatus.BEGIN);
            else if(last)
                event.setTransactionStatus(DataEvent.TransactionStatus.END);
            else
                event.setTransactionStatus(DataEvent.TransactionStatus.CURRENT);
            
            if(updateTimestamps)
                event.getData().setTimestamp(timestamp);
            
            if(listener != null)
                listener.handleDataEvent(event);
        }
        events.clear();
    }

    @Override
    public <T> void add(T item) {
        ndb.add(item);
    }

    @Override
    public <T> void update(T item) {
        ndb.update(item);
    }

    @Override
    public <T> void delete(T item) {
        ndb.delete(item);
    }

    @Override
    public <T> T get(Class<T> aClass, Object id) {
        return ndb.get(aClass, id);
    }

    @Override
    public <T> List<T> findAll(Class<T> aClass, String query, Object... args) {
        return ndb.findAll(aClass, query, args);
    }

    @Override
    public <T> T find(Class<T> aClass, String query, Object... args) {
        return ndb.find(aClass, query, args);
    }    
}
