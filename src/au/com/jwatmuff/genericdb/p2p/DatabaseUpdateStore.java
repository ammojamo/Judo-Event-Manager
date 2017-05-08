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
