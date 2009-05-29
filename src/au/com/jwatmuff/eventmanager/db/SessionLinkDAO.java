/*
 * SessionLinkDAO.java
 *
 * Created on 22 August 2008, 03:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionLinkDAO extends GenericDAO<SessionLink> {
    public static final String FOR_SESSION = "forSession";
    public Collection<SessionLink> findForSession(int sessionID);
}
