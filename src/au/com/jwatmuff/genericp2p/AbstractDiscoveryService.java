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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author James
 */
public abstract class AbstractDiscoveryService implements PeerDiscoveryService {
    private PeerDiscoveryListener peerListener;
    private final Map<String, PeerInfo> discoveredPeers = Collections.synchronizedMap(new HashMap<String, PeerInfo>());
    private boolean running = false;

    @Override
    public void setListener(PeerDiscoveryListener listener) {
        this.peerListener = listener;
    }

    protected void addPeer(PeerInfo peer) {
        discoveredPeers.put(peer.getName(), peer);
        if(this.peerListener != null)
            peerListener.handleDiscoveryEvent(new PeerDiscoveryEvent(PeerDiscoveryEvent.Type.FOUND, peer));
    }

    protected boolean removePeer(String serviceName) {
        PeerInfo peer = discoveredPeers.remove(serviceName);
        if(peer != null && this.peerListener != null)
                peerListener.handleDiscoveryEvent(new PeerDiscoveryEvent(PeerDiscoveryEvent.Type.LOST, peer));
        return peer != null;
    }

    protected void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public List<PeerInfo> getPeers() {
        if(!running)
            throw new RuntimeException("Invalid state - service has not been started yet");
        return new ArrayList<PeerInfo>(discoveredPeers.values());
    }

}
