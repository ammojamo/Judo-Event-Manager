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

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionPoolDAO extends GenericDAO<SessionPool> {
    public static final String ALL = "all";
    public Collection<SessionPool> findAll();
    
    public static final String FOR_SESSION = "forSession";
    public Collection<SessionPool> findForSession(int sessionID);
    
    public static final String FOR_POOL = "forPool";
    public Collection<SessionPool> findForPool(int poolID);
}
