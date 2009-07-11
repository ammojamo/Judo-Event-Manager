/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import au.com.jwatmuff.eventmanager.Main;
import au.com.jwatmuff.genericdb.cache.CachingDatabase;
import au.com.jwatmuff.genericdb.p2p.AuthenticationUtils.AuthenticationPair;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericp2p.NoSuchServiceException;
import au.com.jwatmuff.genericp2p.Peer;
import au.com.jwatmuff.genericp2p.PeerConnectionEvent;
import au.com.jwatmuff.genericp2p.PeerConnectionListener;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public abstract class DatabaseManager {
    private static final Logger log = Logger.getLogger(DatabaseManager.class);

    public static final String DATABASE_INFO_SERVICE = "databaseInfoService";
    
    private File databaseStore;
    private PeerManager peerManager;
    
    private Map<UUID, DatabaseInfo> databases = new HashMap<UUID, DatabaseInfo>();
    
    private Listener listener;
    
    private DistributedDatabase activeDatabase;
    
    private DatabaseInfoService infoService = new DatabaseInfoService() {
        @Override
        public DatabaseInfo getDatabaseInfo() {
            if(activeDatabase != null) {
                log.info("Received request for database info, return info");
                DatabaseInfo info = new DatabaseInfo();
                info.name = activeDatabase.getName();
                info.id = activeDatabase.getID();
                info.passwordHash = activeDatabase.getPasswordHash();
                info.version = Main.VERSION;
                return info;
            }
            else {
                log.info("Received request for database info, return null");
                return null;
            }
        }

        @Override
        public boolean checkAuthentication(byte[] prefix, byte[] hash) {
            if(activeDatabase != null) {
                AuthenticationPair pair = new AuthenticationPair(prefix, hash);
                return AuthenticationUtils.checkAuthenticationPair(pair, activeDatabase.getPasswordHash());
            } else {
                log.warn("Received authentication check when no database is active");
                return false;
            }
        }

        @Override
        public void handleDatabaseAnnouncement(UUID peerID, DatabaseInfo database) {
            log.info("Received database announcement from " + peerID);
            updateAllDatabaseInfo();
            if(listener != null) listener.handleDatabaseManagerEvent();
        }
    };
    
    public DatabaseManager(File databaseStore, PeerManager peerManager) {
        this.databaseStore = databaseStore;
        this.peerManager = peerManager;
        
        peerManager.registerService(DatabaseInfoService.class, infoService);
        
        peerManager.addConnectionListener(new PeerConnectionListener() {
            @Override
            public void handleConnectionEvent(PeerConnectionEvent evt) {
                updateAllDatabaseInfo();
                if(listener != null) listener.handleDatabaseManagerEvent();
            }
        });
        
        if(!databaseStore.exists())
            if(!databaseStore.mkdir())
                throw new RuntimeException("Unable to create database store directory " + databaseStore.getAbsolutePath());
        
        updateAllDatabaseInfo();
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    public Collection<DatabaseInfo> getDatabases() {
        return new ArrayList<DatabaseInfo>(databases.values());
    }
    
    private void createDatabaseDirectory(DatabaseInfo database) {
        File dbDir = new File(databaseStore, database.id.toString());
        dbDir.mkdir();
        
        Properties props = new Properties();
        props.setProperty("UUID", "" + database.id);
        props.setProperty("name", database.name);
        props.setProperty("password", "" + database.passwordHash);
        props.setProperty("version", database.version);
        
        File infoFile = new File(dbDir, "info.dat");
        
        try {
            props.store(new FileOutputStream(infoFile), null);
        } catch (IOException e) {
            log.error("Unable to write database info to file", e);
        }
        
        database.localDirectory = dbDir;
    }
    
    public DatabaseInfo createNewDatabase(String databaseName, int passwordHash) {
        DatabaseInfo database = new DatabaseInfo();
        database.name = databaseName;
        database.id = UUID.randomUUID();
        database.passwordHash = passwordHash;
        database.local = true;
        database.version = Main.VERSION;

        createDatabaseDirectory(database);
        
        updateAllDatabaseInfo();

        return database;
    }
    
    public boolean authenticate(DatabaseInfo database, int passwordHash) {
        if(database.local) {
            if(passwordHash == database.passwordHash)
                return true;
            else {
                log.info("Authentication failed: local password did not match");
                return false;
            }
        } else {
            for(Peer peer : peerManager.getPeers()) {
                // Find out if this peer is hosting our desired database
                DatabaseInfoService peerInfoService = null;
                try {
                    peerInfoService = peer.getService(DatabaseInfoService.class);
                } catch(NoSuchServiceException e) { }
                if(infoService == null) continue;
                DatabaseInfo info = peerInfoService.getDatabaseInfo();
                if(info == null) continue;
                if(!info.id.equals(database.id)) continue;

                AuthenticationPair pair = AuthenticationUtils.getAuthenticationPair(passwordHash);
                if(peerInfoService.checkAuthentication(pair.getPrefix(), pair.getHash())) {
                    return true;
                } else {
                    log.info("Authentication failed: authentication not accepted by peer");
                    return false;
                }
            }
        }
        
        log.error("No peer found to authenticate with");
        return false;
    }
    
    protected abstract TransactionalDatabase getLocalDatabase(DatabaseInfo database);
    
    public DistributedDatabase activateDatabase(UUID databaseID, int passwordHash) {
        DatabaseInfo database = databases.get(databaseID);
        if(database == null)
            throw new RuntimeException("No database with ID " + databaseID + "found");

        if(!authenticate(database, passwordHash))
            throw new RuntimeException("Authentication failed");
        
        if(!database.local)
            createDatabaseDirectory(database);
        
        TransactionalDatabase localDb = getLocalDatabase(database);
        
        TransactionalDatabase localCachedDb = new CachingDatabase(localDb);
        
        DistributedDatabase distDb = new DistributedDatabase(
                database.id,
                database.name,
                passwordHash,
                localCachedDb,
                peerManager,
                new File(database.localDirectory, "update.dat"));
        
        activeDatabase = distDb;
        
        for(Peer peer : peerManager.getPeers()) {
            try {
                peer.getService(DatabaseInfoService.class).handleDatabaseAnnouncement(peerManager.getUUID(), database);
            } catch(NoSuchServiceException e) {
                log.error("Failed to get DatabaseInfoService for peer " + peer.getName(), e);
            }
        }
        
        return distDb;
    }

    public void deactivateDatabase() {
        activeDatabase = null;
        for(Peer peer : peerManager.getPeers()) {
            try {
                peer.getService(DatabaseInfoService.class).handleDatabaseAnnouncement(peerManager.getUUID(), null);
            } catch(NoSuchServiceException e) {
                log.error("Failed to get DatabaseInfoService for peer " + peer.getName(), e);
            }
        }
    }

    //Bit of a hack making this public - to help refresh load competition screen
    public synchronized void updateAllDatabaseInfo() {
        databases.clear();
        updateLocalDatabaseInfo();
        updatePeerDatabaseInfo();
    }
    
    private void updatePeerDatabaseInfo() {
        Collection<Peer> peers = peerManager.getPeers();
        
        log.info("Asking for database info from " + peers.size() + " peers");

        for(Peer peer : peers) {
            log.info("Asking for database info from peer " + peer.getName());
            try {
                DatabaseInfo info = peer.getService(DatabaseInfoService.class).getDatabaseInfo();
                if(info == null) {
                    log.info("No database info available from " + peer.getName());
                }
                else {
                    log.info("Got database info from " + peer.getName() + " : " + info.name);
                    if(databases.containsKey(info.id)) {
                        DatabaseInfo oldDi = databases.get(info.id);
                        oldDi.name = info.name;
                        info = oldDi;
                    }
                    info.peers++;
                    databases.put(info.id, info);
                }
            } catch(NoSuchServiceException e) {
                log.info("No database info available from " + peer.getName());
            } catch(Exception e) {
                log.error("Exception while getting database info from peer " + peer.getName(), e);
            }
        }
    }
    
    private void updateLocalDatabaseInfo() {
        log.info("Updating Local Database Info");
        /* find all subdirectories in the comps directory */
        File[] dbDirs = databaseStore.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        /* iterate through directories */
        for(File dbDir : dbDirs) {
            File dbInfo = new File(dbDir, "info.dat");
            if(dbInfo.exists()) {
                try {
                    Properties props = new Properties();
                    InputStream is = new FileInputStream(dbInfo);
                    props.load(is);
                    is.close();

                    try {
                        DatabaseInfo info = new DatabaseInfo();
                        info.id = UUID.fromString(props.getProperty("UUID"));
                        if(databases.containsKey(info.id))
                            info = databases.get(info.id);
                        info.local = true;
                        info.localDirectory = dbDir;
                        info.name = props.getProperty("name");
                        info.passwordHash = Integer.parseInt(props.getProperty("password"));
                        info.version = props.getProperty("version");
                        databases.put(info.id, info);
                    } catch(Exception e) {
                        log.warn("Unable to read competition info from " + dbInfo, e);
                    }
                } catch(IOException e) {
                    log.error("I/O error while reading competition info from " + dbInfo.getPath(), e);
                }
            }
        }        
    }
    
    public interface Listener {
        public void handleDatabaseManagerEvent();
    }
}
