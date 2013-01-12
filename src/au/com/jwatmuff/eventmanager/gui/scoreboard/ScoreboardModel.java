/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    enum ScoringSystem {
        NEW, OLD
    }

    enum Score {
        IPPON(10), WAZARI(7), YUKO(5), SHIDO(3), DECISION(1);
        
        int points;

        Score(int points) {
            this.points = points;
        }
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
        TIMER, HOLDDOWN, MODE, SCORE, SHIDO, PENDING_SCORE, UNDO_AVAILABLE,
        GOLDEN_SCORE, SIREN, FIGHT_PENDING, ALL
    }
    
    enum Win {
        BY_IPPON(Score.IPPON, ""),
        WAZA_ARI_AWESETE_IPPON(Score.IPPON, "Waza-ari-awesete-ippon"),
        BY_WAZARI(Score.WAZARI, ""),
        BY_YUKO(Score.YUKO, ""),
        BY_SHIDO(Score.SHIDO, ""),
        BY_DECISION(Score.DECISION, "");
        
        public final Score score;
        public final String description;
        
        Win(Score score, String description) {
            this.score = score;
            this.description = description;
        }
    }
    
    interface ScoreboardModelListener {
        public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model);
    }

    void addListener(ScoreboardModelListener listener);
    
    void removeListener(ScoreboardModelListener listener);

    ScoringSystem getSystem();
    
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

    int getScore(int player, Score type);

    int getShido(int player);

    int getTime();

    Win getWin();

    int getWinPoints();

    int getWinningPlayer();

    boolean isHolddownActivated();

    boolean isHolddownPending();
    
    boolean undoCancelHolddownAvailable();

    int getPendingFightTime(int player);
    
    ScoringColors getColors();
    
    void reset(int fightTime, int goldenScoreTime, String[] playerNames);

    void reset(int fightTime, int goldenScoreTime, String[] playerNames, Date lastFights[], int minimumBreak);

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

    void changeShido(int player, boolean up);

    void endFight();

    void undoCancelHolddown();
    
    void shutdown();
    
    void setTimer(int seconds);
    
    void setHolddownTimer(int seconds);

    void decideWinner(int player);
    
    void setColors(ScoringColors colors);
}
