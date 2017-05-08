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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ScoreboardModelWrapper implements ScoreboardModel {
    public static final Logger log = Logger.getLogger(ScoreboardModelWrapper.class);
    
    private ScoreboardModel model;
    private final Collection<ScoreboardModelListener> listeners = new ArrayList<ScoreboardModelListener>();
    
    private Boolean running = true;

    /* for notifying a listener when this class stops running, for example if
     * an underlying network service is closed */
    public interface ShutdownHandler {
        void handleShutdown();
    }
    private ShutdownHandler shutdownHandler;

    public ScoreboardModelWrapper(final ScoreboardModel model) {
        this.model = model;
        
        new Thread("ScoreboardModelWrapper") {
            @Override
            public void run() {
                while(running) {
                    ScoreboardUpdate update = null;
                    try {
                        update = model.getUpdateEvent(ScoreboardModelWrapper.this.hashCode(), 3000);
                    } catch(Exception e) {
                        running = false;
                    }

                    if(update != null) {
                        Collection<ScoreboardModelListener> listenersCopy = new ArrayList<ScoreboardModelListener>(listeners);
                        for(ScoreboardModelListener listener : listenersCopy) {
                            try {
                                listener.handleScoreboardUpdate(update, model);
                            } catch(Exception e) {
                                log.error("Exception while notifying listener", e);
                            }
                        }
                    }
                }

                if(shutdownHandler != null) {
                    shutdownHandler.handleShutdown();
                }
            }
        }.start();

    }

    public void setShutdownHandler(ShutdownHandler handler) {
        this.shutdownHandler = handler;
    }
    
    @Override
    public void shutdown() {
        running = false;
    }
    
    @Override
    public void addListener(final ScoreboardModelListener listener) {
        listeners.add(listener);
        log.debug("addListener: " + listeners.size());
    }

    @Override
    public void removeListener(ScoreboardModelListener listener) {
        listeners.remove(listener);
        log.debug("removeListener: " + listeners.size());
    }

    @Override
    public ScoreboardUpdate getUpdateEvent(Object id, int timeout) {
        return model.getUpdateEvent(id, timeout);
    }

    @Override
    public String getEventLog() {
        return model.getEventLog();
    }

    @Override
    public GoldenScoreMode getGoldenScoreMode() {
        return model.getGoldenScoreMode();
    }

    @Override
    public int getHolddownPlayer() {
        return model.getHolddownPlayer();
    }

    @Override
    public int getHolddownTime() {
        return model.getHolddownTime();
    }

    @Override
    public Mode getMode() {
        return model.getMode();
    }

    @Override
    public Score getPendingHolddownScore() {
        return model.getPendingHolddownScore();
    }

    @Override
    public List<Score> getPendingScores(int player) {
        return model.getPendingScores(player);
    }

    @Override
    public String getPlayerName(int player) {
        return model.getPlayerName(player);
    }

    @Override
    public String getTeamName(int player) {
        return model.getTeamName(player);
    }

    @Override
    public String getDivisionName() {
        return model.getDivisionName();
    }

    @Override
    public int getScore(int player, Score type) {
        return model.getScore(player, type);
    }

    @Override
    public boolean isHansakumake(int player) {
        return model.isHansakumake(player);
    }

    @Override
    public int getTime() {
        return model.getTime();
    }

    @Override
    public Win getWin() {
        return model.getWin();
    }

    @Override
    public int getWinningPlayer() {
        return model.getWinningPlayer();
    }

    @Override
    public boolean isHolddownActivated() {
        return model.isHolddownActivated();
    }

    @Override
    public boolean isHolddownPending() {
        return model.isHolddownPending();
    }

    @Override
    public ScoringColors getColors() {
        return model.getColors();
    }

    @Override
    public boolean showTeams() {
        return model.showTeams();
    }

    @Override
    public boolean undoCancelHolddownAvailable() {
        return model.undoCancelHolddownAvailable();
    }

    @Override
    public void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, String divisionName) {
        model.reset(fightTime, goldenScoreTime, playerNames, teamNames, divisionName);
    }

    @Override
    public void reset(int fightTime, int goldenScoreTime, String[] playerNames, String[] teamNames, Date[] lastFights, int minimumBreak, String divisionName) {
        model.reset(fightTime, goldenScoreTime, playerNames, teamNames, lastFights, minimumBreak, divisionName);
    }

    @Override
    public void declarePlayerReady(int player) {
        model.declarePlayerReady(player);
    }

    @Override
    public void declareFightReady() {
        model.declareFightReady();
    }

    @Override
    public void setHolddownPlayer(int player) {
        model.setHolddownPlayer(player);
    }

    @Override
    public void approveGoldenScore() {
        model.approveGoldenScore();
    }

    @Override
    public void setGoldenScoreMode(GoldenScoreMode mode) {
        model.setGoldenScoreMode(mode);
    }

    @Override
    public void startHolddownTimer() {
        model.startHolddownTimer();
    }

    @Override
    public void startTimer() {
        model.startTimer();
    }

    @Override
    public void stopTimer() {
        model.stopTimer();
    }

    @Override
    public void toggleTimer() {
        model.toggleTimer();
    }

    @Override
    public void awardPendingScore(int player, int index) {
        model.awardPendingScore(player, index);
    }

    @Override
    public void cancelHolddownTimer() {
        model.cancelHolddownTimer();
    }

    @Override
    public void changeScore(int player, Score type, boolean up) {
        model.changeScore(player, type, up);
    }

    @Override
    public void endFight() {
        model.endFight();
    }

    @Override
    public void undoCancelHolddown() {
        model.undoCancelHolddown();
    }

    @Override
    public void setTimer(int seconds) {
        model.setTimer(seconds);
    }

    @Override
    public void setHolddownTimer(int seconds) {
        model.setHolddownTimer(seconds);
    }

    @Override
    public void decideWinner(int player) {
        model.decideWinner(player);
    }

    @Override
    public int getPendingFightTime(int player) {
        return model.getPendingFightTime(player);
    }
    
    @Override
    public void setColors(ScoringColors colors) {
        model.setColors(colors);
    }

    @Override
    public void setShowTeams(boolean show) {
        model.setShowTeams(show);
    }
}
