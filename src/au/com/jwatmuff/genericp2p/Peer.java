/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

import java.util.UUID;

/**
 *
 * @author James
 */
public interface Peer {
    public <T> T getService(String name, Class<T> serviceType) throws NoSuchServiceException;
    public <T> T getService(Class<T> serviceType) throws NoSuchServiceException;
    public String getName();
    public UUID getUUID();
}
