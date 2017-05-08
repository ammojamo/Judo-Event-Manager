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

package au.com.jwatmuff.eventmanager.util;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * The most basic event bus I could think of
 * 
 * @author james
 */
public class EventBus {
    public interface Listener {
        void receive(String key, Object value);
    }
    
    static List<Listener> listeners = new ArrayList<>();
    
    public synchronized static void send(final String key, final Object value) {
        for(final Listener listener : listeners) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.receive(key, value);
                }
            });
        }
    }

    public synchronized static void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public synchronized static void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
