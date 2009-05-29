/*
 * Distributable.java
 *
 * Created on 12 August 2008, 02:01
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

/**
 *
 * @author James
 */
public interface Distributable<K> {
    public void setID(K id);
    public K getID();
    public void setValid(boolean valid);
    public boolean isValid();
    public void setTimestamp(Timestamp t);
    public Timestamp getTimestamp();
}
