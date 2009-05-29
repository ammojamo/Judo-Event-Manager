/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p.rmi;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author James
 */
public interface LookupService {
    /**
     * This method is to be called remotely to determine what services are
     * offered by a particular peer.
     * 
     * @return A map of service names and corresponding service classes
     */
    Map<String, Class> getServices();
    
    /**
     * This method is to be called remotely to determine the names of services
     * of a given class.
     * 
     * @return A list of service names matching the given class
     */    
    Collection<String> getServices(Class serviceClass);
}
