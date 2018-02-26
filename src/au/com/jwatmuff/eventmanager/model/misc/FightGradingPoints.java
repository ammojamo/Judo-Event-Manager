/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
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
    public Grade effectiveWinningGrade;
    public Pool pool;

    private static final int[] POINTS = new int[] { 1, 3, 5, 7, 10, 15, 20 };

    public FightGradingPoints(ResultInfo ri, Database database) {
        result = ri;
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());
        Date censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
        double[] scores = ri.getResult().getSimpleScores(database);
        int winner = scores[0] > scores[1] ? 0 : 1;
        int loser = 1 - winner;

        winningPlayer = ri.getPlayer()[winner];
        losingPlayer = ri.getPlayer()[loser];

        pool = database.get(Pool.class, ri.getFight().getPoolID());
        effectiveLoserGrade = losingPlayer.getGrade();
        effectiveWinningGrade = winningPlayer.getGrade();

        points = calculatePoints(ri, effectiveLoserGrade, effectiveWinningGrade, configurationFile);
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

    public static int calculatePoints(ResultInfo info, Grade loserGrade, Grade winningGrade, ConfigurationFile configurationFile) {
        // Source: JFA National Grading Policy, Version 1
        // https://assets.sportstg.com/assets/console/document/documents/BBA6FBD7-5056-BD36-A33F7B1F32E5DB9D.pdf
        // Accessed 16/2/2018
        Score winningScore =  info.getResult().getWinningScore();

        if(winningScore == null) {
            return 0;
        }

        int rankDifference = loserGrade.ordinal() - winningGrade.ordinal();
        rankDifference = Math.max(Math.min(rankDifference, 2), -2);

        switch(winningScore) {
            case IPPON:
                return POINTS[rankDifference + 4];
            case WAZARI:
                return POINTS[rankDifference + 3];
            case DECISION:
                return 0;
            default:
                return rankDifference + 3;
        }
    }
}
