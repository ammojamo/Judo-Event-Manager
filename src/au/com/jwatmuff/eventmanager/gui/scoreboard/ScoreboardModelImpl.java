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

import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.HANSAKUMAKE;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.IPPON;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.SHIDO;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors;
import au.com.jwatmuff.eventmanager.util.Stopwatch;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ScoreboardModelImpl implements ScoreboardModel, Serializable {
    private static final Logger log = Logger.getLogger(ScoreboardModel.class);

    /* constants */
    private static final int UNKNOWN_PLAYER = -1;
    private static final int HOLD_DOWN_UNDO_DELAY = 5000;

    /* queues for blocking event notification */
    private Map<Object, LinkedBlockingQueue<ScoreboardUpdate>> queues =  new HashMap<Object, LinkedBlockingQueue<ScoreboardUpdate>>();

    /* main stuff */
    private Stopwatch mainTimer;
    private Mode mode = Mode.NO_FIGHT;
    private int score[][] = new int[2][Score.values().length];
    private int scoreAtStartOfGoldenScore[][] = new int[2][Score.values().length];
    private int fightTime;
    private int goldenScoreTime;
    private String[] playerNames = new String[2];
    private String[] teamNames = new String[2];
    private String divisionName;

    /* hold down related stuff */
    private Stopwatch holddownTimer;
    private HolddownMode holddownMode = HolddownMode.INACTIVE;
    private Score pendingHolddownScore;                         // stores score from holddown which has not yet been assigned to a player
    private int holddownPlayer = UNKNOWN_PLAYER;
    private List<Score>[] pendingScores;                        // stores scores from holddowns which have been assigned to a player but not yet awarded
    private int holddownTimeIpon;
    private int holddownTimeWazari;

    /* cancel holddown undo stuff */
    private Stopwatch shadowTimer = new Stopwatch(10, true);
    private Timer undoDisableTimer;                             // timer for disabling the undo facility after a few seconds have elapsed
    private int undoPendingScorePlayer;                         // if an cancel hold down is done, tells us which player has a pending score which must be removed

    /* winning player stuff */
    private int winningPlayer = UNKNOWN_PLAYER;
    private Win win = null;

    /* golden rule */
    private GoldenScoreMode goldenScoreMode = GoldenScoreMode.INACTIVE;

    /* pending fight timers */
    private Stopwatch[] pendingFightTimers = new Stopwatch[2];

    private ScoringColors colors = new ScoringColors();
    private boolean showTeams = true;

    private StringBuilder eventLog = new StringBuilder();

    private Collection<ScoreboardModelListener> listeners = new ArrayList<ScoreboardModelListener>();

    @SuppressWarnings("unchecked")
    public ScoreboardModelImpl() {
        addListener(new ScoreboardModelListener() {
            @Override
            public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
                for(BlockingQueue queue : queues.values()) {
                    try { queue.put(update); } catch(InterruptedException e) {}
                }
            }
        });

        pendingScores =  (List<Score>[])new List[] {
            new LinkedList<Score>(), new LinkedList<Score>()
        };

        mainTimer = new Stopwatch(10, false, new Runnable() {
            @Override
            public void run() {
                if(mainTimer.getTime() < 0 || (!mainTimer.getDirection() && mainTimer.getTime() == 0)) {
                    mainTimer.stop();
                    switch(goldenScoreMode) {
                        case INACTIVE:
                            setGoldenScoreMode(GoldenScoreMode.READY);
                            logEvent("Stop timer");
                            checkForWin();
                            break;
                        case ACTIVE:
                            setGoldenScoreMode(GoldenScoreMode.FINISHED);
                            break;
                    }

                    if(holddownMode != HolddownMode.ACTIVE)
                        notifyListeners(ScoreboardUpdate.SIREN);

                    if(win == null)
                        setMode(Mode.IDLE);
                }
                notifyListeners(ScoreboardUpdate.TIMER);
            }
        }, 1000);

        holddownTimer = new Stopwatch(10, true, new Runnable() {

            @Override
            public void run() {
                if(holddownTimer.getTime()/1000 >= holddownTimeIpon) {
                    stopTimer();
                    holddownTimer.stop();
                    handleHolddownTimerEnd();
                } else if(holddownTimer.getTime()/1000 >= holddownTimeWazari &&
                          holddownPlayer != UNKNOWN_PLAYER &&
                          getScore(holddownPlayer, Score.WAZARI) == 1) {
                    stopTimer();
                    holddownTimer.stop();
                    setHolddownMode(HolddownMode.INACTIVE);
                    notifyListeners(ScoreboardUpdate.SIREN);
                    pendingScores[holddownPlayer].add(Score.WAZARI);
                    notifyListeners(ScoreboardUpdate.PENDING_SCORE);
                }
                notifyListeners(ScoreboardUpdate.HOLDDOWN);
            }
        }, 1000);
    }

    @Override
    public void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, String divisionName) {
        this.fightTime = fightTime;
        holddownTimeIpon = 20;
        holddownTimeWazari = 10;
        this.goldenScoreTime = goldenScoreTime;
        this.playerNames = playerNames;
        this.teamNames = teamNames;
        this.divisionName = divisionName;
        shutdownPendingTimers();
        stopTimer();
        cancelHolddownTimer();
        mainTimer.reset(fightTime * 1000);
        mainTimer.direction(false);
        holddownTimer.reset(0);
        for(int i=0; i<2; i++) {
            for(int j=0; j<Score.values().length; j++)
                score[i][j] = 0;
        }
        goldenScoreMode = GoldenScoreMode.INACTIVE;
        holddownMode = HolddownMode.INACTIVE;
        mode = Mode.IDLE;
        eventLog = new StringBuilder();
        notifyListeners(ScoreboardUpdate.ALL);
        setMode(Mode.FIGHT_PENDING);
    }

    @Override
    public void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, Date lastFights[], int minimumBreak, String divisionName) {
        reset(fightTime, goldenScoreTime, playerNames, teamNames, divisionName);
        for(int i=0; i<2; i++) {
            final int j = i;
            if(lastFights[i] == null) continue;
            int secsSinceLast = (int) ( ((new Timestamp().getTime()) - lastFights[i].getTime()) / 1000);
            log.debug("secsSinceLast: " + secsSinceLast);
            if(secsSinceLast < minimumBreak) {
                pendingFightTimers[i] = new Stopwatch(10, false, new Runnable() {
                    @Override
                    public void run() {
                        notifyListeners(ScoreboardUpdate.FIGHT_PENDING);
                        if(!(pendingFightTimers[j] == null) && getPendingFightTime(j) <= 0) {
                            pendingFightTimers[j].stop();
                            pendingFightTimers[j] = null;
                        }

                    }
                }, 1000);
                pendingFightTimers[i].reset((minimumBreak - secsSinceLast) * 1000);
                pendingFightTimers[i].start();
            }
        }
    }

    @Override
    public void declarePlayerReady(int player) {
        Stopwatch s = pendingFightTimers[player];
        if(s != null) {
            s.stop();
            pendingFightTimers[player] = null;
            notifyListeners(ScoreboardUpdate.FIGHT_PENDING);
        }
    }

    public void declareFightReady() {
        assert(getMode() == Mode.FIGHT_PENDING);
        assert(getPendingFightTime(0) <= 0);
        assert(getPendingFightTime(1) <= 0);
        setMode(Mode.IDLE);
    }

    @Override
    public void addListener(ScoreboardModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ScoreboardModelListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ScoreboardUpdate update) {
        for(ScoreboardModelListener listener : listeners)
            listener.handleScoreboardUpdate(update, this);
    }

    private void setMode(Mode mode) {
        if(this.mode != mode) {
            this.mode = mode;
            notifyListeners(ScoreboardUpdate.MODE);
        }
    }

    private void setHolddownMode(HolddownMode mode) {
        this.holddownMode = mode;
        notifyListeners(ScoreboardUpdate.MODE);
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void approveGoldenScore() {
        /* must be ready to start golden rule */
        if(goldenScoreMode != GoldenScoreMode.READY)
            return;

        /* can't start golden rule time if a holddown is still active */
        if(holddownMode == HolddownMode.ACTIVE)
            return;

        notifyListeners(ScoreboardUpdate.SCORE);

        if (goldenScoreTime == 0) {
            mainTimer.direction(true);
            mainTimer.reset(0);
        } else {
            mainTimer.direction(false);
            mainTimer.reset(goldenScoreTime * 1000);
        }

        for(int i = 0; i < score.length; i++) {
            for(int j = 0; j < score[i].length; j++) {
                scoreAtStartOfGoldenScore[i][j] = score[i][j];
            }
        }
        notifyListeners(ScoreboardUpdate.TIMER);
        setGoldenScoreMode(GoldenScoreMode.ACTIVE);

        logEvent("Start Golden Score");
    }

    @Override
    public void startTimer() {
        /* ignore if one player has already won */
        if(mode == Mode.WIN) {
            return;
        }

        /* ignore if fight is not ready */
        if(mode == Mode.FIGHT_PENDING) {
            return;
        }

        /* ignore if no fight to start */
        if(mode == Mode.NO_FIGHT) {
            return;
        }

        /* ignore if the timer has run out and golden rule is active */
        if(mainTimer.getTime() < 0 || (!mainTimer.getDirection() && mainTimer.getTime() == 0)) {
            return;
        }

        /* ignore if an ippon from a holddown is pending assignment to a player */
        if(holddownMode == HolddownMode.PENDING && pendingHolddownScore == Score.IPPON) {
            return;
        }

        disableCancelHolddownUndo();
        mainTimer.start();
        if(holddownMode == HolddownMode.ACTIVE) {
            holddownTimer.start();
        }
        setMode(Mode.FIGHTING);

        logEvent("Start timer");
    }

    @Override
    public void stopTimer() {
        disableCancelHolddownUndo();
        mainTimer.stop();
        if(holddownMode == HolddownMode.ACTIVE)
            holddownTimer.stop();
        if(mode == Mode.FIGHTING)
            setMode(Mode.IDLE);

        logEvent("Stop timer");
    }

    @Override
    public void toggleTimer() {
        if(mainTimer.isRunning())
            stopTimer();
        else
            startTimer();
    }

    @Override
    public int getTime() {
        return mainTimer.getSeconds();
    }

    @Override
    public void startHolddownTimer() {
        if(mode != Mode.FIGHTING) return;
        if(holddownMode != HolddownMode.INACTIVE) return;
        disableCancelHolddownUndo();
        holddownPlayer = UNKNOWN_PLAYER;
        holddownTimer.reset(0);
        holddownTimer.start();
        setHolddownMode(HolddownMode.ACTIVE);

        logEvent("Start holddown");
    }

    /*
     * Called when the hold down timer reaches Ippon
     */
    private void handleHolddownTimerEnd() {
        notifyListeners(ScoreboardUpdate.SIREN);
        if(holddownPlayer != UNKNOWN_PLAYER) {
            setHolddownMode(HolddownMode.INACTIVE);
            pendingScores[holddownPlayer].add(Score.IPPON);
            notifyListeners(ScoreboardUpdate.PENDING_SCORE);
        }
        else {
            pendingHolddownScore = Score.IPPON;
            setHolddownMode(HolddownMode.PENDING);
        }
    }

    @Override
    public void cancelHolddownTimer() {
        /* ignore if hold down timer is not active */
        if(holddownMode != HolddownMode.ACTIVE) return;

        holddownTimer.stop();

        int holddownTime = holddownTimer.getSeconds();

        if(mainTimer.isRunning())
            enableCancelHolddownUndo();

        if(mainTimer.getTime() <= 0)
            notifyListeners(ScoreboardUpdate.SIREN);

        /* calculate score due to hold down */
        Score holddownScore = holddownScore(holddownTime);

        if(holddownScore == null) {
            /*
             * holddown was not long enough to result in score
             */
            setHolddownMode(HolddownMode.INACTIVE);

        } else if(holddownScore == Score.IPPON) {
             /*
              * this should already have been picked up by the holddown timer
              * handler, but in rare cases this may be possible
              */
            handleHolddownTimerEnd();

        } else if(holddownPlayer == UNKNOWN_PLAYER) {
            /*
             * holddown resulted in score but the player has not been
             * assigned
             */
            pendingHolddownScore = holddownScore;
            setHolddownMode(HolddownMode.PENDING);

        } else {
            /*
             * holddown score is added to list of pending scores for the
             * appropriate player
             */
            pendingScores[holddownPlayer].add(holddownScore);
            notifyListeners(ScoreboardUpdate.PENDING_SCORE);
            setHolddownMode(HolddownMode.INACTIVE);
            undoPendingScorePlayer = holddownPlayer; // in case we have to roll back in case of an undo
        }

        logEvent("End holddown (" + holddownTime + "s)");
    }

    private void enableCancelHolddownUndo() {
        shadowTimer.reset(holddownTimer.getTime());
        shadowTimer.start();
        undoDisableTimer = new Timer();
        undoDisableTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                disableCancelHolddownUndo();
            }
        }, HOLD_DOWN_UNDO_DELAY);
        notifyListeners(ScoreboardUpdate.UNDO_AVAILABLE);
    }

    private void disableCancelHolddownUndo() {
        shadowTimer.stop();
        if(undoDisableTimer != null) {
            undoDisableTimer.cancel();
            undoDisableTimer = null;
            undoPendingScorePlayer = UNKNOWN_PLAYER;
            notifyListeners(ScoreboardUpdate.UNDO_AVAILABLE);
        }
    }

    @Override
    public void undoCancelHolddown() {
        if(undoCancelHolddownAvailable()) {
            // roll back pending score if necessary
            if(undoPendingScorePlayer != UNKNOWN_PLAYER) {
                List<Score> pend = pendingScores[undoPendingScorePlayer];
                if(pend.size() > 0) pend.remove(pend.size()-1);
                notifyListeners(ScoreboardUpdate.PENDING_SCORE);
            }
            long sTime = shadowTimer.getTime();
            log.debug(sTime);
            if(sTime < 25 * 1000) {
                holddownTimer.reset(sTime);
                holddownTimer.start();
                setHolddownMode(HolddownMode.ACTIVE);
            } else {
                /* TODO: adjust main timer if necessary? */
                handleHolddownTimerEnd();
            }
            disableCancelHolddownUndo();
        }

        logEvent("Undo holddown end");
    }

    @Override
    public boolean undoCancelHolddownAvailable() {
        return shadowTimer.isRunning();
    }

    @Override
    public int getHolddownTime() {
        return holddownTimer.getSeconds();
    }

    @Override
    public Score getPendingHolddownScore() {
        return pendingHolddownScore;
    }

    @Override
    public boolean isHolddownActivated() {
        return holddownMode != HolddownMode.INACTIVE;
    }

    @Override
    public boolean isHolddownPending() {
        return holddownMode == HolddownMode.PENDING;
    }

    @Override
    public int getHolddownPlayer() {
        return holddownPlayer;
    }

    @Override
    public void setHolddownPlayer(int player) {
        switch(holddownMode) {
            case PENDING:
                holddownPlayer = UNKNOWN_PLAYER;
                setHolddownMode(HolddownMode.INACTIVE);
                Score holddownScore = pendingHolddownScore;
                pendingScores[player].add(holddownScore);
                notifyListeners(ScoreboardUpdate.PENDING_SCORE);
                break;
            case ACTIVE:
                holddownPlayer = player;
                notifyListeners(ScoreboardUpdate.MODE);
                break;
            case INACTIVE:
                //do nothing
        }
        logEvent("Set Holddown Player: " + playerNames[player]);
    }

    private Score holddownScore(int time) {
        if(time == holddownTimeIpon) {
            return Score.IPPON;
        } else if (time >= holddownTimeWazari) {
            return Score.WAZARI;
        } else {
            return null;
        }
    }

    @Override
        public void changeScore(int player, Score type, boolean up) {
        disableCancelHolddownUndo();
        int olds = score[player][type.ordinal()];

        int s = olds + (up?1:-1);
        score[player][type.ordinal()] = s;

        boolean scoreok = true;

        if(s < 0) scoreok = false;
        else switch(type) {
            case IPPON:
                if(getScore(player,type) > 1) scoreok = false;
                break;
            case WAZARI:
                if(getScore(player, type) > 2) scoreok = false;
                break;
            case SHIDO:
            case HANSAKUMAKE:
                if(getScore(player, SHIDO) > 3 ||
                    getScore(player, HANSAKUMAKE) > 1) {
                    scoreok = false;
                }
                break;
        }

        if(!scoreok) {
            score[player][type.ordinal()] = olds;
            return;
        }

        logEvent(playerNames[player] + " " + (up?"+":"-") + type.toString().substring(0,1));
        checkForWin();
        notifyListeners(ScoreboardUpdate.SCORE);
    }

    @Override
    public int getScore(int player, Score type) {
        int s = score[player][type.ordinal()];

        if(type == Score.IPPON && isHansakumake(1 - player)) {
            s++;
        }

        return s;
    }

    @Override
    public boolean isHansakumake(int player) {
        // 2 Ways to get Hansakumake:
        // - direct hansakumake
        // - 3 shidos of any kind
        return shidoCount(player) == 3 ||
            getScore(player, Score.HANSAKUMAKE) == 1;
    }

    @Override
    public List<Score> getPendingScores(int player) {
        return Collections.unmodifiableList(pendingScores[player]);
    }

    @Override
    public void awardPendingScore(int player, int index) {
        disableCancelHolddownUndo();
        Score s = pendingScores[player].remove(index);
        changeScore(player, s, true);
        notifyListeners(ScoreboardUpdate.PENDING_SCORE);
    }

    private void checkForWin() {
        win = null;
        winningPlayer = UNKNOWN_PLAYER;

        if(holddownMode == HolddownMode.ACTIVE) {
            return;
        }

        for(int i=0; i<2; i++) {
            if(getScore(i, Score.IPPON) == 1) {
                if((getScore(i, Score.WAZARI) == 2) ||
                   (getScore(1-i, Score.IPPON) == 1) ||
                   (getScore(1-i, Score.WAZARI) == 2) ||
                   (!pendingScores[i].isEmpty()) ||
                   (!pendingScores[1-i].isEmpty())) {
                    // Stop timer until ippon / pending scores are sorted out
                    stopTimer();
                    setMode(Mode.IDLE);
                } else {
                    winningPlayer = i;
                    win = Win.BY_IPPON;
                    stopTimer();
                    setMode(Mode.WIN);
                }
                return;
            }
            if(getScore(i, Score.WAZARI) == 2) {
                if((getScore(1-i, Score.IPPON) == 1) ||
                   (getScore(1-i, Score.WAZARI) == 2) ||
                   (!pendingScores[i].isEmpty()) ||
                   (!pendingScores[1-i].isEmpty())) {
                    // Stop timer until ippon / pending scores are sorted out
                    stopTimer();
                    setMode(Mode.IDLE);
                } else {
                    winningPlayer = i;
                    win = Win.BY_IPPON;
                    stopTimer();
                    setMode(Mode.WIN);
                }
                return;
            }
        }

        boolean goldenScoreStarted = goldenScoreMode == GoldenScoreMode.ACTIVE || goldenScoreMode == GoldenScoreMode.FINISHED;
        boolean mainTimeFinished = goldenScoreStarted || goldenScoreMode == GoldenScoreMode.READY;

        if(mainTimeFinished) {
            for(int player = 0; player < 2; player++) {
                if(getScore(player, Score.WAZARI) > getScore(1-player, Score.WAZARI)) {
                    win = Win.BY_WAZARI;
                    winningPlayer = player;
                    break;
                }
            }

            if(win != null) {
                stopTimer();
                setMode(Mode.WIN);
                return;
            }
        }

        if(mode == Mode.WIN) {
            winningPlayer = UNKNOWN_PLAYER;
            win = null;
            setMode(Mode.IDLE);
        }
    }

    private int shidoCount(int player) {
        return score[player][Score.SHIDO.ordinal()];
    }

    private int shidoCountAtStartOfGoldenScore(int player) {
        return scoreAtStartOfGoldenScore[player][Score.SHIDO.ordinal()];
    }

    @Override
    public int getWinningPlayer() {
        return winningPlayer;
    }

    @Override
    public Win getWin() {
        return win;
    }

    @Override
    public void setGoldenScoreMode(GoldenScoreMode goldenScoreMode) {
        if(this.goldenScoreMode != goldenScoreMode) {
            this.goldenScoreMode = goldenScoreMode;
            if(this.goldenScoreMode == GoldenScoreMode.INACTIVE) {
                mainTimer.direction(false);
            }else{
                if (goldenScoreTime == 0) {
                    mainTimer.direction(true);
                } else {
                    mainTimer.direction(false);
                }
            }
            notifyListeners(ScoreboardUpdate.GOLDEN_SCORE);
        }
    }

    @Override
    public GoldenScoreMode getGoldenScoreMode() {
        return goldenScoreMode;
    }

    @Override
    public String getPlayerName(int player) {
        return playerNames[player];
    }

    @Override
    public String getTeamName(int player) {
        return teamNames == null ? null : teamNames[player];
    }

    @Override
    public String getDivisionName() {
        return divisionName;
    }

    @Override
    public void endFight() {
        //reset(0, new String[] {"",""});
        logEvent("End fight");
        setMode(Mode.NO_FIGHT);
    }

    private void logEvent(String eventString) {
        eventLog.append(getTimeString() + ":" + eventString + "\n");
    }

    private static DecimalFormat format = new DecimalFormat("00");

    private String getTimeString() {
        int time = mainTimer.getSeconds();
        int min = time / 60;
        int sec = time % 60;
        return format.format(min) + ":" + format.format(sec) + ((goldenScoreMode == GoldenScoreMode.ACTIVE)?"[GS]":"    ");
    }

    @Override
    public String getEventLog() {
        return eventLog.toString();
    }

    @Override
    public ScoreboardUpdate getUpdateEvent(Object id, int timeout) {
        if(!queues.containsKey(id))
            queues.put(id, new LinkedBlockingQueue<ScoreboardUpdate>());
        try {
            return queues.get(id).poll(timeout, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {
            return null;
        }
    }

    @Override
    public void shutdown() {
        mainTimer.stop();
        holddownTimer.stop();
        if(undoDisableTimer != null)
            undoDisableTimer.cancel();
        if(shadowTimer != null)
            shadowTimer.stop();
        shutdownPendingTimers();
    }

    private void shutdownPendingTimers() {
        for(int i = 0; i < 2; i++) {
            if(pendingFightTimers[i] != null) {
                pendingFightTimers[i].stop();
                pendingFightTimers[i] = null;
            }
        }
    }

    @Override
    public void setTimer(int seconds) {
        logEvent("Set time (" + seconds + "s)");
        if(mainTimer.isRunning()) {
            stopTimer();
        }
        mainTimer.reset(seconds * 1000);
        notifyListeners(ScoreboardUpdate.TIMER);
    }

    @Override
    public void setHolddownTimer(int seconds) {
        int oldTime = holddownTimer.getSeconds();
        logEvent("Set holddown (" + seconds + "s, was " + oldTime + "s)");
        switch(holddownMode) {
            case ACTIVE:
                stopTimer();
                holddownTimer.reset(seconds * 1000);
                disableCancelHolddownUndo();
                break;
            case INACTIVE:
                startTimer();
                startHolddownTimer();
                stopTimer();
                holddownTimer.reset(seconds * 1000);
                disableCancelHolddownUndo();
                break;
            case PENDING:
                setHolddownMode(HolddownMode.INACTIVE);
                startTimer();
                startHolddownTimer();
                stopTimer();
                holddownTimer.reset(seconds * 1000);
                disableCancelHolddownUndo();
                break;
        }
        notifyListeners(ScoreboardUpdate.HOLDDOWN);
    }

    @Override
    public void decideWinner(int player) {
        assert(goldenScoreMode.equals(GoldenScoreMode.FINISHED));
        assert(win == null);
        assert(player == 0 || player == 1);

        score[player][Score.DECISION.ordinal()] = 1;
        score[1 - player][Score.DECISION.ordinal()] = 0;

        win = Win.BY_DECISION;
        winningPlayer = player;

        logEvent("Decision: " + playerNames[player]);

        endFight();
    }

    @Override
    public int getPendingFightTime(int player) {
        if(pendingFightTimers != null &&
           pendingFightTimers[player] != null) {
            return pendingFightTimers[player].getSeconds();
        }
        return 0;
    }

    @Override
    public ScoringColors getColors() {
        return colors;
    }

    @Override
    public void setColors(ScoringColors colors) {
        this.colors = colors;
        notifyListeners(ScoreboardUpdate.ALL);
    }

    @Override
    public boolean showTeams() {
        return showTeams;
    }

    @Override
    public void setShowTeams(boolean show) {
        this.showTeams = show;
        notifyListeners(ScoreboardUpdate.ALL);
    }
}
