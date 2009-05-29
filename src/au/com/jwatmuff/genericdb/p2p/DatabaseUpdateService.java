/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

/**
 *
 * @author James
 */
public interface DatabaseUpdateService {
    UpdateSyncInfo sync(UpdateSyncInfo syncInfo);
}
