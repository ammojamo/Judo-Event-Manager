/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.bonjour;

import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDRegistration;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.RegisterListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class BonjourRegistrationService {
    private final static Logger log = Logger.getLogger(BonjourRegistrationService.class);
    private final static int TIMEOUT = 15;
    public final static String REG_TYPE = "_eventmanager._tcp";
    
    private String ourName;
    private final int port;
    
    private DNSSDRegistration registration;
    private final MyListener listener = new MyListener();
    
    public BonjourRegistrationService(int port) {
        this.port = port;
        
        try {
            ourName = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e) {
            ourName = System.getProperty("user.name") + "@unknown";
        }
    }
    
    public void register() {
        try {
            DNSSD.register(ourName, REG_TYPE, port, listener);
            log.debug("Attempting to register as service (" + ourName + ")");

            int timeout = 0;
            while(registration == null) {
                if(timeout++ > TIMEOUT*10)
                    throw new RuntimeException("Bonjour registration timed out");
                Thread.sleep(100);
            }
        } catch(DNSSDException e) {
            log.error("Unable to register with network", e);
            throw new RuntimeException("Bonjour registration failed", e);
        } catch(InterruptedException e) {
            log.error("Service interrupted during registration.");
            throw new RuntimeException("Service interrupted during bonjour registration");
        }        
    }
    
    public void unregister() {
        registration.stop();
        registration = null;
    }
    
    public String getOurName() {
        if(registration == null)
            throw new RuntimeException("Invalid state - service is not running");
        return ourName;
    }

    private class MyListener implements RegisterListener {
        @Override
        public void serviceRegistered(DNSSDRegistration reg, int flags, String serviceName, String regType, String domain) {
            log.debug("Succesfully registered as service (" + serviceName +")");
            registration = reg;
            ourName = serviceName;
        }

        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            log.error("Service (" + service.getClass() + ") reported error code " + errorCode);
        }
    }
}
