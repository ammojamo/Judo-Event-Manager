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
