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

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.genericdb.p2p.DatabaseInfo;
import au.com.jwatmuff.genericdb.p2p.DatabaseManager;
import au.com.jwatmuff.genericdb.p2p.DatabaseUpdateStore;
import au.com.jwatmuff.genericdb.p2p.UpdateStore;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.sql.SQLException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SQLiteDatabaseManager extends DatabaseManager {
    private static final Logger log = Logger.getLogger(SQLiteDatabaseManager.class);

    private BasicDataSource dataSource;
    public SQLiteDatabaseManager(File databaseStore, PeerManager peerManager) {
        super(databaseStore, peerManager);
    }
            
    @Override
    protected TransactionalDatabase getLocalDatabase(DatabaseInfo database) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(org.sqlite.JDBC.class.getName());
        dataSource.setUrl("jdbc:sqlite:" + database.localDirectory + "/database.db");
        //dataSource.setUrl("jdbc:sqlite::memory:");
        dataSource.setInitialSize(1); //only one connection - important if we use in memory sqlite database
        dataSource.setMaxActive(1); 
        dataSource.setMaxWait(0); // wait indefinitely for connection to be free
        
        SQLiteDatabase localDb = new SQLiteDatabase();
        localDb.setDataSource(dataSource);
        localDb.afterPropertiesSet();
        
        return localDb;
    }

    @Override
    protected UpdateStore getUpdateStore() {
        return DatabaseUpdateStore.withDataSource(dataSource);
    }

    @Override
    public void deactivateDatabase(long timeoutMillis) {
        super.deactivateDatabase(timeoutMillis);
        try {
            dataSource.close();
        } catch(SQLException e) {
            log.error("SQLError while closing SQLite data source", e);
        }
    }
}
