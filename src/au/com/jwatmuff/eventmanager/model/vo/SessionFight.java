/*
 * SessionFight.java
 *
 * Created on 20 August 2008, 15:07
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
public class SessionFight extends DistributableObject<SessionFight.Key> {
    private int position;

    // immutable
    public static class Key implements Serializable {
        public final int sessionID;
        public final int fightID;
        
        public Key(int sessionID, int fightID) {
            this.sessionID = sessionID;
            this.fightID = fightID;
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
            if (this.fightID != other.fightID) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + this.sessionID;
            hash = 97 * hash + this.fightID;
            return hash;
        }
    }

    /** Creates a new instance of SessionFight */
    public SessionFight() {
        setID(new Key(0,0));
    }

    public int getSessionID() {
        return getID().sessionID;
    }
    
    public int getFightID() {
        return getID().fightID;
    }    

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
