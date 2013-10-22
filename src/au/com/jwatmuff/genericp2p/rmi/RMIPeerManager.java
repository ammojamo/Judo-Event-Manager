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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private class PeerService<T> {
        String name;
        Class<T> serviceClass;
        T implementation;
        RmiServiceExporter exporter;
    }

    private int registryPort;
    private File idFile;
    private UUID uuid;
    private Collection<PeerConnectionListener> listeners = new ArrayList<>();
    private Map<String, PeerService> peerServiceMap = new HashMap<>();
    private String name;
    private final Collection<ManagedPeer> peers = Collections.synchronizedCollection(new ArrayList<ManagedPeer>());
    private final Set<InetSocketAddress> addresses = Collections.synchronizedSet(new HashSet<InetSocketAddress>());
    private Registry registry;
    
    private ScheduledExecutorService slowPeerCheckExecutor;
    private ScheduledExecutorService fastPeerCheckExecutor;
    private ExecutorService timeoutExecutor = Executors.newCachedThreadPool();

    public RMIPeerManager(int registryPort, File idFile) {
        try {
            registry = LocateRegistry.createRegistry(registryPort);
        } catch(RemoteException e) {
            log.error("Failed to create RMI registry", e);
        }
        
        this.registryPort = registryPort;
        this.idFile = idFile;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public void start() {
        log.debug("Obtained local ID: " + this.getUUID());

        registerService(AnnounceService.class, this);
        registerService(IdentifyService.class, this);
        registerService(LookupService.class, this);
        
        fastPeerCheckExecutor = Executors.newScheduledThreadPool(2);
        slowPeerCheckExecutor = Executors.newScheduledThreadPool(4);
        
        fastPeerCheckExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for(ManagedPeer peer : getManagedPeers()) {
                    log.debug("FAST CHECK CHECKING: " + peer);
                    checkPeer(peer);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        slowPeerCheckExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                addSelf();
                for(InetSocketAddress address : getAddresses()) {
                    log.debug("SLOW CHECK CHECKING: " + address);
                    checkAddress(address);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        addSelf();

        log.debug("Completed RMIPeerManager startup");
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
        handlePeerInfo(new PeerInfo(name, address, id));
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
    public void announceDisconnected(String name, UUID id) {
        ManagedPeer peer = getPeerWithUUID(id);
        if(peer != null) peer.setDisconnected();
    }

    public void handlePeerInfo(final PeerInfo info) {
        addresses.add(info.getAddress());
        fastPeerCheckExecutor.submit(new Runnable() {
            @Override
            public void run() {
                checkAddress(info.getAddress());
            } 
        });
    }
    
    private void checkAddress(InetSocketAddress address) {
        if(getPeerWithAddress(address) != null) return;

        ManagedPeer peer = new ManagedPeer(address);
        peer.checkConnectivity();
        if(peer.isConnected()) {
            peer.populatePeerInfo();
            boolean notify = false;
            synchronized(peers) {
                if(getPeerWithUUID(peer.getUUID()) == null) {
                    peers.add(peer);
                    notify = true;
                }
            }
            if(notify) {
                notifyPeerConnectionListeners(new PeerConnectionEvent(PeerConnectionEvent.Type.CONNECTED, peer));
                announcePeer(peer);
            }
        }
    }
    
    private void checkPeer(ManagedPeer peer) {
        peer.checkConnectivity();
        if(peer.isConnected()) {
            announcePeer(peer);
        }
    }
    
    private void announcePeer(ManagedPeer p) {
        try {
            AnnounceService service = p.getService(AnnounceService.class);
            for(InetSocketAddress address : getAddresses()) {
                service.announce(null, address, null);
            }
        } catch(NoSuchServiceException e) {
            log.error("Exception announcing peers", e);
        }
    }
    
    private ManagedPeer getPeerWithAddress(InetSocketAddress address) {
        synchronized(peers) {
            for(ManagedPeer peer : peers) {
                if(peer.getAddress().equals(address)) return peer;
            }
        }
        return null;
    }
    
    private ManagedPeer getPeerWithUUID(UUID uuid) {
        synchronized(peers) {
            for(ManagedPeer peer : peers) {
                if(peer.getUUID().equals(uuid)) return peer;
            }
        }
        return null;
    }
    
    private void notifyPeerConnectionListeners(PeerConnectionEvent event) {
        int n = listeners.size();
        int i = 0;
        for(PeerConnectionListener listener : new ArrayList<PeerConnectionListener>(listeners)) {
            log.debug("NOTIFYING LISTENER " + (++i) + "/" + n + " - " + listener);
            try {
                listener.handleConnectionEvent(event);
            } catch(Exception e) {
                log.error("Error notifying listener of peer connection event", e);
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
            this.handlePeerInfo(event.getPeerInfo());
        }
    }

    /**
     * I want this to tell all peers to set this peer to disconnected.
     */
    @Override
    public void stop() {
        slowPeerCheckExecutor.shutdown();
        fastPeerCheckExecutor.shutdown();
        try {
            for(Peer peer : getPeers())
                peer.getService(AnnounceService.class).announceDisconnected(name, uuid);
        } catch(NoSuchServiceException e) {
            log.error("Exception announcing disconnection to peers", e);
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
            //exporter.setRegistryPort(registryPort);
            exporter.setRegistry(registry);
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
    
    private void addSelf() {
            // Add ourselves as a peer on our new IP address
            try {
                InetSocketAddress local = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), registryPort);
                handlePeerInfo(new PeerInfo(name, local, uuid));
            } catch(UnknownHostException e) {
                log.error("Could not find local host", e);
            }
    }
    
    @Override
    public void refreshServices() {
        try {
            // Restart RMI Registry
            UnicastRemoteObject.unexportObject(registry,true);
            registry = LocateRegistry.createRegistry(registryPort);
            
            // Re-export services to new RMI Registry
            for(PeerService service : peerServiceMap.values()) {
                refreshService(service);
            }
            
            addSelf();
        } catch(Exception e) {
            log.error("Failed to refresh services", e);
        }
    }
    
    private void refreshService(PeerService service) {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setRegistryPort(registryPort);
        exporter.setServiceName(service.name);
        exporter.setServiceInterface(service.serviceClass);
        exporter.setService(service.implementation);
        try {
            service.exporter.destroy();
            exporter.afterPropertiesSet();
            exporter.prepare();
            service.exporter = exporter;
        } catch (RemoteException e) {
            log.error("Refreshing service " + service.name + " (" + service + ") failed", e);
        }
    }


    @SuppressWarnings("unchecked")
    private static <T> T getService(InetSocketAddress address, String serviceName, Class<T> serviceClass) throws NoSuchServiceException {
        String url = "rmi://" + address.getHostName() + ":" + address.getPort() + "/" + serviceName;
        try {
            /* maybe should use address.getAddress().getHostAddress() */
            RmiProxyFactoryBean factory = new RmiProxyFactoryBean();
            factory.setServiceInterface(serviceClass);
            factory.setServiceUrl(url);
            factory.afterPropertiesSet();
            return (T) factory.getObject();
        } catch (RemoteLookupFailureException rlfe) {
            throw new NoSuchServiceException("Unable to resolve service " + serviceName + " at url: " + url);
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
        synchronized(peers) {
            for (ManagedPeer peer : peers) {
                if (peer.isConnected()) {
                    connectedPeers.add(peer);
                }
            }
        }

        return connectedPeers;
    }

    private Collection<ManagedPeer> getManagedPeers() {
        synchronized(peers) {
            return new ArrayList<>(peers);
        }
    }
    
    private Collection<InetSocketAddress> getAddresses() {
        synchronized(addresses) {
            return new ArrayList<>(addresses);
        }
    }

    @Override
    public boolean isRegistered(String serviceName) {
        return peerServiceMap.containsKey(serviceName);
    }

    @Override
    public boolean initialisedOk() {
        return true;
    }

    private static enum PeerStatus {
        DISCONNECTED, CONNECTED
    }
    private static final ProxyFactory pf = new ProxyFactory();

    private class ManagedPeer implements Peer {
        boolean connected = true;
        PeerInfo info;
        InetSocketAddress address;

        public ManagedPeer(InetSocketAddress address) {
            this.address = address;
            populatePeerInfo();
        }

        private Interceptor interceptor = new Interceptor() {
            @Override
            public Object intercept(Invocation inv) throws Throwable {
                if (!connected) {
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

        private void populatePeerInfo() {
            try {
                IdentifyService service = getService(IdentifyService.class);
                info = new PeerInfo(service.getName(), address, service.getUUID());
            } catch(NoSuchServiceException e) {
                log.error(e.getMessage(), e);
            }
        }
        
        public void checkConnectivity() {
            if(!connected) return;

            log.debug("Checking connectivity of: " + this);
            final AtomicBoolean connected = new AtomicBoolean(false);
            Future<?> f = timeoutExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        UUID uuid = getServiceWithoutProxy(IdentifyService.class).getUUID();
                        if(info == null || uuid.equals(info.getID())) connected.set(true);
                    } catch (Exception e) {
                    }
                }
            });

            try {
                f.get(5, TimeUnit.SECONDS);
            } catch(TimeoutException e) {
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
            
            if(!connected.get()) {
                setDisconnected();
            } else {
                log.debug("Connectivity check: OK");
            }
        }

        public void setDisconnected() {
            if(!connected) return;
            
            if(peers.remove(this)) {
                log.debug("Setting peer " + this + " Disconnected");
                connected = false;
                
                timeoutExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        notifyPeerConnectionListeners(new PeerConnectionEvent(PeerConnectionEvent.Type.DISCONNECTED, ManagedPeer.this));
                    }
                });
            }
        }

        public boolean isConnected() {
            return connected;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(final String name, final Class<T> serviceClass) throws NoSuchServiceException {
            Future<T> future = timeoutExecutor.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    return RMIPeerManager.getService(address, name, serviceClass);
                }
            });
            
            try {
                T service = future.get(5, TimeUnit.SECONDS);
                T proxy = (T) pf.createInterceptorProxy(service, interceptor, new Class[]{serviceClass});
                return proxy;
            } catch(TimeoutException e) {
                setDisconnected();
                throw new NoSuchServiceException("No such service: " + name + " - due to timeout");
            } catch(ExecutionException e) {
                if(e.getCause() instanceof NoSuchServiceException) throw (NoSuchServiceException)e.getCause();
                else throw new RuntimeException(e);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T getService(Class<T> serviceClass) throws NoSuchServiceException {
            return getService(serviceClass.getCanonicalName(), serviceClass);
        }

        private <T> T getServiceWithoutProxy(String name, Class<T> serviceClass) throws NoSuchServiceException {
            return RMIPeerManager.getService(address, name, serviceClass);
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
            return info.getID();
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return getName() + " @ " + address + " : " + uuid;
        }
    }
}
