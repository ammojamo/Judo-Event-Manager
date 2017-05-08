/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    private ServiceInfo service;
    private boolean registered = false;

    public JmDNSRegistrationService(int port) {
        this.port = port;
    }

    @Override
    public void register() {
        if(jmdns != null) unregister();

        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
        } catch(IOException e) {
            log.error("IOException while initializing JmDNS", e);
        }

        ourName = System.getProperty("user.name");

        service = ServiceInfo.create(REG_TYPE, ourName, port, "Event Manager");

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
        jmdns = null;
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
