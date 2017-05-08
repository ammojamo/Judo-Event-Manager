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

package au.com.jwatmuff.eventmanager.gui.scoreboard;

import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors;
import java.util.Date;
import java.util.List;

/**
 *
 * @author James
 */
public interface ScoreboardModel {
    enum Score {
        IPPON, WAZARI, SHIDO, LEG_SHIDO, HANSAKUMAKE, DECISION;
        
        public char initial = name().charAt(0);
    }
    
    enum Mode {
        IDLE, FIGHTING, WIN, NO_FIGHT, FIGHT_PENDING
    }
    
    enum HolddownMode {
        INACTIVE, ACTIVE, PENDING
    }
    
    enum GoldenScoreMode {
        INACTIVE, READY, ACTIVE, FINISHED
    }
    
    enum ScoreboardUpdate {
        TIMER, HOLDDOWN, MODE, SCORE, PENDING_SCORE, UNDO_AVAILABLE,
        GOLDEN_SCORE, SIREN, FIGHT_PENDING, ALL
    }

    enum Win {
        BY_IPPON(Score.IPPON),
        BY_WAZARI(Score.WAZARI),
        BY_SHIDO(Score.SHIDO),
        BY_DECISION(Score.DECISION);
        
        public final Score score;

        Win(Score score) {
            this.score = score;
        }
    }
    
    interface ScoreboardModelListener {
        public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model);
    }

    void addListener(ScoreboardModelListener listener);
    
    void removeListener(ScoreboardModelListener listener);

    ScoreboardUpdate getUpdateEvent(Object id, int timeout);

    String getEventLog();

    GoldenScoreMode getGoldenScoreMode();

    void setGoldenScoreMode(GoldenScoreMode mode);

    int getHolddownPlayer();

    int getHolddownTime();

    Mode getMode();

    Score getPendingHolddownScore();

    List<Score> getPendingScores(int player);

    String getPlayerName(int player);
    
    String getTeamName(int player);
    
    String getDivisionName();

    int getScore(int player, Score type);
    
    boolean isHansakumake(int player);

    int getTime();

    Win getWin();

    int getWinningPlayer();

    boolean isHolddownActivated();

    boolean isHolddownPending();
    
    boolean undoCancelHolddownAvailable();

    int getPendingFightTime(int player);
    
    ScoringColors getColors();
    
    boolean showTeams();
    
    void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, String divisionName);

    void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, Date lastFights[], int minimumBreak, String divisionName);

    void declarePlayerReady(int player);

    void declareFightReady();

    void setHolddownPlayer(int player);

    void approveGoldenScore();

    void startHolddownTimer();

    void startTimer();

    void stopTimer();

    void toggleTimer();
    
    void awardPendingScore(int player, int index);

    void cancelHolddownTimer();

    void changeScore(int player, Score type, boolean up);

    void endFight();

    void undoCancelHolddown();
    
    void shutdown();
    
    void setTimer(int seconds);
    
    void setHolddownTimer(int seconds);

    void decideWinner(int player);
    
    void setColors(ScoringColors colors);
    
    void setShowTeams(boolean show);
}
