/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
