/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.rmi;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 *
 * @author James
 */
public interface AnnounceService {
    /**
     * This method is to be called remotely (via RMI) by a peer to inform the
     * local client of its existence on the network. The local client may
     * already be aware of the peer, by this mechanism or otherwise.
     *
     * @param name      The name of the remote peer
     * @param address   The socket address by which the peer may be contacted
     */
    void announce(String name, InetSocketAddress address, UUID id);

    /**
     * This method is to be called remotely (via RMI) by a peer to inform the
     * local client of its existence on the network. The local client may
     * already be aware of the peer, by this mechanism or otherwise.
     *
     * @param name      The name of the remote peer
     * @param address   The socket address by which the peer may be contacted
     */
    void announceDisconnected(String name, UUID id);
}
