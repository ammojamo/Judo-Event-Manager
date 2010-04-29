/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.jmdns;

import au.com.jwatmuff.genericp2p.PeerRegistrationService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class JmDNSRegistrationService implements PeerRegistrationService {
    private final static Logger log = Logger.getLogger(JmDNSRegistrationService.class);
    public final static String REG_TYPE = "_eventmanager._tcp.local.";

    private String ourName;
    private final int port;

    private final JmDNS jmdns;
    private final ServiceInfo service;
    private boolean registered = false;

    public JmDNSRegistrationService(int port) {
        this.port = port;

        try {
            jmdns = JmDNS.create();
        } catch(IOException e) {
            throw new RuntimeException("IOException while initializing JmDNS", e);
        }

        try {
            ourName = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e) {
            ourName = System.getProperty("user.name") + "@unknown";
        }

        service = ServiceInfo.create(REG_TYPE, ourName, port, "Event Manager");
    }

    public void register() {
        try {
            jmdns.registerService(service);
            log.debug("Attempting to register as service (" + ourName + ")");
            //todo: is there a way to verify that we succesfully registered?
            registered = true;
        } catch(IOException e) {
            log.error("Unable to register with network", e);
            throw new RuntimeException("Bonjour registration failed", e);
        }
    }

    public void unregister() {
        jmdns.unregisterService(service);
        registered = false;
    }

    public String getOurName() {
        if(!registered)
            throw new RuntimeException("Invalid state - service is not running");
        return ourName;
    }
}
