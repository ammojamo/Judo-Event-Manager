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
    private int[] playerIDs = new int[] {0, 0};

    private int[] simpleScores = new int[] {0, 0};
    // full score, e.g. "I:0,W:1,Y:4,S:0,D:0"
    private String[] scores = new String[] {"", ""};
    // duration of fight in seconds
    private int duration;

    private String eventLog;

    /** Creates a new instance of Result */
    public Result() {
        setID(IDGenerator.generate());
    }

    public void setFightID(int fightID) {
        this.fightID = fightID;
    }

    public int getFightID() {
        return fightID;
    }

    public void setPlayerIDs(int[] playerIDs) {
        assert playerIDs != null;
        assert playerIDs.length == 2;

        this.playerIDs = playerIDs;
    }

    public int[] getPlayerIDs() {
        return playerIDs;
    }

    public void setEventLog(String eventLog) {
        this.eventLog = eventLog;
    }
    
    public String getEventLog() {
        return eventLog;
    }

    public int[] getSimpleScores() {
        return simpleScores;
    }

    public void setSimpleScores(int[] playerScores) {
        assert playerScores != null;
        assert playerScores.length == 2;

        this.simpleScores = playerScores;
    }

    public String[] getScores() {
        assert scores != null;
        assert scores.length == 2;

        return scores;
    }

    public void setScores(String[] scores) {
        this.scores = scores;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
