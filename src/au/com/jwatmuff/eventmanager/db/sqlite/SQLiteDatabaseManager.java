/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.genericdb.p2p.DatabaseInfo;
import au.com.jwatmuff.genericdb.p2p.DatabaseManager;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import org.apache.commons.dbcp.BasicDataSource;

/**
 *
 * @author James
 */
public class SQLiteDatabaseManager extends DatabaseManager {
    public SQLiteDatabaseManager(File databaseStore, PeerManager peerManager) {
        super(databaseStore, peerManager);
    }
            
    @Override
    protected TransactionalDatabase getLocalDatabase(DatabaseInfo database) {
        BasicDataSource dataSource = new BasicDataSource();
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

}
