/*
 * BonjourPeerManager.java
 *
 * Created on 11 March 2008, 04:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.bonjour;

import au.com.jwatmuff.genericp2p.AbstractDiscoveryService;
import au.com.jwatmuff.genericp2p.PeerInfo;
import com.apple.dnssd.BrowseListener;
import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.QueryListener;
import com.apple.dnssd.ResolveListener;
import com.apple.dnssd.TXTRecord;
import org.apache.log4j.Logger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 *
 * @author James
 */
public class BonjourDiscoveryService extends AbstractDiscoveryService {
    private static Logger log = Logger.getLogger(BonjourDiscoveryService.class);
    
    public final static String REG_TYPE = "_eventmanager._tcp";
    
    private DNSSDService browser;
    private final MyListener listener = new MyListener();
    
    /** Creates a new instance of BonjourPeerManager */
    public BonjourDiscoveryService() {
    }
    
    @Override
    public void start() {
        try {
            log.debug("Browsing for services");
            setRunning(true);
            browser = DNSSD.browse(REG_TYPE, listener);
        } catch(DNSSDException e) {
            log.error("Unable to browse network", e);
        }        
    }
    
    @Override
    public void stop() {
        browser.stop();
        browser = null;
        setRunning(false);
    }
         
    class MyListener implements BrowseListener {
        @Override
        public void serviceFound(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
            try {
                log.info("Found service (" + serviceName + "), resolving..");
                DNSSD.resolve(0, DNSSD.ALL_INTERFACES, serviceName, REG_TYPE, domain, new MyResolver(serviceName));
            } catch(DNSSDException e) {
                log.error("Unable to resolve service " + serviceName, e);
            }
        }

        @Override
        public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
            log.info("Peer lost (" + serviceName + ")");
            if(!removePeer(serviceName)) {
                log.warn("Peer '" + serviceName + "' was not known to be found.");
            }
        }        
        
        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            log.error("Service (" + service.getClass() + ") reported error code " + errorCode);
        }
    }
    
    class MyResolver implements ResolveListener, QueryListener {
        private int port;
        private String serviceName;
        
        public MyResolver(String serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, TXTRecord txtRecord) {
            this.port = port;
            try {
                log.debug("Resolved service (" + fullName + "), querying..");
                DNSSD.queryRecord(0, ifIndex, hostName, 1 /* address query */, 1 /* internet class */, this);
            } catch(DNSSDException e) {
                log.error("Unable to query service for IP address", e);
            } finally {
                resolver.stop();
            }
        }
        
        @Override
        public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
            try {
                log.debug("Query answered for service (" + fullName + ")");
                InetAddress addr = InetAddress.getByAddress(rdata);
                PeerInfo peer = new PeerInfo(serviceName, new InetSocketAddress(addr, port));
                log.info("Peer found (" + serviceName + ")");
                addPeer(peer);
            } catch(UnknownHostException e) {
                log.error("Unable to find IP address for service " + fullName, e);
            }
        }
        
        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            log.error("Service (" + service.getClass() + ") reported error code " + errorCode);
        }
    }
}
