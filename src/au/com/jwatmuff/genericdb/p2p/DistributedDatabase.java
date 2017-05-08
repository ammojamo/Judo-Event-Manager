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
