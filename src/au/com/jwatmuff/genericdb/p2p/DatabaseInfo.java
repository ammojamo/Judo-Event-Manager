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
}
