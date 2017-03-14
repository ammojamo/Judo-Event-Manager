/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import au.com.jwatmuff.eventmanager.test.TestUtil;
import au.com.jwatmuff.eventmanager.util.EventBus;
import au.com.jwatmuff.genericdb.distributed.Clock;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import au.com.jwatmuff.genericdb.p2p.AuthenticationUtils.AuthenticationPair;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabaseUpdater;
import au.com.jwatmuff.genericp2p.NoSuchServiceException;
import au.com.jwatmuff.genericp2p.Peer;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private final Update updateTable;
    private final UpdateStore updateStore;

    private PeerManager peerManager;
    private TransactionalDatabaseUpdater databaseUpdater;
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
            UpdateStore     updateStore,
            UUID            databaseID,
            int             passwordHash) {
        this.peerManager = peerManager;
        this.databaseUpdater = databaseUpdater;
        this.updateStore = updateStore;
        this.databaseID = databaseID;
        this.passwordHash = passwordHash;
        
        ourID = peerManager.getUUID();
        
        Update updateFromStore = null;
        try {
            updateFromStore = updateStore.loadUpdate();
        } catch(IOException e) {
            log.error("Unable to load update store");
        }
        
        if(updateFromStore == null) {
            updateTable = new Update();
        } else {
            updateTable = updateFromStore;
        }

        updateTable.updateClock();
        
        /* Write any uncommitted events from the update store */
        Update uncommitted = updateTable.afterPosition(updateStore.getCommittedPosition());
        log.debug("Uncommitted: " + uncommitted);
        if(uncommitted.size() > 0) {
            log.info("Recovering uncommitted data");
            handleUpdate(uncommitted, true, true);
            log.info("Finished recovery");
        }

        peerManager.registerService(DatabaseUpdateService.class, this);

        /*
         * Initially do a sequential sync with each peer. Future syncing
         * is done in a multithreaded fashion.
         */
        for(final Peer peer : peerManager.getPeers()) {
            try {
                databaseUpdater.perform(new Transaction() {
                    @Override
                    public void perform() {
                        syncWithPeer(peer);
                    }
                });
            } catch(Exception e) {
                log.error("Exception performing initial sync with peer", e);
            }
        }

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

        EventBus.send("sync-status", "Receiving update");
        
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

        UpdateSyncInfo peerSyncInfo;
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
        syncInfo.update = updateTable.afterPosition(peerSyncInfo.position);
        syncInfo.authHash = AuthenticationUtils.getAuthenticationPair(peerSyncInfo.authPrefix, passwordHash).getHash();
        syncInfo.position = updateTable.getPosition();

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
            syncInfo.position = updateTable.getPosition();
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
                syncInfo.update = updateTable.afterPosition(peerSyncInfo.position);

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

    private void handleUpdate(Update update, boolean trackProgress, boolean recovering) {
        if(!recovering) {
            /* add to update table */
            /* update will now only contain events that were not already present in the update table */
            update = updateTable.mergeWith(update);
        }

        /* verification checks - for debugging, disable for deployment */
        update.verifyTransactionStates();

        /* write to update file */
        if(!recovering) {
            try {
                updateStore.writePartialUpdate(update);
            } catch(IOException e) {
                log.error("Critical error - failed to write update to file");
            }
        }

        /* update the database */
        int i = 0;
        List<DataEvent> allEvents = update.getAllEventsOrdered();
        for(DataEvent event : allEvents) {
            // Update progress bar (if any)
            i++;
            if(trackProgress && i % 47 == 0) {
                EventBus.send("sync-status", String.format("Processing update %d / %d", i, allEvents.size()));
            }

            try {
                databaseUpdater.handleDataEvent(event);
            } catch(Exception e) {
                log.error("Exception while applying a peer update to database", e);
            }
            
//             For Testing only - Simulate random failure
             if(TestUtil.SIMULATE_FLAKINESS && !recovering && Math.random() > 0.995) System.exit(1);
        }

        /* write committed position */
        try {
            updateStore.writeCommitedPosition(update.getPosition());
        } catch(IOException e) {
            log.error("Critical error - failed to write committed position");
        }
    }

    private void handlePeerUpdate(UUID peerID, Date peerTime, Update updateFromPeer) {
        handleUpdate(updateFromPeer, true, false);
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        /* Convert events to an update object so we can send to handleUpdate */
        Update update = updateTable.forPeer(ourID).afterPosition(updateTable.getPosition());
        update.addEvents(ourID, events);
        
        handleUpdate(update, false, false);

        /* sync with peers */
        syncWithPeers();
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
