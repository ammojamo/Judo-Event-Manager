/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

/**
 *
 * @author James
 */
public class PeerDiscoveryEvent {
    public enum Type {
        FOUND, LOST
    }
    
    private Type type;
    private PeerInfo info;
    
    public PeerDiscoveryEvent(Type type, PeerInfo info) {
        this.type = type;
        this.info = info;
    }
    
    public Type getType() {
        return type;
    }

    public PeerInfo getPeerInfo() {
        return info;
    }
}
