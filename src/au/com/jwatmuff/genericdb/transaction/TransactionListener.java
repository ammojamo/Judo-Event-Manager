/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.transaction;

import au.com.jwatmuff.genericdb.distributed.DataEvent;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author James
 */
public interface TransactionListener {
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses);
}
