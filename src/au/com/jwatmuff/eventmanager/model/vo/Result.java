/*
 * Result.java
 *
 * Created on 28 August 2008, 03:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.*;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
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


    public double[] getSimpleScores(Database database) {
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        return getSimpleScores(configurationFile);
    }

    public double[] getSimpleScores(ConfigurationFile configurationFile) {
        double[] simple = {0,0};
        
        for(int i = 0; i < 2; i++) {
            Score score = scores[i].getWinningScore(scores[1-i]);
            if(score == IPPON) {
                simple[i] = configurationFile.getDoubleProperty("defaultVictoryPointsIppon", 100);
            } else if(score == WAZARI) {
                simple[i] = configurationFile.getDoubleProperty("defaultVictoryPointsWazari", 10);
            } else if(score == SHIDO || score == DECISION) {
                simple[i] = configurationFile.getDoubleProperty("defaultVictoryPointsShido", 0.5);
            }
        }

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
