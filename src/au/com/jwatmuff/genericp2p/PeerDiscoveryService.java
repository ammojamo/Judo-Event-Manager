/*
 * PeerManager.java
 *
 * Created on 10 March 2008, 20:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

import java.util.List;

/**
 *
 * @author James
 */
public interface PeerDiscoveryService {
    public void start();
    public void stop();
    public void setListener(PeerDiscoveryListener listener);
    public List<PeerInfo> getPeers();
}
