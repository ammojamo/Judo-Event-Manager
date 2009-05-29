/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
