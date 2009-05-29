/*
 * DistributableObject.java
 *
 * Created on 12 August 2008, 11:48
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import java.io.Serializable;

/**
 *
 * @author James
 */
public class DistributableObject<K> implements Distributable<K>, Serializable {
    private K id;
    private boolean valid = true;
    private Timestamp timestamp;

    /** Creates a new instance of DistributableObject */
    public DistributableObject() {
    }

    @Override
    public void setID(K id) {
        this.id = id;
    }

    @Override
    public K getID() {
        return id;
    }

    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void setTimestamp(Timestamp t) {
        this.timestamp = new Timestamp(t.getTime());
    }

    @Override
    public Timestamp getTimestamp() {
        return new Timestamp(timestamp.getTime());
    }    
}
