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
