/*
 * Result.java
 *
 * Created on 28 August 2008, 03:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;

/**
 *
 * @author James
 */
public class Result extends DistributableObject<Integer> {
    private int fightID;
    private int[] playerIDs = new int[] {0, 0};

    private FullScore[] scores = new FullScore[] { new FullScore(), new FullScore() };

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

    public FullScore[] getScores() {
        return scores;
    }

    public void setScores(FullScore[] scores) {
        assert scores != null;
        assert scores.length == 2;

        this.scores = scores;
    }


    public int[] getSimpleScores(Database database) {
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        return getSimpleScores(configurationFile);
    }

    public int[] getSimpleScores(ConfigurationFile configurationFile) {
        int[] simple = {0,0};
        int simpleScore = 0;

        if (scores[0].getIppon() != scores[1].getIppon())
            simpleScore  = configurationFile.getIntegerProperty("defaultVictoryPointsIppon", 10);
        else if(scores[0].getWazari() != scores[1].getWazari())
            simpleScore  = configurationFile.getIntegerProperty("defaultVictoryPointsWazari", 7);
        else if (scores[0].getYuko() != scores[1].getYuko())
            simpleScore  = configurationFile.getIntegerProperty("defaultVictoryPointsYuko", 5);
        else if (scores[0].getDecision() != scores[1].getDecision())
            simpleScore  = configurationFile.getIntegerProperty("defaultVictoryPointsDecision", 1);
        if (scores[0].compareTo(scores[1])>0)
            simple[0] = simpleScore;
        else
            simple[1] = simpleScore;

        return simple;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDurationString() {
        return String.format("%02d:%02d", duration/60, duration%60);
    }
}
