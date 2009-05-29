/*
 * DataChangeNotifier.java
 *
 * Created on 27 February 2008, 15:22
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DataEventNotifier implements DataEventListener {
    private static final Logger log = Logger.getLogger(DataEventNotifier.class);
    private List<DataEventListener> listeners = Collections.synchronizedList(new ArrayList<DataEventListener>());
    private boolean handleExceptions = true;

    /** Creates a new instance of DataEventNotifier */
    public DataEventNotifier() {
        //pool = Executors.newFixedThreadPool(10);
    }
    
    public void addListener(DataEventListener listener) {
        listeners.add(listener);
        log.debug("Number of listeners: " + listeners.size());
    }
    
    public void setListeners(List<DataEventListener> listeners)
    {
        this.listeners = new ArrayList<DataEventListener>(listeners);
    }
    
    public void fireEvent(final DataEvent event) {
        for(final DataEventListener listener : listeners) {
            if(handleExceptions) {
                try {
                    listener.handleDataEvent(event);
                } catch(Exception e) {
                    log.error("Error in event listener", e);
                }
            } else {
                listener.handleDataEvent(event);
            }
        } 
    }

    @Override
    public void handleDataEvent(DataEvent event) {
        fireEvent(event);
    }

    public void removeListener(DataEventListener listener) {
        listeners.remove(listener);
        log.debug("Number of listeners: " + listeners.size());
    }
}
