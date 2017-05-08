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

package au.com.jwatmuff.genericp2p.windows;

import au.com.jwatmuff.genericp2p.AbstractDiscoveryService;
import au.com.jwatmuff.genericp2p.PeerInfo;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class WindowsNetDiscoveryService  extends AbstractDiscoveryService {
    private static Logger log = Logger.getLogger(WindowsNetDiscoveryService.class);
    Timer checkTimer;
    TimerTask checkTask = new TimerTask() {
        @Override
        public void run() {
            for(String computerName : WindowsNetUtil.getNetworkComputerNames()) {

                try {
                    InetAddress address = InetAddress.getByName(computerName);
                } catch(UnknownHostException e) {
                    log.info("Unable to resolve host: [" + computerName + "]");
                }

                PeerInfo peer = new PeerInfo(
                        computerName +  " (Discovered via Windows Network)",
                        new InetSocketAddress(computerName, 1199));
                addPeer(peer);
            }
        }
    };
    
    @Override
    public void start() {
        if(!WindowsNetUtil.runningOnWindows()) {
            log.info("Not running on windows - skipping windows network discovery");
            return;
        }
        if(checkTimer != null) stop();
        checkTimer = new Timer(WindowsNetDiscoveryService.class.getSimpleName());
        checkTimer.schedule(checkTask, 0, 30000);
    }

    @Override
    public void stop() {
        if(checkTimer != null) {
            checkTimer.cancel();
            checkTimer = null;
        }
    }
    
}
