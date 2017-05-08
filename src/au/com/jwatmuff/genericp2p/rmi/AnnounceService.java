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
