/*
 * Session.java
 *
 * Created on 18 August 2008, 17:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;

/**
 *
 * @author James
 */
public class Session extends DistributableObject<Integer> {
    public static enum LockedStatus {
        UNLOCKED, POSITION_LOCKED, FIGHTS_LOCKED
    }
    
    public static enum SessionType {
        NORMAL, MAT
    }
    
    private SessionType type;
    private String name;
    private LockedStatus lockedStatus = LockedStatus.UNLOCKED;
    private String mat;
    
    /** Creates a new instance of Session */
    public Session() {
        setID(IDGenerator.generate());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LockedStatus getLockedStatus() {
        return lockedStatus;
    }

    public void setLockedStatus(LockedStatus lockedStatus) {
        this.lockedStatus = lockedStatus;
    }
    
    public String getMat() {
        return mat;
    }

    public void setMat(String mat) {
        this.mat = mat;
    }
    
    public static Session getLockedCopy(Session s, LockedStatus lockedStatus) {
        assert lockedStatus.compareTo(s.getLockedStatus()) > 0;
        
        s = ObjectCopier.copy(s);
        s.setID(IDGenerator.generate());
        s.setLockedStatus(lockedStatus);
        return s;
    }

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }
}
