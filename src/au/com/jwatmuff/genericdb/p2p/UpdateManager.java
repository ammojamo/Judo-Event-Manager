/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import au.com.jwatmuff.genericdb.distributed.Clock;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import au.com.jwatmuff.genericdb.p2p.AuthenticationUtils.AuthenticationPair;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabaseUpdater;
import au.com.jwatmuff.genericp2p.NoSuchServiceException;
import au.com.jwatmuff.genericp2p.Peer;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;

/**
 *
 * @author James
 */
public class UpdateManager implements TransactionListener, DatabaseUpdateService {
    private static final Logger log = Logger.getLogger(UpdateManager.class);

    private static final int VERSION = 1;

    private final Update update;

    private PeerManager peerManager;
    private TransactionalDatabaseUpdater databaseUpdater;
    private String updateFileName;
    private UUID databaseID;
    private int passwordHash;

    private UUID ourID;

    private Map<UUID, byte[]> peerPrefixes = Collections.synchronizedMap(new HashMap<UUID, byte[]>());

    private BlockingQueue<Peer> syncQueue = new LinkedBlockingQueue<Peer>();
    private Set<Peer> synchronizingPeers = Collections.synchronizedSet(new HashSet<Peer>());
    private Set<Peer> pendingPeers = Collections.synchronizedSet(new HashSet<Peer>());

    private boolean shutdown = false;

    public UpdateManager(
            PeerManager     peerManager,
            TransactionalDatabaseUpdater databaseUpdater,
            String          updateFileName,
            UUID            databaseID,
            int             passwordHash) {
        this.peerManager = peerManager;
        this.databaseUpdater = databaseUpdater;
        this.updateFileName = updateFileName;
        this.databaseID = databaseID;
        this.passwordHash = passwordHash;

        ourID = peerManager.getUUID();

        Update updateFromFile = loadUpdatesFromFile();

        if(updateFromFile == null) {
            log.info("Creating new update store");
            update = new Update();
        } else {
            update = updateFromFile;
        }

        peerManager.registerService(DatabaseUpdateService.class, this);

        /*
         * Initially do a sequential sync with each peer. Future syncing
         * is done in a multithreaded fashion.
         */
        for(Peer peer : peerManager.getPeers())
            syncWithPeer(peer);

        syncBumpThread.start();
        syncControlThread.start();
    }

    private Thread syncBumpThread = new Thread() {
        @Override
        public void run() {
            while(!shutdown) {
                syncWithPeers();
                try {
                    Thread.sleep(60000);
                } catch(InterruptedException e) { }
            }
        }
    };

    private Thread syncControlThread = new Thread() {
        @Override
        public void run() {
            while(!shutdown) {
                /* Gets the next peer that needs synchronizing from the queue*/
                final Peer peer;
                try {
                    peer = syncQueue.take();
                } catch(InterruptedException e) { continue; }

                /* If the peer is currently synchronizing, it is added to the list of pending peers */
                if(synchronizingPeers.contains(peer)) {
                    pendingPeers.add(peer);
                    continue;
                }

                /* Otherwise, it is added to the synchronizing list */
                synchronizingPeers.add(peer);

                /* Synchronization with the peer is started in a new thread */
                new Thread() {
                    @Override
                    public void run() {
                        syncWithPeer(peer);
                        /* Synchronization is done - remove peer from synchronizing list */
                        synchronizingPeers.remove(peer);
                        /* If peer is pending, it is added to the syncQueue again */
                        if(pendingPeers.remove(peer)) {
                            try {
                                syncQueue.put(peer);
                            } catch(InterruptedException e) {}
                        }
                    }
                }.start();
            }
        }
    };

    private void syncWithPeers() {
        for(Peer peer : peerManager.getPeers()) {
            syncQueue.offer(peer);
        }
    }

    private void syncWithPeer(Peer peer) {
        /* ignore self */
        if(peer.getUUID().equals(ourID))
            return;

        log.info("Syncing with peer " + peer + " (Stage 1)");

        DatabaseUpdateService updateService;
        try {
            updateService = peer.getService(DatabaseUpdateService.class);
        } catch (NoSuchServiceException e) {
            log.info("Peer " + peer + " has no update service");
            /* peer does not have an active update service, bail */
            return;
        }

        /*** STAGE ONE: IDENTIFY OURSELVES AND ASK PEER TO AUTHENTICATE THEMSELF ***/

        UpdateSyncInfo syncInfo = new UpdateSyncInfo();

        syncInfo.senderID = ourID;
        syncInfo.databaseID = databaseID;
        syncInfo.authPrefix = AuthenticationUtils.generatePrefix();
        syncInfo.senderTime = new Timestamp();
        log.debug("our time: " + new Timestamp());

        UpdateSyncInfo peerSyncInfo = null;
        try {
            peerSyncInfo = updateService.sync(syncInfo);
        } catch(RemoteAccessException e) {
            log.warn("Exception while communicating with peer, aborting sync: " + e.getMessage());
            return;
        }

        if(peerSyncInfo.status != UpdateSyncInfo.Status.OK) {
            log.info("Peer " + peer + " does not wish to sync with us (" + peerSyncInfo.status + ")");
            return;
        }

        if( !AuthenticationUtils.checkAuthenticationPair(
                new AuthenticationPair(syncInfo.authPrefix, peerSyncInfo.authHash),
                passwordHash) ) {
            log.warn("Peer " + peer + " failed to authenticate with us.");
            return;
        }

        Clock.setEarliestTime(peerSyncInfo.senderTime);

        /*** PEER WISHES TO SYNC AND HAS AUTHENTICATED THEMSELF ***/

        /*** STAGE 2: SEND UPDATE DATA AND ASK PEER FOR MISSING DATA ***/

        log.info("Syncing with peer " + peer + " (Stage 2)");

        syncInfo = new UpdateSyncInfo();

        syncInfo.senderID = ourID;
        syncInfo.senderTime = new Timestamp();
        syncInfo.databaseID = databaseID;
        syncInfo.update = update.afterPosition(peerSyncInfo.position);
        syncInfo.authHash = AuthenticationUtils.getAuthenticationPair(peerSyncInfo.authPrefix, passwordHash).getHash();
        syncInfo.position = update.getPosition();

        log.info("Received reply position:\n" + peerSyncInfo.position);
        log.info("Sending update:\n" + syncInfo.update.dumpTable());
        log.info("Sending position:\n" + syncInfo.position);

        peerSyncInfo = updateService.sync(syncInfo);

        if(peerSyncInfo.status != UpdateSyncInfo.Status.OK) {
            log.info("Peer " + peer + " does not wish to sync with us (" + peerSyncInfo.status + ") (Stage 2)");
            return;
        }

        if(peerSyncInfo.update != null) {
            log.info("Received reply update:\n" + peerSyncInfo.update.dumpTable());
            handlePeerUpdate(peerSyncInfo.senderID, peerSyncInfo.senderTime, peerSyncInfo.update);
        }
    }

    @Override
    public UpdateSyncInfo sync(UpdateSyncInfo peerSyncInfo) {
        log.info("Received sync request from peer ID " + peerSyncInfo.senderID);

        UpdateSyncInfo syncInfo = new UpdateSyncInfo();

        syncInfo.senderID = ourID;
        syncInfo.senderTime = new Timestamp();
        syncInfo.databaseID = databaseID;

        /* database IDs must match */
        if(!peerSyncInfo.databaseID.equals(databaseID)) {
            syncInfo.status = UpdateSyncInfo.Status.DATABASE_ID_MISMATCH;
            return syncInfo;
        }

        /* only one of authHash or authPrefix may be specified */
        if( !((peerSyncInfo.authHash == null) ^ (peerSyncInfo.authPrefix == null))) {
            syncInfo.status = UpdateSyncInfo.Status.BAD_INFO;
            return syncInfo;
        }

        /* we have been sent a prefix. send back a hash for this prefix, a new prefix, and our update position */
        if(peerSyncInfo.authPrefix != null) {
            syncInfo.authHash = AuthenticationUtils.getAuthenticationPair(peerSyncInfo.authPrefix, passwordHash).getHash();
            syncInfo.authPrefix = AuthenticationUtils.generatePrefix();
            peerPrefixes.put(peerSyncInfo.senderID, syncInfo.authPrefix);
            syncInfo.position = update.getPosition();
            log.info("Returning position:\n" + syncInfo.position);
            syncInfo.status = UpdateSyncInfo.Status.OK;
            return syncInfo;
        }

        /* we have been sent a hash. authenticate the hash and accept the data */
        else if(peerSyncInfo.authHash != null) {
            byte[] prefix = peerPrefixes.get(peerSyncInfo.senderID);
            if(AuthenticationUtils.checkAuthenticationPair(new AuthenticationPair(prefix, peerSyncInfo.authHash), passwordHash)) {
                Clock.setEarliestTime(peerSyncInfo.senderTime);

                syncInfo.status = UpdateSyncInfo.Status.OK;
                syncInfo.update = update.afterPosition(peerSyncInfo.position);

                log.info("Received update:\n" + peerSyncInfo.update.dumpTable());
                log.info("Received position:\n" + peerSyncInfo.position);
                log.info("Returning update:\n" + syncInfo.update.dumpTable());

                handlePeerUpdate(peerSyncInfo.senderID, peerSyncInfo.senderTime, peerSyncInfo.update);
            } else {
                syncInfo.status = UpdateSyncInfo.Status.AUTH_FAILED;
            }
            return syncInfo;
        }

        throw new RuntimeException("Should never happen");
    }


    private void handlePeerUpdate(UUID peerID, Date peerTime, Update updateFromPeer) {
        /*
        long timeDiff = (new Date().getTime()) - peerTime.getTime();
        updateFromPeer.adjustTimestamps(timeDiff);
         */

        /* add to update table */
        Update mergedUpdate = update.mergeWith(updateFromPeer);

        /* update the database */
        for(DataEvent event : mergedUpdate.getAllEventsOrdered()) {
            try {
                databaseUpdater.handleDataEvent(event);
            } catch(Exception e) {
                log.error("Exception while applying a peer update to database", e);
            }
        }
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        /* add to update table */
        update.addEvents(ourID, events);

        /* update the database */
        try {
            for(DataEvent event : events)
                databaseUpdater.handleDataEvent(event);
        } catch(Exception e) {
            log.error("Exception whil applying local update to database", e);
        }

        /* sync with peers */
        syncWithPeers();
    }

    private Update loadUpdatesFromFile() {
        try {
            File updateFile = new File(updateFileName);
            FileInputStream fis = new FileInputStream(updateFile);
            ObjectInputStream reader = new ObjectInputStream(fis);
            Update loadedUpdate = (Update)reader.readObject();
            reader.close();
            fis.close();
            return loadedUpdate;
        } catch (ClassNotFoundException cnfe) {
            log.error("Couldn't deserialize update object - class unknown", cnfe);
        } catch (FileNotFoundException fnfe) {
            log.info("Updates file not found");
        } catch (IOException ioe) {
            log.error("Input/output error while loading updates file", ioe);
        }

        return null;
    }

    public void saveUpdatesToFile() {
        try {
            log.info("Saving updates to file");
            File updateFile = new File(updateFileName);
            FileOutputStream fos = new FileOutputStream(updateFile);
            ObjectOutputStream writer = new ObjectOutputStream(fos);
            synchronized(update) {
                writer.writeObject(update);
            }
            writer.close();
            fos.close();
        } catch(Throwable e) { // we really don't want this thread to die due to uncaught throwable
            log.error("Unable to write updates to file", e);
        }
    }

    void shutdown() {
        shutdown = true;

        syncControlThread.interrupt();
        syncBumpThread.interrupt();
        /* Give them half a second to shutdown nicely */
        try {
            syncControlThread.join(500);
            syncBumpThread.join(500);
        } catch(InterruptedException e) {}
        /* Log if the threads haven't died yet */
        if(syncControlThread.isAlive())
            log.warn("Sync control thread didn't shutdown quickly");
        if(syncBumpThread.isAlive())
            log.warn("Sync bump thread didn't shutdown quickly");
    }
}
