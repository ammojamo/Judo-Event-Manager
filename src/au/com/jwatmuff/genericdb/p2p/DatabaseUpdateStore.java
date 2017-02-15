/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericdb.p2p;

import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * UpdateStore implementation that uses the database to store data.
 * See UpdateStore.java for more information.
 * 
 * @author james
 */
public class DatabaseUpdateStore implements UpdateStore {
    private static final Logger log = Logger.getLogger(DatabaseUpdateStore.class);
    private UpdatePosition committedPosition = new UpdatePosition();
    private final JdbcTemplate template;
    
    // FST is an alternative to Java serialization method that is much more
    // efficient.
    static final FSTConfiguration FST_CONFIG = FSTConfiguration.createDefaultConfiguration();

    public static DatabaseUpdateStore withDataSource(BasicDataSource dataSource) {
        return new DatabaseUpdateStore(dataSource);
    }
            
    private DatabaseUpdateStore(BasicDataSource dataSource) {
        template = new JdbcTemplate(dataSource);        
        template.update("CREATE TABLE IF NOT EXISTS update_log (" +
                "data BLOB" +
                ")");
    }

    @Override
    public Update loadUpdate() throws IOException {
        Update update = new Update();
        
        log.debug("Loading update data");
        SqlRowSet rows = template.queryForRowSet("SELECT data FROM update_log");
        while(rows.next()) {
            try {
                byte[] bytes = (byte[]) rows.getObject(1);
//                Serializable object = ObjectCopier.bytesToObject(bytes);
                Object object = FST_CONFIG.asObject(bytes);

                if(object instanceof Update) {
                    log.debug(object);
                    update.mergeWith((Update)object);
                }
                if(object instanceof UpdatePosition) {
                    log.debug(object);
                    committedPosition = (UpdatePosition)object;
                }
            } catch(InvalidResultSetAccessException e) {
                log.error("Error processing update log", e);
            }
        }

        log.debug("Finished update load");

        return update;
    }

    @Override
    public void writePartialUpdate(Update update) throws IOException {
        writeObject(update);
    }

    @Override
    public void writeCommitedPosition(UpdatePosition position) throws IOException {
        committedPosition = position;
        writeObject(position);
    }
    
    private void writeObject(Serializable object) {
        try {
//            byte[] bytes = ObjectCopier.objectToBytes(object);
            byte[] bytes = FST_CONFIG.asByteArray(object);
            template.update("INSERT INTO update_log (data) VALUES (?)", new Object[]{ bytes });
        } catch(DataAccessException e) {
            log.error("CRITICAL - Failed to write object to update log. Ability to recover from crashes is compromised", e);
        }
    }

    @Override
    public UpdatePosition getCommittedPosition() {
        return committedPosition;
    }
}
