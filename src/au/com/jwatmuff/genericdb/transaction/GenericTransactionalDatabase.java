/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.transaction;

import au.com.jwatmuff.genericdb.*;

/**
 *
 * @author James
 */
public class GenericTransactionalDatabase extends GenericDatabase implements TransactionalDatabase {
    @Override
    public void perform(Transaction t) {
        t.perform();
    }
}
