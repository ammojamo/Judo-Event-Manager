package au.com.jwatmuff.genericdb.p2p;

import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import au.com.jwatmuff.genericdb.transaction.NotifyingTransactionalDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabaseUpdater;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.util.UUID;
import org.apache.log4j.Logger;

public class DistributedDatabase extends NotifyingTransactionalDatabase {
    private static final Logger log = Logger.getLogger(DistributedDatabase.class);
    
    private final UpdateManager updateManager;
    private final TransactionalDatabaseUpdater updater;
    private final NotifyingTransactionalDatabase databaseForUpdater;
    private final PeerManager peerManager;

    private final int passwordHash;
    private final String name;
    private final UUID id;

    public DistributedDatabase(UUID id,
                               String dbName,
                               int passwordHash,
                               TransactionalDatabase localDatabase,
                               PeerManager peerManager,
                               UpdateStore updateStore) {
        super(localDatabase);
        super.setReadOnly(true);
        super.setUpdateTimestamps(true);

        this.peerManager = peerManager;

        databaseForUpdater = new NotifyingTransactionalDatabase(localDatabase);
        databaseForUpdater.setReadOnly(false);
        databaseForUpdater.setUpdateTimestamps(false);

        updater = new TransactionalDatabaseUpdater(databaseForUpdater);
        updateManager = new UpdateManager(
                peerManager,
                updater,
                updateStore,
                id,
                passwordHash);

        TransactionNotifier notifier = new TransactionNotifier();
        notifier.addListener(updateManager);
        super.setListener(notifier);


        this.id = id;
        this.name = dbName;
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public UUID getID() {
        return id;
    }

    public int getPasswordHash() {
        return passwordHash;
    }

    @Override
    public void setListener(DataEventListener listener) {
        databaseForUpdater.setListener(listener);
    }
    
    public void shutdown() {
        updateManager.shutdown();
    }
}
