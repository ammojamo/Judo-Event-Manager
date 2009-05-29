/*
 * SessionFightDAO.java
 *
 * Created on 20 August 2008, 15:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface SessionFightDAO extends GenericDAO<SessionFight> {
    public static final String FOR_SESSION = "forSession";
    public Collection<SessionFight> findForSession(int sessionID);
    
    public static final String FOR_FIGHT = "forFight";
    public SessionFight findForFight(int fightID);
}
