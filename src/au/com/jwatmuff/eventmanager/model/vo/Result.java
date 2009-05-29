/*
 * Result.java
 *
 * Created on 28 August 2008, 03:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;

/**
 *
 * @author James
 */
public class Result extends DistributableObject<Integer> {
    private int fightID;
    private int[] playerScores = new int[] {0, 0};
    private int[] playerIDs = new int[] {0, 0};
    private String eventLog;

    /** Creates a new instance of Result */
    public Result() {
        setID(IDGenerator.generate());
    }

    public int getFightID() {
        return fightID;
    }
    
    public String getEventLog() {
        return eventLog;
    }

    public void setFightID(int fightID) {
        this.fightID = fightID;
    }

    public int[] getPlayerIDs() {
        return playerIDs;
    }

    public void setPlayerIDs(int[] playerIDs) {
        assert playerIDs != null;
        assert playerIDs.length == 2;

        this.playerIDs = playerIDs;
    }

    public int[] getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(int[] playerScores) {
        assert playerScores != null;
        assert playerScores.length == 2;

        this.playerScores = playerScores;
    }
    
    public void setEventLog(String eventLog) {
        this.eventLog = eventLog;
    }
}
