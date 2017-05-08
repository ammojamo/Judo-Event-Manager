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
