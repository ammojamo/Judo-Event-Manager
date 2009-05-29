/*
 * DataEventListener.java
 *
 * Created on 12 August 2008, 00:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

/**
 *
 * @author James
 */
public interface DataEventListener {
    public void handleDataEvent(DataEvent event);
}
