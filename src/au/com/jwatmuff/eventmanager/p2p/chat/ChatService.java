/*
 * ChatService.java
 *
 * Created on 24 April 2008, 01:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.p2p.chat;

/**
 *
 * @author James
 */
public interface ChatService {
    public static final String SERVICE_NAME = "chatService";
    public void sendMessage(String message, String fromPeer);
}
