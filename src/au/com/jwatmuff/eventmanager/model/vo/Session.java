/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
