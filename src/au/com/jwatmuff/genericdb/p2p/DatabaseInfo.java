/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author James
 */
public class DatabaseInfo implements Serializable {
    public boolean local = false;
    public File localDirectory;
    public String name;
    public UUID id;
    public int passwordHash;
    public int peers;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DatabaseInfo other = (DatabaseInfo) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}
