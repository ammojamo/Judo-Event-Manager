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

package au.com.jwatmuff.genericdb.transaction;

import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DatabaseUpdater;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author James
 */
public class TransactionalDatabaseUpdater extends DatabaseUpdater {
    boolean inTransaction = false;
    Collection<DataEvent> events = new ArrayList<DataEvent>();
    private TransactionalDatabase database;

    public TransactionalDatabaseUpdater(TransactionalDatabase database) {
        super(database);
        this.database = database;
    }

    // Expose the 'perform' method of the database so we can do a batch of updates
    // in a single transaction if needed (e.g. for initial sync)
    public void perform(Transaction t) {
        this.database.perform(t);
    }

    @Override
    public void handleDataEvent(DataEvent event) {
        if(!inTransaction) {
            switch(event.getTransactionStatus()) {
                case BEGIN:
                    inTransaction = true;
                    events.clear();
                    events.add(event);
                    break;
                case END:
                case CURRENT:
                    throw new RuntimeException("Invalid transaction status " + event.getTransactionStatus() + ": no transaction present.");
                case NONE:
                    superHandleDataEvent(event);
            }
        } else {
            switch(event.getTransactionStatus()) {
                case END:
                    inTransaction = false;
                    events.add(event);
                    database.perform(new Transaction() {
                        @Override
                        public void perform() {
                            for(DataEvent e : events)
                                superHandleDataEvent(e);
                        }
                    });
                    break;
                case BEGIN:
                case NONE:
                    throw new RuntimeException("Invalid transaction status " + event.getTransactionStatus() + ": a transaction is present.");
                case CURRENT:
                    events.add(event);
            }
        }
    }
    
    private void superHandleDataEvent(DataEvent event) {
        super.handleDataEvent(event);
    }
}
