/*
 * Timestamp.java
 *
 * Created on 11 March 2008, 00:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author James
 */
public class Timestamp extends Date implements Serializable {
    
    /** Creates a new instance of Timestamp */
    public Timestamp() {
        super();
    }
    
    public Timestamp(long date) {
        super(date);
    }

    public Timestamp(Date t) {
        this(t.getTime());
    }
}
