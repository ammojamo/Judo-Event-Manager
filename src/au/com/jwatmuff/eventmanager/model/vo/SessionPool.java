/*
 * SessionPool.java
 *
 * Created on 18 August 2008, 19:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class SessionPool extends DistributableObject<SessionPool.Key> {
    // immutable
    public static class Key implements Serializable {
        public final int sessionID;
        public final int poolID;
        
        public Key(int sessionID, int poolID) {
            this.sessionID = sessionID;
            this.poolID = poolID;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (this.sessionID != other.sessionID) {
                return false;
            }
            if (this.poolID != other.poolID) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + this.sessionID;
            hash = 59 * hash + this.poolID;
            return hash;
        }
    }
    
    /** Creates a new instance of SessionPool */
    public SessionPool() {
        setID(new Key(0,0));
    }
    
    public int getSessionID() {
        return getID().sessionID;
    }
    
    public int getPoolID() {
        return getID().poolID;
    }
}
