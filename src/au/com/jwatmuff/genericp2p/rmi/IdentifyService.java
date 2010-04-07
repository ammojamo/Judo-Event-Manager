/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.rmi;

import java.util.UUID;

/**
 *
 * @author James
 */
public interface IdentifyService {
    /**
     * This method is to be called remotely by a peer to determine the ID of the
     * local client. This ID is unique on an operating system user basis, using
     * a mechanism such as a user registry (under Windows) or a user
     * configuration file (under Linux) to store the ID.
     * 
     * @return  The ID of the local client
     */
    public UUID getUUID();

    /**
     * This method is to be called remotely by a peer to obtain a human readable
     * name identifying the local client. This name does not need to be unique.
     *
     * @return the name of the local client
     */
    public String getName();
}
