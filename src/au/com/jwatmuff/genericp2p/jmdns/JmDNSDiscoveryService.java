/*
 * JmDNSDiscoveryService.java
 *
 * Created on 29 March 2010, 04:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.jmdns;

import au.com.jwatmuff.genericp2p.AbstractDiscoveryService;
import au.com.jwatmuff.genericp2p.PeerInfo;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 *
 * @author James
 */
public class JmDNSDiscoveryService extends AbstractDiscoveryService {
    private static Logger log = Logger.getLogger(JmDNSDiscoveryService.class);

    public final static String REG_TYPE = "_eventmanager._tcp.local.";

    private final JmDNS jmdns;
    private final MyListener listener = new MyListener();

    /** Creates a new instance of JmDNSDiscoveryService */
    public JmDNSDiscoveryService() {
        try {
            jmdns = JmDNS.create();
        } catch(IOException e) {
            throw new RuntimeException("IOException while initializing JmDNS", e);
        }
    }

    @Override
    public void start() {
        log.debug("Browsing for services");
        jmdns.addServiceListener(REG_TYPE, listener);
        setRunning(true);
    }

    @Override
    public void stop() {
        jmdns.removeServiceListener(REG_TYPE, listener);
        setRunning(false);
    }

    class MyListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            String serviceName = event.getName();
            log.info("Found service (" + serviceName + "), resolving..");
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            String serviceName = event.getName();
            log.info("Peer lost (" + serviceName + ")");
            if(!removePeer(serviceName)) {
                log.warn("Peer '" + serviceName + "' was not known to be found.");
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            String serviceName = event.getName();
            InetAddress addr  = event.getInfo().getInetAddress();
            int port = event.getInfo().getPort();
            PeerInfo peer = new PeerInfo(serviceName, new InetSocketAddress(addr, port));
            log.info("Peer found (" + serviceName + ")");
            addPeer(peer);
        }
    }
}
