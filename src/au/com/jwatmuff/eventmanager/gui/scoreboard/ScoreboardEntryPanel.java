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

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.GoldenScoreMode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Mode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoreboardUpdate;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.ScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.SideBySideScoreboardLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Rect;
import au.com.jwatmuff.eventmanager.util.gui.ScalableLabel;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class ScoreboardEntryPanel extends ScoreboardDisplayPanel {
    private static final Logger log = Logger.getLogger(ScoreboardEntryPanel.class);

    private final JPanel topLayer;
    private final JPanel glassLayer;

    private final ScalableLabel[][][] scoreRegions;
    private final ScalableLabel[][][] scoreArrows;

    private final ScalableLabel[][][] shidoButton; // 2 players * 3 types * 2 sizes (big/small)
    private final ScalableLabel[][] shidoButtonRegion;
    private final ScalableLabel[][] shidoCross;
    private final ScalableLabel[][] shidoRegion;

    private final ScalableLabel[] holddownIcon;
    private final ScalableLabel holddownRegion;

    private final ScalableLabel decision;

    private final ScalableLabel[] holddownArrowRegion;
    private final ScalableLabel[] holddownArrow;

    private final ScalableLabel undo;
    private final ScalableLabel endFight;

    public static ScoreboardEntryPanel getInstance() {
        ScoreboardEntryPanel panel = new ScoreboardEntryPanel(new SideBySideScoreboardLayout());
        panel.init();
        return panel;
    }

    private ScoreboardEntryPanel(ScoreboardLayout scoreboardLayout) {
        super(scoreboardLayout);
        // Only default scoreboard layout is allowed
//        super(new SideBySideScoreboardLayout());

        imageFiles = new File[0];

        ScalableAbsoluteLayout layout;

        /*
         * Pending fight layer
         */

        for(int i = 0; i < 2; i++) {
            final int ii = i;
            onClick(pendingFightTimer[i], () -> {
                int status = JOptionPane.showConfirmDialog(
                        null,
                        "Declare player to be ready?",
                        "Player Ready",
                        JOptionPane.YES_NO_OPTION);
                if(status == JOptionPane.YES_OPTION)
                    model.declarePlayerReady(swapPlayers?(1-ii):ii);
            });
        }
        // Don't let clicks pass through this layer to layers below
        pendingFightLayer.addMouseListener(new MouseAdapter() { });

        /*
         * Winning result layer
         */
        layout = (ScalableAbsoluteLayout) resultLayer.getLayout();

        onClick(goldenScoreApprove, () -> model.approveGoldenScore());

        // Undo button
        undo = new ScalableLabel("U");
        undo.setVisible(false);
        onClick(undo, () -> {
            model.undoCancelHolddown();
        });
        layout.addComponent(undo, 15, 0, 1, 1);

        // End fight button
        endFight = new ScalableLabel("E");
        endFight.setVisible(false);
        onClick(endFight, () -> {
            int option = JOptionPane.showConfirmDialog(null, "Are you sure?", "Confirm Result", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(option == JOptionPane.YES_OPTION) {
                model.endFight();
            }
        });
        layout.addComponent(endFight, 0, 0, 1, 1);

        // Decision button
        decision = new ScalableLabel("D");
        decision.setVisible(false);
        onClick(decision, () -> {
            DecisionDialog dd = new DecisionDialog(null, true, model.getPlayerName(0), model.getPlayerName(1));
            dd.setVisible(true);
            if(dd.getSuccess()) {
                model.decideWinner(dd.getPlayer());
            }
        });
        layout.addComponent(decision, 0, 0, 1, 1);

        /*
         * Pending score layers
         */
        for(int i=0; i<2; i++) { // Up to 3 pending scores
            for(int j=0; j<3; j++) { // 2 players
                final int ii = i, jj = j;
                ScalableLabel label = pendingScores[i][j];
                onClick(label, () -> {
                    model.awardPendingScore(swapPlayers?1-ii:ii, jj);
                });
            }
        }

        /*
         * Holddown glass layer
         */
        JPanel hdGlassLayer = new JPanel();
        hdGlassLayer.setOpaque(false);
        layeredPane.add(hdGlassLayer, new Integer(5));
        layout = new ScalableAbsoluteLayout(hdGlassLayer, 16, 12);
        hdGlassLayer.setLayout(layout);
        holddownArrowRegion = new ScalableLabel[3];
        for(int i=0; i<3; i++) {
            final int ii=i;
            ScalableLabel label = new ScalableLabel("");
            label.setOpaque(false);
            label.setBorder(new EmptyBorder(0,0,0,0));
            label.setVisible(false);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent arg0) {
                    if(ii<2) {
                        model.setHolddownPlayer(swapPlayers?1-ii:ii);
                    } else {
                        model.cancelHolddownTimer();
                    }
                }
                @Override
                public void mouseEntered(MouseEvent arg0) {
                    holddownArrow[ii].setVisible(true);
                }
                @Override
                public void mouseExited(MouseEvent arg0) {
                    holddownArrow[ii].setVisible(false);
                }
            });
            holddownArrowRegion[i] = label;
        }
        layout.addComponent(holddownArrowRegion[0], 6, 8,  2, 3);
        layout.addComponent(holddownArrowRegion[1], 8, 8,  2, 3);
        layout.addComponent(holddownArrowRegion[2], 6, 11, 4, 1);

        /*
         * Holddown arrow layer
         */
        JPanel hdArrowLayer = new JPanel();
        hdArrowLayer.setOpaque(false);
        layeredPane.add(hdArrowLayer, new Integer(4));
        layout = new ScalableAbsoluteLayout(hdArrowLayer, 16, 12);
        hdArrowLayer.setLayout(layout);

        holddownArrow = new ScalableLabel[3];
        for(int i=0; i<3; i++) {
            holddownArrow[i] = new ScalableLabel(new String[] { "<", ">", "x" }[i]);
            holddownArrow[i].setVisible(false);
            holddownArrow[i].setBorder(new EmptyBorder(0,0,0,0));
            holddownArrow[i].setOpaque(false);
        }
        layout.addComponent(holddownArrow[0], 6, 8, 1, 1);
        layout.addComponent(holddownArrow[1], 9, 8, 1, 1);
        layout.addComponent(holddownArrow[2], 6, 11, 4, 1);

        /*
         * Top layer - for displaying arrows etc. over the top of other
         *             components
         */
        topLayer = new JPanel();
        topLayer.setOpaque(false);
        layeredPane.add(topLayer, new Integer(2));
        layout = new ScalableAbsoluteLayout(topLayer, 16, 12);
        topLayer.setLayout(layout);

        scoreArrows = new ScalableLabel[2][2][2];
        for(int i=0; i<2; i++) { //player
            for(int j=0; j<2; j++) { // score
                for(int k=0; k<2; k++) { // up/down
                    ScalableLabel label = new ScalableLabel(k==0?"^":"v");
                    label.setBorder(new EmptyBorder(0,0,0,0));
                    label.setOpaque(false);
                    label.setVisible(false);
                    Rect rect = scoreboardLayout.getScoreRect(i, Score.values()[j]);
                    rect = (k == 0) ? rect.topFraction(0.25) : rect.bottomFraction(0.25);
                    layout.addComponent(label, rect);
//                    double x = i*8 + j*2.5 - (j==0?0:1) + offset;
//                    double y = 5 + k*(j==0?1.5:3);
//                    double w = j==0?1:2;
//                    double h = j==0?0.5:1;
//                    layout.addComponent(label, x, y, w, h);
                    scoreArrows[i][j][k] = label;
                }
            }
        }

        /*
         * Glass layer - for detecting mouse over certain regions
         */
        glassLayer = new JPanel();
        glassLayer.setOpaque(false);
        layeredPane.add(glassLayer, new Integer(3));
        layout = new ScalableAbsoluteLayout(glassLayer, 16, 12);
        glassLayer.setLayout(layout);

        scoreRegions = new ScalableLabel[2][4][2];
        for(int i=0; i<2; i++) {            // player 1/2
            for(int j=0; j<2; j++) {
                for(int k=0; k<2; k++) {    // up/down
                    final int ii = i, jj = j, kk = k;

                    ScalableLabel label = new ScalableLabel("");
                    label.setOpaque(false);
                    label.setBorder(new EmptyBorder(0,0,0,0));
                    label.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(MouseEvent arg0) {
                            model.changeScore(swapPlayers?1-ii:ii, Score.values()[jj], kk==0);
                        }
                        @Override
                        public void mouseEntered(MouseEvent arg0) {
                            scoreArrows[ii][jj][kk].setVisible(true);
                        }
                        @Override
                        public void mouseExited(MouseEvent arg0) {
                            scoreArrows[ii][jj][kk].setVisible(false);
                        }
                    });
                    scoreRegions[i][j][k] = label;
//                    double x = i*8 + j*2.5 - (j==0?0:1) + offset;
//                    double y = 5 + k*(j==0?1.3:2);
//                    double w = j==0?1:2;
//                    double h = j==0?(k==0?1.3:0.7):2;
//                    layout.addComponent(label, x, y, w, h);
                    Rect rect = scoreboardLayout.getScoreRect(i, Score.values()[j]);
                    rect = (k == 0) ? rect.topFraction(0.67) : rect.bottomFraction(0.33);
                    layout.addComponent(label, rect);
                }
            }
        }

        shidoButtonRegion = new ScalableLabel[2][3];
        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 3; j++) {
                final Score shidoType = SHIDO_TYPES[j];
                final int ii = i;
                final int jj = j;
                ScalableLabel label = new ScalableLabel("");
                label.setOpaque(false);
                label.setBorder(new EmptyBorder(0,0,0,0));
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent arg0) {
                        model.changeScore(swapPlayers?1-ii:ii, shidoType, true);
                    }
                    @Override
                    public void mouseEntered(MouseEvent arg0) {
                        shidoButton[ii][jj][0].setVisible(false);
                        shidoButton[ii][jj][1].setVisible(true);
                    }
                    @Override
                    public void mouseExited(MouseEvent arg0) {
                        shidoButton[ii][jj][0].setVisible(true);
                        shidoButton[ii][jj][1].setVisible(false);
                    }

                });
                shidoButtonRegion[i][j] = label;
                double x = (i == 0) ? j : 15 - j;
                layout.addComponent(shidoButtonRegion[i][j], x, 11, 1, 1);
            }
        }


        holddownRegion = new ScalableLabel("");
        holddownRegion.setOpaque(false);
        holddownRegion.setBorder(new EmptyBorder(0,0,0,0));
        holddownRegion.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent arg0) {
                    if(model.getMode() == Mode.FIGHTING)
                        model.startHolddownTimer();
                }
                @Override
                public void mouseEntered(MouseEvent arg0) {
                    holddownIcon[0].setVisible(false);
                    holddownIcon[1].setVisible(true);
                }

                @Override
                public void mouseExited(MouseEvent arg0) {
                    if(!model.isHolddownActivated())
                        holddownIcon[0].setVisible(true);
                    holddownIcon[1].setVisible(false);
                }
        });
        layout.addComponent(holddownRegion, 7.5, 11, 1, 1);

        /*
         * Bottom layer - background and core elements such as timer, player
         *                names and scores
         */
        layout = (ScalableAbsoluteLayout)bottomLayer.getLayout();

        holddownIcon = new ScalableLabel[2];
        holddownIcon[0] = new ScalableLabel("O");
        layout.addComponent(holddownIcon[0], 7.75, 11.5, 0.5, 0.5);
        holddownIcon[1] = new ScalableLabel("O");
        holddownIcon[1].setVisible(false);
        layout.addComponent(holddownIcon[1], 7.5, 11, 1, 1);

        shidoButton = new ScalableLabel[2][3][2];
        for(int i=0; i<2; i++) { // player
            for(int j = 0; j < 3; j++) { // score
                Score s = SHIDO_TYPES[j];
                for(int k = 0; k < 2; k++) { // size
                    ScalableLabel label = new ScalableLabel("" + s.initial);
                    label.setVisible(k == 0);
                    double size = (k == 0) ? 0.5 : 1;
                    double x = (i == 0) ? j : 16 - size - j;
                    layout.addComponent(label, x, 12 - size, size, size);
                    shidoButton[i][j][k] = label;
                }
            }
        }

        /*
         * Shidos below are laid out dynamically inside updateShido
         */
        shidoCross = new ScalableLabel[2][4];
        shidoRegion = new ScalableLabel[2][4];
        for(int i=0; i<2; i++) {
            for(int j=0; j<4; j++) {
                final int ii = i, jj = j;

                final ScalableLabel cross = new ScalableLabel("x");
                cross.setVisible(false);
                cross.setBorder(new EmptyBorder(0,0,0,0));
                clearBackground(cross);
                shidoCross[i][j] = cross;

                ScalableLabel region = new ScalableLabel("");
                region.setBorder(new EmptyBorder(0,0,0,0));
                clearBackground(region);
                shidoRegion[i][j] = region;

                region.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if(shido[ii][jj].isVisible()) {
                            char scoreCharacter = shido[ii][jj].getText().charAt(0);
                            for(Score s : Score.values()) {
                                if(s.initial == scoreCharacter) {
                                    model.changeScore(swapPlayers?1-ii:ii, s, false);
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cross.setVisible(shido[ii][jj].isVisible());
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        cross.setVisible(false);
                    }
                });
            }
        }

        wireEvents();
    }

    private void onClick(JComponent c, final Consumer<MouseEvent> handler) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                handler.accept(e);
            }
        });
    }

    private void onClick(JComponent c, final Runnable handler) {
        onClick(c, e -> handler.run());
    }

    private void wireEvents() {
        onClick(timer, event -> {
            if(model.getMode() == Mode.FIGHT_PENDING) {
                model.declareFightReady();
                updateTimer();
            } else if(event.getClickCount() == 1) {
                model.toggleTimer();
            }
        });

        onClick(holddownTimer, event -> {
            if(event.getClickCount() == 1) {
                model.cancelHolddownTimer();
            }
        });
    }

    @Override
    void updateColors() {
        super.updateColors();

        for(ScalableLabel label : new ScalableLabel[] {
                holddownIcon[0], holddownIcon[1],
                holddownArrow[0], holddownArrow[1], holddownArrow[2],
                endFight, decision }
           ) {
            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
        }
        for(ScalableLabel[][] lss : shidoButton) {
            for(ScalableLabel[] ls : lss) {
                for(ScalableLabel label : ls) {
                    label.setForeground(Color.BLACK);
                    label.setBackground(Color.WHITE);
                }
            }
        }

        // Make sure score arrows match score colors
        for(int i = 0; i < 2; i++) { // player
            for(int j = 0; j < 2; j++) { // score
                Color fg = score[i][j].getForeground();
                Color bg = score[i][j].getBackground();
                for(int k=0; k<2; k++) {
                    scoreArrows[i][j][k].setForeground(fg);
                    scoreArrows[i][j][k].setBackground(bg);
                }
            }
        }
    }

    @Override
    void showHolddownTimer(boolean b) {
        super.showHolddownTimer(b);

        holddownRegion.setVisible(!b);
        holddownIcon[0].setVisible(!b);
        for(ScalableLabel label : holddownArrowRegion) {
            label.setVisible(b);
        }
    }

    @Override
    void updateResult() {
        super.updateResult();
        endFight.setVisible(model.getMode() == Mode.WIN);
    }

    @Override
    void updateGoldenScore() {
        super.updateGoldenScore();

        boolean gsFinished = model.getGoldenScoreMode() == GoldenScoreMode.FINISHED;
        boolean noWinner = model.getMode() != Mode.WIN;

        decision.setVisible(gsFinished && noWinner);
    }

//    private static File[] findImageFiles() {
//        return new File[0];
//    }

    @Override
    void updatePlayers() {
        super.updatePlayers();
        for(int i = 0; i < 2; i++) {
            team[i].setVisible(false);
        }
    }

    @Override
    void updatePendingFight() {
        super.updatePendingFight();
        vsLayer.setVisible(false); // Never show P1 vs P2 screen

        if(model.getMode().equals(Mode.FIGHT_PENDING)) {
            // Note - timer will not be visible if pendingFight layer is
            // active
            timer.setText("Click when Ready");
        }
    }

    @Override
    public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
        super.handleScoreboardUpdate(update, model);
        switch(update) {
            case UNDO_AVAILABLE:
                undo.setVisible(model.undoCancelHolddownAvailable());
        }
    }

    @Override
    void updateShido() {
        super.updateShido();

        ScalableAbsoluteLayout bottomLayout = (ScalableAbsoluteLayout) bottomLayer.getLayout();
        ScalableAbsoluteLayout topLayout = (ScalableAbsoluteLayout) topLayer.getLayout();
        ScalableAbsoluteLayout glassLayout = (ScalableAbsoluteLayout) glassLayer.getLayout();

        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 4; j++) {
                ScalableLabel region = shidoRegion[i][j];
                ScalableLabel cross = shidoCross[i][j];
                ScalableLabel label = shido[i][j];
                if(label.isVisible()) {
                    Rect r = bottomLayout.getRect(label);
                    topLayout.addComponent(cross, r.bottomFraction(0.3));
                    glassLayout.addComponent(region, r);
                    region.setVisible(true);
                } else {
                    cross.setVisible(false);
                    region.setVisible(false);
                }
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
