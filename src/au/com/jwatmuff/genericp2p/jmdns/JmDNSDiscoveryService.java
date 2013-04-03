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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class JmDNSDiscoveryService extends AbstractDiscoveryService {
    private static Logger log = Logger.getLogger(JmDNSDiscoveryService.class);

    public final static String REG_TYPE = "_eventmanager._tcp.local.";

    private JmDNS jmdns;
    private final MyListener listener = new MyListener();

    /** Creates a new instance of JmDNSDiscoveryService */
    public JmDNSDiscoveryService() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = InetAddress.getByName(addr.getHostName()).toString();
            jmdns = JmDNS.create(addr, hostname);
        } catch(IOException e) {
            log.error("IOException while initializing JmDNS", e);
        }
    }

    @Override
    public void start() {
        if(jmdns == null) return;

        log.debug("Browsing for services");
        jmdns.addServiceListener(REG_TYPE, listener);
        setRunning(true);
    }

    @Override
    public void stop() {
        if(jmdns == null) return;

        jmdns.removeServiceListener(REG_TYPE, listener);
        setRunning(false);
    }

    class MyListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            String serviceName = event.getName();
            log.info("Found service (" + serviceName + "), resolving..");
            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
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
            for(InetAddress addr : event.getInfo().getInetAddresses()) {
                int port = event.getInfo().getPort();
                PeerInfo peer = new PeerInfo(serviceName, new InetSocketAddress(addr, port));
                log.info("Peer found (" + serviceName + ") at address: " + addr);
                addPeer(peer);
            }
        }
    }

    public boolean initialisedOk() {
        return jmdns != null;
    }
}
