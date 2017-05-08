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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class TransactionNotifier implements DataEventListener {
    private static final Logger log = Logger.getLogger(TransactionNotifier.class);
    
    private List<DataEvent> events = new ArrayList<DataEvent>();
    private Set<Class> classes = new HashSet<Class>();
    
    private Map<TransactionListener, Set<Class>> listeners = new HashMap<TransactionListener, Set<Class>>();
    private Map<DataEventListener, Set<Class>> eventListeners = new HashMap<DataEventListener, Set<Class>>();
    
    private LinkedBlockingQueue<DataEvent> eventQueue = new LinkedBlockingQueue<DataEvent>();
    
    private boolean useThread = false;
    
    private Thread notifyThread = new Thread() {
        @Override
        public void run() {
            while(true) {
                try {
                    reallyHandleDataEvent(eventQueue.take());
                } catch (InterruptedException e) {
                    log.error("Interrupted while taking event from event queue", e);
                }
            }
        }
    };
    
    public TransactionNotifier(boolean useThread) {
        this.useThread = useThread;
        if(useThread)
            notifyThread.start();
    }
    
    public TransactionNotifier() {
        this(false);
    }
    
    public void addListener(TransactionListener listener) {
        listeners.put(listener, null);
    }
    
    public void addListener(TransactionListener listener, Class... classes) {
        listeners.put(listener, new HashSet<Class>(Arrays.asList(classes)));
    }
    
    public void removeListener(TransactionListener listener) {
        listeners.remove(listener);
    }
    
    public void addListener(DataEventListener listener) {
        eventListeners.put(listener, null);
    }
    
    public void addListener(DataEventListener listener, Class... classes) {
        eventListeners.put(listener, new HashSet<Class>(Arrays.asList(classes)));
    }
    
    public void removeListener(DataEventListener listener) {
        eventListeners.remove(listener);
    }
    
    @Override
    public void handleDataEvent(DataEvent event) {
        if(useThread) {
            try {
                eventQueue.put(event);
            } catch (InterruptedException e) {
                log.error("Interrupted while placing data event on queue", e);
            }
        } else {
            reallyHandleDataEvent(event);
        }
    }

    public void reallyHandleDataEvent(DataEvent event) {
        switch(event.getTransactionStatus()) {
            case BEGIN:
                events.clear();
                classes.clear();
            case CURRENT:
                events.add(event);
                classes.add(event.getDataClass());
                break;
            case END:
            case NONE:
                events.add(event);
                classes.add(event.getDataClass());
                notifyListeners();
                events.clear();
                classes.clear();
                break;
        }
        
        for(Entry<DataEventListener, Set<Class>> entry : eventListeners.entrySet()) {
            DataEventListener listener = entry.getKey();
            Set<Class> interestingClasses = entry.getValue();
            
            if(interestingClasses == null || interestingClasses.contains(event.getDataClass()))
                try {
                    listener.handleDataEvent(event);
                } catch(Exception e) {
                    log.error(e);
                }
        }
    }
    
    private void notifyListeners() {
        Map<TransactionListener, Set<Class>> listenersCopy = new HashMap<>(listeners);
        
        for(Entry<TransactionListener, Set<Class>> entry : listenersCopy.entrySet()) {
            TransactionListener listener = entry.getKey();
            Set<Class> interestingClasses = entry.getValue();
            
            if(interestingClasses == null ||
               CollectionUtils.containsAny(interestingClasses, classes)) {
                try {
                    listener.handleTransactionEvents(events, classes);
                } catch(Exception e) {
                    log.error("Error while notifying listener: " + listener, e);
                }
            }
        }
    }    
}
