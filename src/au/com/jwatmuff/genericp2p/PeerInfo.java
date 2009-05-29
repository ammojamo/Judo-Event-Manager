/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 *
 * @author James
 */
public class PeerInfo {
    private String name;
    private InetSocketAddress address;
    private UUID id;
    
    public PeerInfo(String name, InetSocketAddress address) {
        this(name, address, null);
    }

    public PeerInfo(String name, InetSocketAddress address, UUID id) {
        this.name = name;
        this.address = address;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }

    public UUID getID() {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }
}
