/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericp2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class ManualDiscoveryService extends AbstractDiscoveryService {
    private static final Logger log = Logger.getLogger(ManualDiscoveryService.class);

    @Override
    public void start() {
        // nothing to do
    }

    @Override
    public void stop() {
        // nothing to do
    }

    public void manuallyAddHost(String hostName) {
        try {
            PeerInfo info = new PeerInfo(hostName, new InetSocketAddress(InetAddress.getByName(hostName), 1199));
            addPeer(info);
        } catch(UnknownHostException e) {
            log.error("Attempted to manually add unknown host: " + hostName, e);
        }
    }    
}
