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

package au.com.jwatmuff.eventmanager.gui.scoring;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors.Area;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.ResultRecorder;
import au.com.jwatmuff.eventmanager.model.misc.UpcomingFightFinder;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.FullScore;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class SimpleScoringPanel extends javax.swing.JPanel implements TransactionListener {
    private static final Logger log = Logger.getLogger(SimpleScoringPanel.class);

    private Database database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private Fight currentFight;
    private ScalableScoringPanel ssp1, ssp2;
    private Session matSession;
    private FightPlayer player1, player2;

    private Result pendingResult;
    
    /** Creates new form FightProgressionPanel */
    public SimpleScoringPanel(Session matSession) {
        initComponents();
        this.matSession = matSession;
        
        mainPanel.setLayout(new GridLayout(1,2));
        ssp1 = new ScalableScoringPanel();
        ssp1.setColors(Color.BLACK, Color.YELLOW, Color.BLACK, Color.WHITE);
        mainPanel.add(ssp1);
        
        ssp2 = new ScalableScoringPanel();
        ssp2.setColors(Color.BLACK, Color.YELLOW, Color.WHITE, Color.BLUE);
        mainPanel.add(ssp2);

        MouseListener ml1 = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if(currentFight == null) return;

                if(pendingResult == null) {
                    ssp1.showConfirmButtons(true);
                    ssp2.showConfirmButtons(false);
                    ssp1.showScoreButtons(false);
                    ssp2.showScoreButtons(false);

                    pendingResult = new Result();

                    pendingResult.setPlayerIDs(new int[] {
                        (player1 == null || player1.player == null) ? -1 : player1.player.getID(),
                        (player2 == null || player2.player == null) ? -1 : player2.player.getID()
                    });

                    pendingResult.setFightID(currentFight.getID());
                    FullScore score = new FullScore();
                    
                    Score s = scoreForButton(ssp1, evt.getComponent());
                    if(s != null) {
                        score.set(s, 1);
                    } else {
                        pendingResult = null;
                        return;
                    }
                    pendingResult.setScores(new FullScore[]{score, new FullScore()});
                    evt.getComponent().setVisible(true);
                } else {
                    if(evt.getComponent() == ssp1.confirmButton) {
                        ResultRecorder.recordResult(database, pendingResult);
                        pendingResult = null;
                    }
                    else if(evt.getComponent() == ssp1.cancelButton)
                        pendingResult = null;
                    else
                        return;

                    ssp1.showConfirmButtons(false);
                    ssp2.showConfirmButtons(false);
                    ssp1.showScoreButtons(true);
                    ssp2.showScoreButtons(true);
                }
            }
        };

        ssp1.iButton.addMouseListener(ml1);
        ssp1.wButton.addMouseListener(ml1);
        ssp1.dButton.addMouseListener(ml1);
        ssp1.cancelButton.addMouseListener(ml1);
        ssp1.confirmButton.addMouseListener(ml1);
        
        MouseListener ml2 = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {


                if(currentFight == null) return;

                if(pendingResult == null) {
                    ssp1.showConfirmButtons(false);
                    ssp2.showConfirmButtons(true);
                    ssp1.showScoreButtons(false);
                    ssp2.showScoreButtons(false);

                    pendingResult = new Result();

                    pendingResult.setPlayerIDs(new int[] {
                        (player1 == null || player1.player == null) ? -1 : player1.player.getID(),
                        (player2 == null || player2.player == null) ? -1 : player2.player.getID()
                    });
                    
                    pendingResult.setFightID(currentFight.getID());
                    FullScore score = new FullScore();

                    Score s = scoreForButton(ssp2, evt.getComponent());
                    if(s != null) {
                        score.set(s, 1);
                    } else {
                        pendingResult = null;
                        return;
                    }

                    pendingResult.setScores(new FullScore[]{new FullScore(), score});
                    evt.getComponent().setVisible(true);
                } else {
                    if(evt.getComponent() == ssp2.confirmButton) {
                        ResultRecorder.recordResult(database, pendingResult);
                        pendingResult = null;
                    }
                    else if(evt.getComponent() == ssp2.cancelButton)
                        pendingResult = null;
                    else
                        return;

                    ssp1.showConfirmButtons(false);
                    ssp2.showConfirmButtons(false);
                    ssp1.showScoreButtons(true);
                    ssp2.showScoreButtons(true);
                }
            }
        };

        ssp2.iButton.addMouseListener(ml2);
        ssp2.wButton.addMouseListener(ml2);
        ssp2.dButton.addMouseListener(ml2);
        ssp2.cancelButton.addMouseListener(ml2);
        ssp2.confirmButton.addMouseListener(ml2);
    }
    
    private Score scoreForButton(ScalableScoringPanel ssp, Component component) {
        if(component == ssp.iButton) return Score.IPPON;
        if(component == ssp.wButton) return Score.WAZARI;
        if(component == ssp.dButton) return Score.DECISION;
        return null;
    }
    
    public void swapPlayers() {
        Component c1 = mainPanel.getComponent(0);
        Component c2 = mainPanel.getComponent(1);
        mainPanel.removeAll();
        mainPanel.add(c2);
        mainPanel.add(c1);
        this.updateUI();
    }
    
    public void setScoringColors(ScoringColors sc) {
        ssp1.setColors(
            sc.getColor(Area.IDLE_FOREGROUND),
            sc.getColor(Area.IDLE_BACKGROUND),
            sc.getColor(Area.PLAYER1_FOREGROUND),
            sc.getColor(Area.PLAYER1_BACKGROUND));
        ssp2.setColors(
            sc.getColor(Area.IDLE_FOREGROUND),
            sc.getColor(Area.IDLE_BACKGROUND),
            sc.getColor(Area.PLAYER2_FOREGROUND),
            sc.getColor(Area.PLAYER2_BACKGROUND));
    }
    
    @Required
    public void setDatabase(Database database) {
        this.database = database;
    }
    
    @Required
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    public void afterPropertiesSet() {
        notifier.addListener(this, Result.class, Fight.class, SessionFight.class, Session.class);
        updateFightFromDatabase();
    }

    public void destroy() {
        notifier.removeListener(this);
    }
    
    private void updateFightFromDatabase() {
        if(matSession == null) return;
        
        Collection<Fight> fights;
        try {
            fights = UpcomingFightFinder.findUpcomingFights(database, matSession.getID(), 1);
            if(fights.size() > 0) {
                Fight nextFight = fights.iterator().next();
                for(int i=0; i<2; i++) {
                    FightPlayer nextPlayer = PlayerCodeParser.parseCode(database, nextFight.getPlayerCodes()[i], nextFight.getPoolID());
                    if(nextPlayer.type == PlayerType.UNDECIDED){
                        ssp1.playerLabel.setText("---");
                        ssp2.playerLabel.setText("---");
                        currentFight = null;
                        return;
                    }
                }
            }
        } catch (DatabaseStateException e) {
            log.error("Unable to find upcoming fight", e);
            return;
        }

        if(fights.size() > 0) {
            currentFight = fights.iterator().next();

            try {
                player1 = PlayerCodeParser.parseCode(database, currentFight.getPlayerCodes()[0], currentFight.getPoolID());
                ssp1.playerLabel.setText(player1.toString());
            } catch(DatabaseStateException e) {
                player1 = null;
                ssp1.playerLabel.setText(currentFight.getPlayerCodes()[0]);
            }

            try {
                player2 = PlayerCodeParser.parseCode(database, currentFight.getPlayerCodes()[1], currentFight.getPoolID());
                ssp2.playerLabel.setText(player2.toString());
            } catch(DatabaseStateException e) {
                player2 = null;
                ssp2.playerLabel.setText(currentFight.getPlayerCodes()[1]);
            }
        } else {
            ssp1.playerLabel.setText("---");
            ssp2.playerLabel.setText("---");
            currentFight = null;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    
    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateFightFromDatabase();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables

    
}
