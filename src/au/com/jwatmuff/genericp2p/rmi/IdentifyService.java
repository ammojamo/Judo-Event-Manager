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
