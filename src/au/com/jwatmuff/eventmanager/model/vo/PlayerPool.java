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

import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class PlayerPool extends DistributableObject<PlayerPool.Key> implements Serializable {  
    public static enum Status {
        NONE, OK, WITHDRAWN, DISQUALIFIED;

        public static Status fromString(String name) {
            return (name == null) ? NONE : valueOf(name);
        }
    }

    private int playerPosition;
    private int playerPosition2;
    private int seed;

    private boolean approved = false;
    private boolean locked = false;
    private Status status = Status.OK;

    // immutable
    public static class Key implements Serializable {
        public final int playerID;
        public final int poolID;
        
        public Key(int playerID, int poolID) {
            this.playerID = playerID;
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
            if (this.playerID != other.playerID) {
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
            hash = 31 * hash + this.playerID;
            hash = 31 * hash + this.poolID;
            return hash;
        }
    }
    
    /** Creates a new instance of PlayerPool */
    public PlayerPool() {
        setID(new Key(0,0));
    }
    
    public PlayerPool(boolean locked) {
        this();
        setLocked(locked);
    }
   
    public int getPlayerID() {
        return getID().playerID;
    }

    public void setPlayerID(int playerID) {
        setID(new Key(playerID, getID().poolID));
    }

    public int getPoolID() {
        return getID().poolID;
    }

    public void setPoolID(int poolID) {
        setID(new Key(getID().playerID, poolID));
    }
    
    public int getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(int playerPosition) {
        this.playerPosition = playerPosition;
    }

    public int getPlayerPosition2() {
        return playerPosition2;
    }

    public void setPlayerPosition2(int playerPosition2) {
        this.playerPosition2 = playerPosition2;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isLocked() {
        return locked;
    }

    private void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static PlayerPool getLockedCopy(PlayerPool pp, Pool p) {
        //assert !pp.isLocked() : "player pool already locked";
        //assert p.getLockedStatus() == Pool.LockedStatus.PLAYERS_LOCKED : "must be attached to a locked pool";
        pp = ObjectCopier.copy(pp);
        pp.setPoolID(p.getID());
        pp.setLocked(true);
        
        return pp;
    }
}
