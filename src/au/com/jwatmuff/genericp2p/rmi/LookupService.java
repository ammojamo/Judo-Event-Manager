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
