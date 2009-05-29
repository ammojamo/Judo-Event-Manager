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
public interface TransactionalDatabase extends Database {
    public void perform(Transaction t);
}
