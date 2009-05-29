/*
 * SessionFightInfo.java
 *
 * Created on 20 August 2008, 15:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SessionFightInfo {
    private static final Logger log = Logger.getLogger(SessionFightInfo.class);
    private SessionFight sf;
    private Session session;
    private Fight fight;
    
    /** Creates a new instance of SessionFightInfo */
    public SessionFightInfo(Database database, SessionFight sf) {
        this.sf = sf;
        session = database.get(Session.class, sf.getSessionID());
        fight = database.get(Fight.class, sf.getFightID());
    }
    
    public SessionFightInfo(SessionFight sf, Session session, Fight fight) {
        this.sf = sf;
        this.session = session;
        this.fight = fight;
    }
    
    public static Collection<SessionFightInfo> getForSession(Database database, Session session) {
        assert session != null;

        Collection<SessionFightInfo> sfis = new ArrayList<SessionFightInfo>();
        Collection<SessionFight> sfs = database.findAll(SessionFight.class, SessionFightDAO.FOR_SESSION, session.getID());
        for(SessionFight sf : sfs) {
            Fight fight = database.get(Fight.class, sf.getFightID());
            //log.debug("Creating SessionFightInfo(" + sf + ", " + session + ", " + fight + ")");
            sfis.add(new SessionFightInfo(sf, session, fight));
        }
        
        return sfis;
    }

    public SessionFight getSessionFight() {
        return sf;
    }

    public Session getSession() {
        return session;
    }

    public Fight getFight() {
        return fight;
    }
    
}
