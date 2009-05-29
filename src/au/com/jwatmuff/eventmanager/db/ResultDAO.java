/*
 * ResultDAO.java
 *
 * Created on 28 August 2008, 03:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db;

import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.GenericDAO;
import java.util.Collection;

/**
 *
 * @author James
 */
public interface ResultDAO extends GenericDAO<Result> {
    public static final String FOR_SESSION = "forSession";
    public Collection<Result> findForSession(int sessionID);
    
    public static final String FOR_FIGHT = "forFight";
    public Collection<Result> findForFight(int fightID);

    public static final String FOR_PLAYER = "forPlayer";
    public Collection<Result> findForPlayer(int playerID);
    
    public static final String ALL = "all";
    public Collection<Result> findAll();
}
