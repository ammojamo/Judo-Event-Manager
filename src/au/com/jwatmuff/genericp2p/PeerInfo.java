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

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 *
 * @author James
 */
public class PeerInfo {
    private String name;
    private InetSocketAddress address;
    private UUID id;
    
    public PeerInfo(String name, InetSocketAddress address) {
        this(name, address, null);
    }

    public PeerInfo(String name, InetSocketAddress address, UUID id) {
        this.name = name;
        this.address = address;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }

    public UUID getID() {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }
}
