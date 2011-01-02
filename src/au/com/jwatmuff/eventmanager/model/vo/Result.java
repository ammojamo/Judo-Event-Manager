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
//    I don't know if the specification for the finalScore is the best for this situation, maybe change it to a more appropreate structure.
/* Leonard
    private int[] finalScores = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int fightTime;
    private int[] playerScores = new int[] {0, 0};
 *
 */

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
/* Leonard
    public void setFinalScores(int[] finalScores) {
        assert finalScores != null;
        assert finalScores.length == 10;

        this.finalScores = finalScores;
    }

    public int[] getFinalScores() {
        return finalScores;
    }

    public void setFightTime(int fightTime) {
        this.fightTime = fightTime;
    }

    public int getFightTime() {
        return fightTime;
    }
*/
    public void setEventLog(String eventLog) {
        this.eventLog = eventLog;
    }
    
    public String getEventLog() {
        return eventLog;
    }

        //        This needs to be removed.
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

    /* Leonard
    public int[] getPlayerScores() {
//        This needs to be calculated using a points calculator. The points calculator should be a utility of some sort.
        return playerScores;
    }
*/
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
