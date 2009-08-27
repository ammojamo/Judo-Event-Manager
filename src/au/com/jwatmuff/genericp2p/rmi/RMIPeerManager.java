/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericp2p.rmi;

import au.com.jwatmuff.genericp2p.NoSuchServiceException;
import au.com.jwatmuff.genericp2p.Peer;
import au.com.jwatmuff.genericp2p.PeerConnectionEvent;
import au.com.jwatmuff.genericp2p.PeerConnectionListener;
import au.com.jwatmuff.genericp2p.PeerDiscoveryEvent;
import au.com.jwatmuff.genericp2p.PeerDiscoveryListener;
import au.com.jwatmuff.genericp2p.PeerInfo;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.log4j.Logger;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 *
 * @author James
 */
public class RMIPeerManager implements PeerManager, PeerDiscoveryListener, AnnounceService, IdentifyService, LookupService {
    private static final Logger log = Logger.getLogger(RMIPeerManager.class);
    private static final int RETRY_PERIOD = 30000; //milliseconds

    private class PeerService<T> {
        String name;
        Class<T> serviceClass;
        T implementation;
        RmiServiceExporter exporter;
    }

    private int registryPort;
    private UUID uuid;
    private Collection<PeerConnectionListener> listeners = new ArrayList<PeerConnectionListener>();
    private Map<String, PeerService> peerServiceMap = new HashMap<String, PeerService>();
    private String name;
    private Collection<ManagedPeer> peers = Collections.synchronizedCollection(new ArrayList<ManagedPeer>());

    private Thread checkPeerThread = new Thread("checkPeerThread") {
        @Override
        public void run() {
            while (true) {
                checkDisconnectedPeers();
                log.debug(getPeerTables());
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                }
            }
        }
    };

    public RMIPeerManager(int registryPort) {
        this.registryPort = registryPort;

        log.debug("Obtained local ID: " + this.getUUID());

        registerService(AnnounceService.class, this);
        registerService(IdentifyService.class, this);
        registerService(LookupService.class, this);

        log.debug("Starting retry thread");
        checkPeerThread.start();

        log.debug("Completed RMIPeerManager startup");
    }

    public void setName(String name) {
        /* if this is the first time we have been given a name, add ourselves
         * to the list of peers using localhost address */
        if(this.name == null) {
            try {
                InetSocketAddress local = new InetSocketAddress(InetAddress.getLocalHost(), registryPort);
                handlePeerInfo(new PeerInfo(name, local, uuid));
            } catch(UnknownHostException e) {
                log.error("Could not find local host", e);
            }
        }

        this.name = name;
    }

    /**
     * This method is to be called remotely by a peer to determine the ID of the
     * local client. This ID is unique on an operating system user basis, using
     * a mechanism such as a user registry (under Windows) or a user
     * configuration file (under Linux) to store the ID.
     * 
     * The ID is a random 128-bit Universally Unique ID, guaranteed to be
     * globally unique to a very high level of certainty.
     * 
     * @return  The ID of the local client
     */
    @Override
    public UUID getUUID() {
        if (uuid != null) {
            return uuid;
        }

        File idFile = new File("peerid.dat");
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(idFile));
            uuid = UUID.fromString(props.getProperty("uuid"));
        } catch (IOException e) {
            try {
                uuid = UUID.randomUUID();
                props.setProperty("uuid", uuid.toString());
                props.store(new FileOutputStream(idFile), null);
            } catch (IOException ex) {
                log.error("Unable to record peer UUID to file", ex);
            }
        }

        return uuid;
    }

    /**
     * This method is to be called remotely (via RMI) by a peer to inform the
     * local client of its existence on the network. The local client may
     * already be aware of the peer, by this mechanism or otherwise.
     * 
     * @param name      The name of the remote peer
     * @param address   The socket address by which the peer may be contacted
     */
    @Override
    public void announce(String name, InetSocketAddress address, UUID id) {
        log.debug("Got name/IP address/id from peer: " + name + " : " + address + " : " + id);
        final PeerInfo info = new PeerInfo(name, address, id);
        handlePeerInfo(info);
    }

    public void handlePeerInfo(PeerInfo info) {
        /* update ID if necessary (i.e. if from Bonjour) */
        if (info.getID() == null) {
            try {
                UUID peerUUID = getService(
                        info.getAddress(),
                        IdentifyService.class.getCanonicalName(),
                        IdentifyService.class).getUUID();
                info.setID(peerUUID);
            } catch(Exception e) {
                log.error("Couldn't get UUID from new peer", e);
                return;
            }
        }

        for (ManagedPeer peer : new ArrayList<ManagedPeer>(peers)) {
            if (peer.getUUID().equals(info.getID())) {
                if (peer.isConnected() &&
                        !peer.currentAddress.getAddress().isLoopbackAddress()) {
                    log.debug("Peer already connected on network address, adding (possibly) new address to history");
                    peer.addAddressToHistory(info.getAddress());
                } else {
                    log.debug("Reconnecting peer on address " + info.getAddress());
                    peer.setAddress(info.getAddress());
                    announcePeers(peer);
                }
                return;
            }
        }

        /* if the peer was not already in our list, add it */
        log.debug("New peer found, adding as connected");
        ManagedPeer peer = new ManagedPeer(info);
        peers.add(peer);
        for(PeerConnectionListener listener : new ArrayList<PeerConnectionListener>(listeners)) {
            listener.handleConnectionEvent(
                new PeerConnectionEvent(PeerConnectionEvent.Type.CONNECTED, peer));
        }
        announcePeers(peer);
    }

    private void announcePeers(ManagedPeer peer) {
        for(ManagedPeer peer2 : new ArrayList<ManagedPeer>(peers)) {
            try {
                if(peer2.isConnected() && !peer2.getUUID().equals(peer.getUUID())) {
                    peer.getService(AnnounceService.class).announce(peer.getName(), peer.currentAddress, peer.getUUID());
                }
            } catch(Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * Handles a peer FOUND or LOST event generated by a PeerDiscoveryService.
     * If a peer has been found, the manager will attempt to connect to that
     * peer.
     * If a peer has been lost, the manager will mark the peer as disconnected
     * and not attempt to re-connect to it.
     * 
     * @param event
     */
    @Override
    public void handleDiscoveryEvent(PeerDiscoveryEvent event) {
        if (event.getType() == PeerDiscoveryEvent.Type.FOUND) {
            log.debug("Got name/IP address from Bonjour: " + event.getPeerInfo().getName() + " : " + event.getPeerInfo().getAddress());
            this.handlePeerInfo(event.getPeerInfo());
        }
    }

    /**
     * Goes through the list of previously seen (but disconnected) peers, tries
     * to reconnect to them if enough time has elapsed since the last check.
     */
    public void checkDisconnectedPeers() {
        for (ManagedPeer peer : new ArrayList<ManagedPeer>(peers)) {
            if (peer.isDisconnected()) {
                for (InetSocketAddress address : peer.addressHistory) {
                    peer.setAddress(address);
                    if(peer.isConnected()) break;
                }
            } else {
                peer.checkConnectivity();
            }
        }
    }

    /**
     * Registers a PeerConnectionListener which will be notified when peers are
     * connected to or disconnected from the peer manager.
     * 
     * @param listener  The listener to be registered
     */
    @Override
    public void addConnectionListener(PeerConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters a PeerConnectionListener
     * 
     * @param listener  The listener to be unregistered
     * 
     * @see addConnectionListener
     */
    @Override
    public void removeConnectionListener(PeerConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Associates the given class and service name, so that the service may
     * be obtained from any Peer objects supplied by this PeerManager.
     * If an implementation is supplied, remote client may obtain an instance
     * of the service from the this client.
     * 
     * For the service to be obtainable from a given remote peer, that peer must
     * have also registered the service and supplied an implementation of the
     * service.
     * 
     * @param <T>
     * @param serviceName
     * @param serviceClass
     * @param implementation
     */
    @Override
    public <T> void registerService(String serviceName, Class<T> serviceClass, T implementation) {
        PeerService<T> service = new PeerService<T>();
        service.name = serviceName;
        service.serviceClass = serviceClass;
        service.implementation = implementation;

        if (peerServiceMap.containsKey(serviceName)) {
            log.warn("Service with name " + serviceName + " has already been registered");
        }

        if (implementation != null) {
            RmiServiceExporter exporter = new RmiServiceExporter();
            exporter.setRegistryPort(registryPort);
            exporter.setServiceName(serviceName);
            exporter.setServiceInterface(serviceClass);
            exporter.setService(implementation);
            try {
                exporter.afterPropertiesSet();
                exporter.prepare();
                service.exporter = exporter;
                peerServiceMap.put(serviceName, service);
            } catch (RemoteException e) {
                log.error("Exporting service " + serviceName + " (" + serviceClass + ") failed", e);
            }
        }
    }

    @Override
    public <T> void registerService(Class<T> serviceClass, T implementation) {
        registerService(serviceClass.getCanonicalName(), serviceClass, implementation);
    }

    @Override
    public void unregisterService(String serviceName) {
        PeerService service = peerServiceMap.remove(serviceName);
        if(service == null) {
            log.warn("Attempt to unregister unknown service: " + serviceName);
            return;
        }
        try {
            service.exporter.destroy();
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * To do : find a way of checking local RMI registry or detecting when it
     * stops working. Then try reregistering everything.
     */
    @SuppressWarnings("unchecked")
    private void reregisterServices() {
        for (PeerService service : peerServiceMap.values()) {
            registerService(service.name, service.serviceClass, service.implementation);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getService(InetSocketAddress address, String serviceName, Class<T> serviceClass) throws NoSuchServiceException {
        try {
            /* maybe should use address.getAddress().getHostAddress() */
            String url = "rmi://" + address.getHostName() + ":" + address.getPort() + "/" + serviceName;
            RmiProxyFactoryBean factory = new RmiProxyFactoryBean();
            factory.setServiceInterface(serviceClass);
            factory.setServiceUrl(url);
            factory.afterPropertiesSet();
            return (T) factory.getObject();
        } catch (RemoteLookupFailureException rlfe) {
            throw new NoSuchServiceException("Unable to resolve service " + serviceName);
        } catch (Exception e) {
            throw new RuntimeException("getService failed", e);
        }
    }

    @Override
    public Map<String, Class> getServices() {
        Map<String, Class> services = new HashMap<String, Class>();
        for (String serviceName : peerServiceMap.keySet()) {
            services.put(serviceName, peerServiceMap.get(serviceName).serviceClass);
        }
        return services;
    }

    @Override
    public Collection<String> getServices(Class serviceClass) {
        Collection<String> names = new ArrayList<String>();
        for (String serviceName : peerServiceMap.keySet()) {
            if (peerServiceMap.get(serviceName).serviceClass.equals(serviceClass)) {
                names.add(serviceName);
            }
        }
        return names;
    }

    /**
     * Returns the list of peers which this manager is currently connected to.
     *
     * @return
     */
    @Override
    public Collection<Peer> getPeers() {
        Collection<Peer> connectedPeers = new ArrayList<Peer>();
        for (ManagedPeer peer : peers) {
            if (peer.isConnected()) {
                connectedPeers.add(peer);
            }
        }

        return connectedPeers;
    }

    @Override
    public boolean isRegistered(String serviceName) {
        return peerServiceMap.containsKey(serviceName);
    }

    private static enum PeerStatus {
        DISCONNECTED, CONNECTED
    }
    private static final ProxyFactory pf = new ProxyFactory();

    private class ManagedPeer implements Peer {
        private PeerStatus status = PeerStatus.CONNECTED;
        PeerInfo info;
        UUID uuid;
        InetSocketAddress currentAddress;
        List<InetSocketAddress> addressHistory = new ArrayList<InetSocketAddress>();
        private Interceptor RMIInterceptor = new Interceptor() {

            @Override
            public Object intercept(Invocation inv) throws Throwable {
                if (isDisconnected()) {
                    throw new RuntimeException("Peer is disconnected");
                }
                try {
                    return inv.proceed();
                } catch (RemoteException e) {
                    log.error(this + " Connectivity failure", e);
                    setDisconnected();
                    throw e;
                }
            }
        };

        public void checkConnectivity() {
            try {
                getServiceWithoutProxy(IdentifyService.class).getUUID().toString();
                setConnected();
            } catch (Exception e) {
                log.error(this + " Connectivity failure", e);
                setDisconnected();
            }
        }

        private void setConnected() {
            if(status != PeerStatus.CONNECTED) {
                log.debug("Setting peer " + this + " Connected");
                status = PeerStatus.CONNECTED;
                for(PeerConnectionListener listener : new ArrayList<PeerConnectionListener>(listeners))
                    listener.handleConnectionEvent(new PeerConnectionEvent(PeerConnectionEvent.Type.CONNECTED, this));
            }
        }

        private void setDisconnected() {
            if(status != PeerStatus.DISCONNECTED) {
                log.debug("Setting peer " + this + " Disconnected");
                status = PeerStatus.DISCONNECTED;
                for(PeerConnectionListener listener : new ArrayList<PeerConnectionListener>(listeners))
                    listener.handleConnectionEvent(new PeerConnectionEvent(PeerConnectionEvent.Type.DISCONNECTED, this));
            }
        }

        public boolean isConnected() {
            return status == PeerStatus.CONNECTED;
        }

        public boolean isDisconnected() {
            return status == PeerStatus.DISCONNECTED;
        }

        public ManagedPeer(PeerInfo info) {
            this.info = info;
            this.uuid = info.getID();
            setAddress(info.getAddress());
        }

        public void setAddress(InetSocketAddress address) {
            currentAddress = address;
            addAddressToHistory(address);
            checkConnectivity();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(String name, Class<T> serviceClass) throws NoSuchServiceException {
            T service = RMIPeerManager.getService(currentAddress, name, serviceClass);
            T proxy = (T) pf.createInterceptorProxy(service, RMIInterceptor, new Class[]{serviceClass});
            return proxy;
        }

        @Override
        public <T> T getService(Class<T> serviceClass) throws NoSuchServiceException {
            return getService(serviceClass.getCanonicalName(), serviceClass);
        }

        private <T> T getServiceWithoutProxy(String name, Class<T> serviceClass) throws NoSuchServiceException {
            return RMIPeerManager.getService(currentAddress, name, serviceClass);
        }

        private <T> T getServiceWithoutProxy(Class<T> serviceClass) throws NoSuchServiceException {
            return getServiceWithoutProxy(serviceClass.getCanonicalName(), serviceClass);
        }

        @Override
        public String getName() {
            return info.getName();
        }

        @Override
        public UUID getUUID() {
            if (uuid == null) {
                try {
                    uuid = getService(IdentifyService.class).getUUID();
                } catch (NoSuchServiceException e) {
                    throw new RuntimeException("Unable to access identify service for peer.", e);
                }
            }
            return uuid;
        }

        @Override
        public String toString() {
            return getName() + " @ " + currentAddress + " : " + uuid;
        }

        public void addAddressToHistory(InetSocketAddress address) {
            addressHistory.remove(address);
            addressHistory.add(0, address);
        }
    }

    private String getPeerTables() {
        StringBuilder sb = new StringBuilder();
        sb.append("Peer list:");
        for (ManagedPeer peer : peers) {
            sb.append("\n  " + peer + " : " + ((peer.status == PeerStatus.CONNECTED) ? "Connected" : "Disconnected"));
            for(InetSocketAddress address : peer.addressHistory) {
                sb.append("\n    " + address);
            }
        }
        return sb.toString();
    }
}
