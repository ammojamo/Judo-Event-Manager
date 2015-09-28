/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
