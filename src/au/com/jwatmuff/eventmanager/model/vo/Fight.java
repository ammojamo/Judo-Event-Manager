/*
 * Fight.java
 *
 * Created on 14 August 2008, 18:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class Fight extends DistributableObject<Integer> implements Serializable {
    private int poolID;
    private String[] playerCodes = new String[] { "", "" };
    private int position;
    
    private boolean locked = false;
    
    /** Creates a new instance of Fight */
    public Fight() {
        setID(IDGenerator.generate());
    }
    
    public Fight(boolean locked) {
        this();
        setLocked(locked);
    }

    public int getPoolID() {
        return poolID;
    }

    public void setPoolID(int poolID) {
        this.poolID = poolID;
    }

    public String[] getPlayerCodes() {
        return playerCodes;
    }

    public void setPlayerCodes(String[] playerCodes) {
        assert playerCodes != null;
        assert playerCodes.length == 2;
        assert playerCodes[0] != null;
        assert playerCodes[1] != null;

        this.playerCodes = playerCodes;
    }
    
    public void setPlayerCode(int i, String code) {
        assert code != null;

        this.playerCodes[i] = code;
    }

    public boolean isLocked() {
        return locked;
    }

    private void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
    
    public static Fight getLockedCopy(Fight f, Pool p) {
        assert !f.isLocked() : "fight already locked";
        assert p.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED : "must be attached to a locked pool";
        f = ObjectCopier.copy(f);
        f.setID(IDGenerator.generate());
        f.setPoolID(p.getID());
        f.setLocked(true);
        
        return f;
    }

    @Override
    public String toString() {
        return "[ID: " + getID() + ", pool: " + poolID + ", playerCodes: (" + playerCodes[0] + ", " + playerCodes[1] + "), position: " + position + "]";
    }
}
