/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author James
 */
public interface PeerManager {
    public void addConnectionListener(PeerConnectionListener listener);
    public void removeConnectionListener(PeerConnectionListener listener);
    public UUID getUUID();
    public <T> void registerService(String serviceName, Class<T> serviceClass, T implementation);
    public <T> void registerService(Class<T> serviceClass, T implementation);
    public void unregisterService(String serviceName);
    public Collection<Peer> getPeers();
    public boolean isRegistered(String serviceName);
}
