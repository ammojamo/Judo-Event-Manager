/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class FightGradingPoints {
    private static final Logger log = Logger.getLogger(FightGradingPoints.class);

    public ResultInfo result;
    public int points;
    public Player losingPlayer;
    public Player winningPlayer;
    public Grade effectiveLoserGrade;
    public Pool pool;

    private static final int[] POINTS = new int[] { 1, 3, 5, 7, 10, 15, 20 };

    public FightGradingPoints(ResultInfo ri, Database database) {
        result = ri;
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());
        Date censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
        int[] scores = ri.getResult().getSimpleScores(database);
        int winner = scores[0] > scores[1] ? 0 : 1;
        int loser = 1 - winner;
        
        winningPlayer = ri.getPlayer()[winner];
        losingPlayer = ri.getPlayer()[loser];
        
        pool = database.get(Pool.class, ri.getFight().getPoolID());
        effectiveLoserGrade = PoolChecker.getEffectiveGrade(losingPlayer, pool, censusDate, configurationFile);
        points = calculatePoints(ri, effectiveLoserGrade, database);
    }

    public int getPoints() {
        return points;
    }

    public Player getLosingPlayer() {
        return losingPlayer;
    }

    public Grade getEffectiveLoserGrade() {
        return effectiveLoserGrade;
    }

    public String getEffectiveLoserGradeDisplay() {
        return effectiveLoserGrade.shortGrade;
    }

    public ResultInfo getResult() {
        return result;
    }

    public Pool getPool() {
        return pool;
    }

    public int calculatePoints(ResultInfo info, Grade loserGrade, Database database) {
        if(loserGrade == null) return 0;

        int[] score = info.getResult().getSimpleScores(database);
        if(score[0] == score[1]) return 0;
        int w = (score[0] > score[1]) ? 0 : 1;
        int l = 1-w;

        if(info.getPlayer()[w] == null) {
            log.warn("This shouldn't happen");
            return 0;
        }

        Grade winnerGrade = info.getPlayer()[w].getGrade();

        int rankDifference = loserGrade.ordinal() - winnerGrade.ordinal();

        if(rankDifference < -2) return 0;
        rankDifference = Math.min(rankDifference, 2);

        if(score[w] == 10)
            return POINTS[4 + rankDifference];
        else if(score[w] == 7)
            return POINTS[3 + rankDifference];
        else
            return POINTS[2 + rankDifference];
    }
}
