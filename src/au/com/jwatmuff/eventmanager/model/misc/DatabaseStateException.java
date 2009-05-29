/*
 * InvalidDatabaseStateException.java
 *
 * Created on 30 August 2008, 05:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

/**
 * Indicates that the database is not in a valid state for performing a particular
 * operation. For example, required data may be missing, or the data may be
 * inconsistent.
 * 
 * @author James
 */
public class DatabaseStateException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>InvalidDatabaseStateException</code> without detail message.
     */
    public DatabaseStateException() {
    }
    
    
    /**
     * Constructs an instance of <code>InvalidDatabaseStateException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DatabaseStateException(String msg) {
        super(msg);
    }
}
