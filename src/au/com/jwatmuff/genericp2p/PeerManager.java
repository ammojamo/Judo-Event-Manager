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

package au.com.jwatmuff.genericp2p;

import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author James
 */
public interface PeerManager {
    public void stop();
    public void addConnectionListener(PeerConnectionListener listener);
    public void removeConnectionListener(PeerConnectionListener listener);
    public UUID getUUID();
    public <T> void registerService(String serviceName, Class<T> serviceClass, T implementation);
    public <T> void registerService(Class<T> serviceClass, T implementation);
    public void unregisterService(String serviceName);
    public void refreshServices();
    public Collection<Peer> getPeers();
    public boolean isRegistered(String serviceName);
    public boolean initialisedOk();
}
