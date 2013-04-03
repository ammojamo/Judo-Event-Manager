/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.jmdns;

import au.com.jwatmuff.genericp2p.PeerRegistrationService;
import java.io.IOException;
import java.net.InetAddress;
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

    private JmDNS jmdns;
    private final ServiceInfo service;
    private boolean registered = false;

    public JmDNSRegistrationService(int port) {
        this.port = port;

        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
        } catch(IOException e) {
            log.error("IOException while initializing JmDNS", e);
        }

        ourName = System.getProperty("user.name");

        service = ServiceInfo.create(REG_TYPE, ourName, port, "Event Manager");
    }

    @Override
    public void register() {
        if(jmdns == null) return;

        log.debug("Attempting to register as service (" + ourName + ")");

        // perform registration in a seperate thread so that if it blocks for a
        // long time, we can give up waiting for it and continue
        Thread registerThread = new Thread() {
            @Override
            public void run() {
                try {
                    jmdns.registerService(service);
//TODO: is there a way to verify that we succesfully registered?
                    registered = true;
                } catch (IOException e) {
                    log.error("Unable to register with network", e);
                    //throw new RuntimeException("Bonjour registration failed", e);
                }
            }
        };

        registerThread.start();
        try {
            registerThread.join(10000);
            if(!registered) {
                log.warn("Failed to register within 10 seconds, continuing..");
            }
        } catch(InterruptedException e) {
            log.error("Interrupted while waiting for registration thread.");
        }
    }

    @Override
    public void unregister() {
        if(jmdns == null) return;

        jmdns.unregisterService(service);
        registered = false;
    }

    public String getOurName() {
        //if(!registered)
        //    throw new RuntimeException("Invalid state - service is not running");
        return ourName;
    }

    public boolean initialisedOk() {
        return jmdns != null;
    }
}
