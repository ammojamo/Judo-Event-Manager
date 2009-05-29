/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

/**
 *
 * @author James
 */
public interface PeerConnectionListener {
    public void handleConnectionEvent(PeerConnectionEvent event);
}
