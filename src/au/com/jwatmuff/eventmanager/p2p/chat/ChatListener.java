/*
 * ChatListener.java
 *
 * Created on 24 April 2008, 17:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.p2p.chat;

/**
 *
 * @author James
 */
public interface ChatListener {
    public void handleMessage(String message, String fromPeer);
}
