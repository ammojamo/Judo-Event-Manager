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

import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionDAO extends GenericDAO<Session> {
    public static final String ALL = "all";
    public Collection<Session> findAll();
    
    public static final String FOLLOWING = "following";
    public Collection<Session> findFollowing(int sessionID, SessionLink.LinkType linkType);
    
    public static final String PRECEDING = "preceding";
    public Collection<Session> findPreceding(int sessionID, SessionLink.LinkType linkType);
    
    public static String ALL_MATS = "allMats";
    public Collection<Session> findAllMats();

    public static String ALL_NORMAL = "allNormal";
    public Collection<Session> findAllNormal();

    public static String WITH_LOCKED_STATUS = "withLockedStatus";
    public Collection<Session> findWithLockedStatus(Session.LockedStatus lockedStatus);

    public static String FOR_FIGHT = "forFight";
    public Session findForFight(int fightID);

    public static String FOR_POOL = "forPool";
    public Collection<Session> findForPool(int poolID);
}
