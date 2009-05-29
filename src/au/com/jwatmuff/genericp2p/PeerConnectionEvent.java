/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

/**
 *
 * @author James
 */
public class PeerConnectionEvent {
    public enum Type {
        CONNECTED, DISCONNECTED
    };
    
    private Type type;
    private Peer peer;
    
    public PeerConnectionEvent(Type type, Peer peer) {
        this.type = type;
        this.peer = peer;
    }
    
    public Type getType() {
        return type;
    }

    public Peer getPeer() {
        return peer;
    }
}
