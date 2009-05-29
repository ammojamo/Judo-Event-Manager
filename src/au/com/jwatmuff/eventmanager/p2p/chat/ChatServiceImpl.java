/*
 * ChatServiceImpl.java
 *
 * Created on 24 April 2008, 02:18
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.p2p.chat;

/**
 *
 * @author James
 */
public class ChatServiceImpl implements ChatService {
    private ChatListener listener;

    /** Creates a new instance of ChatServiceImpl */
    public ChatServiceImpl() {
    }
    
    public void setLocalChatListener(ChatListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void sendMessage(String message, String fromPeer) {
        if(listener != null)
            listener.handleMessage(message, fromPeer);
    }
}
