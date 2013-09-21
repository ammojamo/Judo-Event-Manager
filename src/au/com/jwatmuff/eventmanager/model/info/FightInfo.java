/*
 * FightInfo.java
 *
 * Created on 28 August 2008, 17:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author James
 */
public class FightInfo {
    private Result r;
    private Fight f;

    private FightInfo() {}
    
    /** Creates a new instance of FightInfo */
    public static FightInfo getFightInfo(Database database, Fight fight) {
        FightInfo info = new FightInfo();
        
        Collection<Result> rs = database.findAll(Result.class, ResultDAO.FOR_FIGHT, fight.getID());
        if(rs.size() > 0)
            info.r = rs.iterator().next();
        
        info.f = fight;
        
        return info;
    }
    
    public static List<FightInfo> getFightInfo(Database database, int poolID) {
        List<Fight> fights = new ArrayList<Fight>(database.findAll(Fight.class, FightDAO.FOR_POOL, poolID));
        List<FightInfo> fi = new ArrayList<FightInfo>();

        for(Fight fight : fights) {
            fi.add(getFightInfo(database, fight));
        }
        
        return fi;
    }

    public boolean resultKnown() {
        return (r != null);
    }

    public int getFightPostion() {
        return f.getPosition();
    }

    private int getWinningPlayerIndex() {
        return (r.getScores()[0].compareTo(r.getScores()[1]) > 0) ? 0 : 1;
    }

    private int getLosingPlayerIndex() {
        return 1 - getWinningPlayerIndex();
    }
    
    public String getWinningPlayerCode() {
        if(resultKnown())
            return f.getPlayerCodes()[getWinningPlayerIndex()];
        else
            return null;
    }

    public String getLosingPlayerCode() {
        if(resultKnown())
            return f.getPlayerCodes()[getLosingPlayerIndex()];
        else
            return null;
    }

    public double getWinningPlayerSimpleScore(ConfigurationFile configurationFile) {
        double[] simpleScores = r.getSimpleScores(configurationFile);
        if(resultKnown())
            return simpleScores[getWinningPlayerIndex()];
        else
            return 0;
    }

    public String[] getAllPlayerCode() {
        return f.getPlayerCodes();
    }

    public int[] getAllPlayerID() {
        return r.getPlayerIDs();
    }
    
    public int getWinningPlayerID() {
            return r.getPlayerIDs()[getWinningPlayerIndex()];
    }

    public int getLosingPlayerID() {
            return r.getPlayerIDs()[getLosingPlayerIndex()];
    }

    public int getFightTime() {
            return r.getDuration();
    }
}
