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
