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

package au.com.jwatmuff.genericp2p.rmi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class InetAddressUtil {
    private static final Logger log = Logger.getLogger(InetAddressUtil.class);
    
    private static Collection<InetAddress> addresses;
    
    private InetAddressUtil() {}
    
    public static Collection<InetAddress> getLocalAddresses() {
        if(addresses == null) {
            addresses = new ArrayList<InetAddress>();
            try {
                for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                        addresses.add(addr);
                    }
                }
            } catch (SocketException ex) {
                log.error("Exception while local enumerating network interfaces", ex);
            }

        }

        return new ArrayList<InetAddress>(addresses);
    }
}
