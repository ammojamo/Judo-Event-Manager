/*
 * ScoreboardPanel.java
 *
 * Created on 10 November 2008, 12:42
 */

package au.com.jwatmuff.eventmanager.gui.scoreboard;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.GoldenScoreMode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Mode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoreboardUpdate;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoringSystem;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors.Area;
import au.com.jwatmuff.eventmanager.util.FileExtensionFilter;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXImagePanel;

/**
 *
 * @author  James
 */
public class ScoreboardEntryPanel extends ScoreboardPanel implements ScoreboardModel.ScoreboardModelListener {    
    private static final Logger log = Logger.getLogger(ScoreboardEntryPanel.class);
    
    private ScoreboardModel model;
        
    private ScalableLabel timer;
    private ScalableLabel[] player;
    private ScalableLabel[][] scoreLabels;
    private ScalableLabel[][] score;
    private ScalableLabel division;

    private ScalableLabel[] pendingPlayer;
    private ScalableLabel[] pendingFightTimer;
    private ScalableLabel pendingDivision;
    
    private ScalableLabel[][][] scoreRegions;
    private ScalableLabel[][][] scoreArrows;
    
    private ScalableLabel[][] shido;
    private ScalableLabel[][][] shidoRegions;
    private ScalableLabel[][][] shidoArrows;
    
    private ScalableLabel[][] cornerShido;
    private ScalableLabel[] cornerShidoRegion;
    
    private ScalableLabel[] holddownIcon;
    private ScalableLabel holddownTimer;
    private ScalableLabel holddownRegion;

    private ScalableLabel decision;

    private ScalableLabel[] holddownArrowRegion;
    private ScalableLabel[] holddownArrow;
    
    private ScalableLabel[][] pendingScores;
    
    private ScalableLabel result;
    private ScalableLabel goldenScore;
    private ScalableLabel goldenScoreApprove;
    private ScalableLabel undo;
    private ScalableLabel endFight;
    
    private JPanel noFightLayer;
    private JPanel pendingFightLayer;

    private ScalableLabel vsPlayer[];
    private ScalableLabel vs;
    private ScalableLabel vsDivision;
    private JPanel vsLayer;
    
    private boolean swapPlayers;
    
    private boolean interactive = true;

    private int imageDisplayTime = 5000;
    private File[] imageFiles;
    private int lastImageIndex = -1;
    private JXImagePanel imageLayer;
    
    public ScoreboardEntryPanel() {
        this(true);
    }

    public ScoreboardEntryPanel(boolean interactive) {
//        this(interactive, ScoringSystem.OLD);
        this(interactive, ScoringSystem.NEW);
    }

    /** Creates new form ScoreboardPanel */
    public ScoreboardEntryPanel(boolean interactive, ScoringSystem system) {
        this.model = new ScoreboardModelImpl(system);
        this.interactive = interactive;
        imageFiles = interactive ? new File[0] : findImageFiles();

//        double offset = (system == ScoringSystem.NEW) ? 0.25 : 0;
        double offset = 0.25;

        ScalableAbsoluteLayout layout;
        
        //initComponents();
        setLayout(new GridLayout(1,1));
        
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        add(layeredPane);

        /*
         * Image screen layer
         */
        imageLayer = new JXImagePanel();
        imageLayer.setVisible(false);
        imageLayer.setStyle(JXImagePanel.Style.SCALED_KEEP_ASPECT_RATIO);
        imageLayer.setBackground(Color.BLACK);
        layeredPane.add(imageLayer, new Integer(13));

        /*
         * No fight screen layer
         */
        noFightLayer = new JPanel();
        noFightLayer.setOpaque(true);
        noFightLayer.setVisible(false);
        layeredPane.add(noFightLayer, new Integer(12));
        layout = new ScalableAbsoluteLayout(noFightLayer, 16, 12);
        noFightLayer.setLayout(layout);
        
        layout.addComponent(new ScalableLabel("No Fight"), 4, 4, 8, 4);
        /*
         * Player vs Player layer
         */
        vsLayer = new JPanel();
        vsLayer.setOpaque(true);
        vsLayer.setVisible(false);
        layeredPane.add(vsLayer, new Integer(11));
        layout = new ScalableAbsoluteLayout(vsLayer, 16, 12);
        vsLayer.setLayout(layout);
        vsPlayer = new ScalableLabel[] {
            new ScalableLabel("Player 1"),
            new ScalableLabel("Player 2")
        };
        vs = new ScalableLabel("vs");
        vsDivision = new ScalableLabel(" ");
        layout.addComponent(vsPlayer[0], 1, 1, 11, 3);
        layout.addComponent(vsPlayer[1], 4, 8, 11, 3);
        layout.addComponent(vs, 7, 5, 2, 2);
        layout.addComponent(vsDivision, 13, 0, 3, 1);

        /*
         * Pending fight layer
         */
        pendingFightLayer = new JPanel();
        pendingFightLayer.setOpaque(true);
        pendingFightLayer.setVisible(false);
        layeredPane.add(pendingFightLayer, new Integer(10));
        layout = new ScalableAbsoluteLayout(pendingFightLayer, 16, 12);
        pendingFightLayer.setLayout(layout);
        pendingPlayer = new ScalableLabel[] {
            new ScalableLabel("Player 1"),
            new ScalableLabel("Player 2")
        };
        pendingFightTimer = new ScalableLabel[] {
            new ScalableLabel("0:00"),
            new ScalableLabel("0:00")
        };
        pendingDivision = new ScalableLabel(" ");
        layout.addComponent(pendingPlayer[0], 1, 2.5, 6, 1.5);
        layout.addComponent(pendingPlayer[1], 9, 2.5, 6, 1.5);
        layout.addComponent(pendingFightTimer[0], 2, 5, 4, 2);
        layout.addComponent(pendingFightTimer[1], 10, 5, 4, 2);
        layout.addComponent(pendingDivision, 13, 0, 3, 1);
        
        for(int i = 0; i < 2; i++) {
            final int ii = i;
            if(interactive) {
                pendingFightTimer[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent evt) {
                        int status = JOptionPane.showConfirmDialog(
                                null,
                                "Declare player to be ready?",
                                "Player Ready",
                                JOptionPane.YES_NO_OPTION);
                        if(status == JOptionPane.YES_OPTION)
                            model.declarePlayerReady(swapPlayers?(1-ii):ii);
                    }
                });
            }
        }
        pendingFightLayer.addMouseListener(new MouseAdapter() { });

        /*
         * Winning result layer
         */
        JPanel resultLayer = new JPanel();
        resultLayer.setOpaque(false);
        layeredPane.add(resultLayer, new Integer(9));
        layout = new ScalableAbsoluteLayout(resultLayer, 16, 12);
        resultLayer.setLayout(layout);
        
        result = new ScalableLabel("");
        result.setVisible(false);
        layout.addComponent(result, 6, 8, 4, 4);
        
        goldenScore = new ScalableLabel("Golden Score");
        goldenScore.setVisible(false);
        layout.addComponent(goldenScore, 5, 0, 6, 0.5);
        
        goldenScoreApprove = new ScalableLabel("Golden Score");
        goldenScoreApprove.setVisible(false);
        if(interactive) {
            goldenScoreApprove.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent evt) {
                    model.approveGoldenScore();
                }
            });
        }
        layout.addComponent(goldenScoreApprove, 4, 0, 8, 2.5);
        
        if(interactive) {
            undo = new ScalableLabel("U");
            undo.setVisible(false);
            undo.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent evt) {
                    model.undoCancelHolddown();
                }
            });
            layout.addComponent(undo, 15, 0, 1, 1);
        }
        
        if(interactive) {
            endFight = new ScalableLabel("E");
            endFight.setVisible(false);
            endFight.addMouseListener(new MouseAdapter() {
               @Override
               public void mouseReleased(MouseEvent evt) {
                   int result = JOptionPane.showConfirmDialog(null, "Are you sure?", "Confirm Result", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                   if(result == JOptionPane.YES_OPTION) {
                       model.endFight();
                   }
               } 
            });
            layout.addComponent(endFight, 0, 0, 1, 1);
        }

        if(interactive) {
            decision = new ScalableLabel("D");
            decision.setVisible(false);
            decision.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent evt) {
                    DecisionDialog dd = new DecisionDialog(null, true, model.getPlayerName(0), model.getPlayerName(1));
                    dd.setVisible(true);
                    if(dd.getSuccess()) {
                        model.decideWinner(dd.getPlayer());
                    }
                }
            });
            layout.addComponent(decision, 0, 0, 1, 1);
        }

        /*
         * Pending score layers
         */
        JPanel[] psLayer = new JPanel[3];
        pendingScores = new ScalableLabel[2][3];
        for(int i=0; i<3; i++) {
            psLayer[i] = new JPanel();
            psLayer[i].setOpaque(false);
            layeredPane.add(psLayer[i], new Integer(8-i));
            layout = new ScalableAbsoluteLayout(psLayer[i], 16, 12);
            psLayer[i].setLayout(layout);

            for(int j=0; j<2; j++) {
                final int ii=i, jj=j;
                ScalableLabel label = new ScalableLabel("");
                label.setVisible(false);
                if(interactive) {
                    label.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(MouseEvent arg0) {
                            model.awardPendingScore(swapPlayers?1-jj:jj, ii);
                        }
                    });
                }
                double x = 3.5 - i * 1.5;
                if(j > 0) x = 14.0 - x;
                layout.addComponent(label, x, 8, 2, 2);
                pendingScores[j][i] = label;
            }
        }
        /*
         * Holddown glass layer
         */
        if(interactive) {
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
        }
        
        /*
         * Holddown arrow layer
         */
        if(interactive) {
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
        }        
        
        /*
         * Holddown layer
         */
        JPanel hdLayer = new JPanel();
        hdLayer.setOpaque(false);
        layeredPane.add(hdLayer, new Integer(3));
        layout = new ScalableAbsoluteLayout(hdLayer, 16, 12);
        hdLayer.setLayout(layout);
        
        holddownTimer = new ScalableLabel(" ");
        holddownTimer.setVisible(false);
        layout.addComponent(holddownTimer, 6, 8, 4, 4);

        /*
         * Top layer - for displaying arrows etc. over the top of other
         *             components
         */
        if(interactive) {
            JPanel topLayer = new JPanel();
            topLayer.setOpaque(false);
            layeredPane.add(topLayer, new Integer(2));
            layout = new ScalableAbsoluteLayout(topLayer, 16, 12);
            topLayer.setLayout(layout);

            scoreArrows = new ScalableLabel[2][4][2];
            for(int i=0; i<2; i++)
                for(int j=0; j<4; j++)
                    for(int k=0; k<2; k++) {
                        ScalableLabel label = new ScalableLabel(k==0?"^":"v");
                        label.setBorder(new EmptyBorder(0,0,0,0));
                        label.setOpaque(false);
                        label.setVisible(false);
                        double x = i*8 + j*2.5 - (j==0?0:1) + offset;
                        double y = 5 + k*(j==0?1.5:3);
                        double w = j==0?1:2;
                        double h = j==0?0.5:1;
                        layout.addComponent(label, x, y, w, h);
                        scoreArrows[i][j][k] = label;
                    }

            shidoArrows = new ScalableLabel[2][4][2];
            for(int i=0; i<2; i++)
                for(int j=0; j<4; j++)
                    for(int k=0; k<2; k++) {
                        ScalableLabel label = new ScalableLabel(k==0?"^":"v");
                        label.setBorder(new EmptyBorder(0,0,0,0));
                        label.setOpaque(false);
                        label.setVisible(false);
                        shidoArrows[i][j][k] = label;
                    }

            layout.addComponent(shidoArrows[0][0][0], 5.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[0][0][1], 5.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[0][1][0], 3.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[0][1][1], 3.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[0][2][0], 1.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[0][2][1], 1.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[0][3][0], 1.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[0][3][1], 1.5, 11,  1, 0.5);

            layout.addComponent(shidoArrows[1][0][0], 13.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[1][0][1], 13.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[1][1][0], 11.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[1][1][1], 11.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[1][2][0], 9.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[1][2][1], 9.5, 11,  1, 0.5);
            layout.addComponent(shidoArrows[1][3][0], 9.5, 9.5, 1, 0.5);
            layout.addComponent(shidoArrows[1][3][1], 9.5, 11,  1, 0.5);
        }
        
        /*
         * Glass layer - for detecting mouse over certain regions
         */
        if(interactive) {
            JPanel glassLayer = new JPanel();
            glassLayer.setOpaque(false);
            layeredPane.add(glassLayer, new Integer(3));
            layout = new ScalableAbsoluteLayout(glassLayer, 16, 12);
            glassLayer.setLayout(layout);

            scoreRegions = new ScalableLabel[2][4][2];
            for(int i=0; i<2; i++) {            // player 1/2
                for(int j=0; j<4; j++) {        // i/w/y/k
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
                        double x = i*8 + j*2.5 - (j==0?0:1) + offset;
                        double y = 5 + k*(j==0?1.3:2);
                        double w = j==0?1:2;
                        double h = j==0?(k==0?1.3:0.7):2;
                        layout.addComponent(label, x, y, w, h);
                    }
                }
            }

            shidoRegions = new ScalableLabel[2][4][2];
            for(int i=0; i<2; i++)
                for(int j=0; j<4; j++)
                    for(int k=0; k<2; k++) {
                        final int ii=i, jj=j, kk=k;
                        ScalableLabel label = new ScalableLabel("");
                        label.setOpaque(false);
                        label.setBorder(new EmptyBorder(0,0,0,0));
                        label.setVisible(false);
                        label.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseReleased(MouseEvent arg0) {
                                model.changeShido(swapPlayers?1-ii:ii, kk==0);
                            }                        
                            @Override
                            public void mouseEntered(MouseEvent arg0) {
                                shidoArrows[ii][jj][kk].setVisible(true);
                            }
                            @Override
                            public void mouseExited(MouseEvent arg0) {
                                shidoArrows[ii][jj][kk].setVisible(false);
                            }
                        });
                        shidoRegions[i][j][k] = label;
                    }

            layout.addComponent(shidoRegions[0][0][0], 5.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[0][0][1], 5.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[0][1][0], 3.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[0][1][1], 3.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[0][2][0], 1.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[0][2][1], 1.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[0][3][0], 1.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[0][3][1], 1.5, 10.5,  1, 1);

            layout.addComponent(shidoRegions[1][0][0], 13.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[1][0][1], 13.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[1][1][0], 11.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[1][1][1], 11.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[1][2][0], 9.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[1][2][1], 9.5, 10.5,  1, 1);
            layout.addComponent(shidoRegions[1][3][0], 9.5, 9.5, 1, 1);
            layout.addComponent(shidoRegions[1][3][1], 9.5, 10.5,  1, 1);

            cornerShidoRegion = new ScalableLabel[2];
            for(int i=0; i<2; i++) {
                final int ii = i;
                ScalableLabel label = new ScalableLabel("");
                label.setOpaque(false);
                label.setBorder(new EmptyBorder(0,0,0,0));
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent arg0) {
                        model.changeShido(swapPlayers?1-ii:ii, true);
                    }                        
                    @Override
                    public void mouseEntered(MouseEvent arg0) {
                        cornerShido[ii][0].setVisible(false);
                        cornerShido[ii][1].setVisible(true);
                    }
                    @Override
                    public void mouseExited(MouseEvent arg0) {
                        if(model.getShido(swapPlayers?1-ii:ii) == 0)
                            cornerShido[ii][0].setVisible(true);
                        cornerShido[ii][1].setVisible(false);
                    } 
                });
                cornerShidoRegion[i] = label;
            }
            layout.addComponent(cornerShidoRegion[0], 0, 11, 1, 1);
            layout.addComponent(cornerShidoRegion[1], 15, 11, 1, 1);

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

        }

        /*
         * Bottom layer - background and core elements such as timer, player
         *                names and scores
         */
        JPanel bottomLayer = new JPanel();
        layeredPane.add(bottomLayer, new Integer(1));
        layout = new ScalableAbsoluteLayout(bottomLayer, 16, 12);
        bottomLayer.setLayout(layout);
        bottomLayer.setOpaque(false);

        timer = new ScalableLabel("5:00");
        player = new ScalableLabel[] {
            new ScalableLabel("Player 1"),
            new ScalableLabel("Player 2")
        };

        layout.addComponent(timer, 5, 0, 6, 2.5);
        layout.addComponent(player[0], 0.25, 2.5, 7.5, 1.5);
        layout.addComponent(player[1], 8.25, 2.5, 7.5, 1.5);
        
        scoreLabels = new ScalableLabel[2][4];
        String[] iwyk = new String[]{"I","W","Y","K"};
        for(int i=0;i<2;i++)
            for(int j=0;j<4;j++) {
                scoreLabels[i][j] = new ScalableLabel(iwyk[j]);
                layout.addComponent(scoreLabels[i][j], i*8 + (j==0?0:(j*2.5-1)) + offset, 4, j==0?1:2, 1);
            }
        
        score = new ScalableLabel[2][4];
        for(int i=0;i<2;i++)
            for(int j=0;j<4;j++) {
                score[i][j] = new ScalableLabel("0");
                layout.addComponent(score[i][j], i*8 + (j==0?0:(j*2.5-1)) + offset, 5, j==0?1:2, j==0?2:4);
            }
        
        if(interactive) {
            holddownIcon = new ScalableLabel[2];
            holddownIcon[0] = new ScalableLabel("O");
            layout.addComponent(holddownIcon[0], 7.75, 11.5, 0.5, 0.5);
            holddownIcon[1] = new ScalableLabel("O");
            holddownIcon[1].setVisible(false);
            layout.addComponent(holddownIcon[1], 7.5, 11, 1, 1);
        }

        shido = new ScalableLabel[2][4];
        for(int i=0; i<2; i++)
            for(int j=0; j<4; j++) {
                shido[i][j] = new ScalableLabel(j<3?"S":"H");
                shido[i][j].setVisible(false);
            }
        
        layout.addComponent(shido[0][0], 5.5, 9.5, 1, 2);
        layout.addComponent(shido[0][1], 3.5, 9.5, 1, 2);
        layout.addComponent(shido[0][2], 1.5, 9.5, 1, 2);
        layout.addComponent(shido[0][3], 1.5, 9.5, 1, 2);

        layout.addComponent(shido[1][0], 13.5, 9.5, 1, 2);
        layout.addComponent(shido[1][1], 11.5, 9.5, 1, 2);
        layout.addComponent(shido[1][2], 9.5, 9.5, 1, 2);
        layout.addComponent(shido[1][3], 9.5, 9.5, 1, 2);

        if(interactive) {
            cornerShido = new ScalableLabel[2][2];
            for(int i=0; i<2; i++)
                for(int j=0; j<2; j++) {
                    cornerShido[i][j] = new ScalableLabel("S");
                    cornerShido[i][j].setVisible(j==0);
                }
            layout.addComponent(cornerShido[0][0], 0, 11.5, 0.5, 0.5);
            layout.addComponent(cornerShido[0][1], 0, 11, 1, 1);
            layout.addComponent(cornerShido[1][0], 15.5, 11.5, 0.5, 0.5);
            layout.addComponent(cornerShido[1][1], 15, 11, 1, 1);
        }

        // remove ippon
        if(!interactive) {
            scoreLabels[0][0].setVisible(false);
            scoreLabels[1][0].setVisible(false);
            score[0][0].setVisible(false);
            score[1][0].setVisible(false);
        }
        
        division = new ScalableLabel(" ");
        layout.addComponent(division, 13, 0, 3, 1);

        // set old KOKA's to invisible
        for(int i=0; i<2; i++) {
            if(interactive) {
                scoreArrows[i][3][0].setVisible(false);
                scoreArrows[i][3][1].setVisible(false);
                scoreRegions[i][3][0].setVisible(false);
                scoreRegions[i][3][1].setVisible(false);
            }
            scoreLabels[i][3].setVisible(false);
            score[i][3].setVisible(false);
        }
        
        if(interactive) wireEvents();
        updateColors();
        
        model.addListener(this);
        handleScoreboardUpdate(ScoreboardUpdate.ALL, model);
    }

    private void wireEvents() {
        timer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if(model.getMode() == Mode.FIGHT_PENDING) {
                    model.declareFightReady();
                    updateTimer();
                } else if(event.getClickCount() == 1) {
                    model.toggleTimer();
                }
            }
        });
        
        holddownTimer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if(event.getClickCount() == 1) {
                    model.cancelHolddownTimer();
                }
            }            
        });
    }
    
    public void swapPlayers() {
        swapPlayers = !swapPlayers;

        this.handleScoreboardUpdate(ScoreboardUpdate.ALL, model);
    }

    public void setImagesEnabled(boolean enabled) {
        imageFiles = enabled ? findImageFiles() : new File[0];
        updateNoFight();
        updateImages(false);
    }
    
    private void updateColors() {
        ScoringColors colors = model.getColors();

        switch(model.getHolddownPlayer()) {
            case 0:
                holddownTimer.setBackground(colors.getColor(Area.PLAYER1_BACKGROUND));
                holddownTimer.setForeground(colors.getColor(Area.PLAYER1_FOREGROUND));
                break;
            case 1:
                holddownTimer.setBackground(colors.getColor(Area.PLAYER2_BACKGROUND));
                holddownTimer.setForeground(colors.getColor(Area.PLAYER2_FOREGROUND));
                break;
            default:
                holddownTimer.setBackground(colors.getColor(Area.IDLE_BACKGROUND));
                holddownTimer.setForeground(colors.getColor(Area.IDLE_FOREGROUND));
        }

        for(ScalableLabel label : new ScalableLabel[] {
                timer,
                goldenScore, goldenScoreApprove, vs,
                division, vsDivision, pendingDivision} ) {
            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
        }
        
        if(interactive) {
            for(ScalableLabel label : new ScalableLabel[] {
                    holddownIcon[0], holddownIcon[1],
                    holddownArrow[0], holddownArrow[1], holddownArrow[2],
                    endFight, decision }
               ) {
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);
            }
        }

        for(ScalableLabel[] labels : shido)
            for(ScalableLabel label : labels) {
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);
            }

        if(interactive) {
            for(ScalableLabel[] labels : cornerShido)
                for(ScalableLabel label : labels) {
                    label.setForeground(Color.BLACK);
                    label.setBackground(Color.WHITE);
                }
        }
        
        Color mainBg;
        Color mainFg;
        
        switch(model.getMode()) {
            case FIGHTING:
                if(!model.isHolddownActivated()) {
                    mainBg = colors.getColor(Area.FIGHTING_BACKGROUND);
                    mainFg = colors.getColor(Area.FIGHTING_FOREGROUND);                
                } else {
                    mainBg = colors.getColor(Area.HOLDDOWN_BACKGROUND);
                    mainFg = colors.getColor(Area.HOLDDOWN_FOREGROUND);                                    
                }
                break;
            default:
                mainBg = colors.getColor(Area.IDLE_BACKGROUND);
                mainFg = colors.getColor(Area.IDLE_FOREGROUND);
                break;
        }
        setBackground(mainBg);
        setForeground(mainFg);

        for(Component c : new Component[] {
            noFightLayer,
            pendingFightLayer,
            vsLayer,
            pendingPlayer[0],
            pendingPlayer[1],
            pendingFightTimer[0],
            pendingFightTimer[1] }) {

            c.setBackground(mainBg);
            c.setForeground(mainFg);
            //if(c instanceof ScalableLabel)
            //    ((ScalableLabel)c).setBorder(new EmptyBorder(0,0,0,0));
        }
        
        for(int i=0; i<2; i++) {
            int playerPnt = swapPlayers?(1-i):i;
            Color fg = colors.getColor((playerPnt==0)?Area.PLAYER1_FOREGROUND:Area.PLAYER2_FOREGROUND);
            Color bg = colors.getColor((playerPnt==0)?Area.PLAYER1_BACKGROUND:Area.PLAYER2_BACKGROUND);
            
            player[i].setForeground(fg);
            player[i].setBackground(bg);

            pendingFightTimer[i].setForeground(fg);
            pendingFightTimer[i].setBackground(bg);

            pendingPlayer[i].setForeground(fg);
            pendingPlayer[i].setBackground(bg);
            
            vsPlayer[playerPnt].setForeground(fg);
            vsPlayer[playerPnt].setBackground(bg);

            for(int j=0; j<4; j++) {
                score[i][j].setForeground(fg);
                score[i][j].setBackground(bg);
                if(interactive) {
                    for(int k=0; k<2; k++) {
                        scoreArrows[i][j][k].setForeground(fg);
                        scoreArrows[i][j][k].setBackground(bg);
                    }
                }
            }
            
            for(int j=0; j<3; j++) {
                pendingScores[i][j].setForeground(fg);
                pendingScores[i][j].setBackground(bg);
            }
        }
        
        for(int i=0; i<2; i++)
            for(int j=0; j<4; j++) {
                scoreLabels[i][j].setBackground(mainBg);
                scoreLabels[i][j].setForeground(mainFg);
                scoreLabels[i][j].setBorder(new EmptyBorder(0,0,0,0));
            }
    }
    
    public ScoreboardModel getModel() {
        return model;
    }
    
    public void setModel(ScoreboardModel m) {
        if(model != null)
            model.removeListener(this);
        if(m != null) {
            model = m;
            model.addListener(this);
            handleScoreboardUpdate(ScoreboardUpdate.ALL, model);
        }
    }
    
    NumberFormat format = new DecimalFormat("00");

    private void updateScore() {
        for(int i=0; i<2; i++)
            for(int j=0; j<4; j++)
                score[i][j].setText(String.valueOf(model.getScore(swapPlayers?1-i:i, Score.values()[j])));
    }
    
    private void updateShido() {
        for(int i=0; i<2; i++) {
            int s = model.getShido(swapPlayers?1-i:i);

            for(int j=0; j<4; j++) {
                shido[i][j].setVisible((s>=j+1) && (j!=2 || s!=4)); /* complicated stuff to make H appear, gah! */
                if(interactive) {
                    shidoRegions[i][j][0].setVisible(s==j+1);
                    shidoRegions[i][j][1].setVisible(s==j+1);

                    if(s!=j+1) {
                        shidoArrows[i][j][0].setVisible(false);
                        shidoArrows[i][j][1].setVisible(false);
                    }
                }
            }
            
            if(interactive) {
                cornerShidoRegion[i].setVisible(s==0);
                cornerShido[i][0].setVisible(s == 0);
                if(s != 0) cornerShido[i][1].setVisible(false);
            }
        }
    }
    
    private void showHolddownTimer(boolean b) {
        holddownTimer.setVisible(b);
        if(interactive) {
            holddownRegion.setVisible(!b);
            holddownIcon[0].setVisible(!b);
            for(ScalableLabel label : holddownArrowRegion)
                label.setVisible(b);
        }
    }
    
    private void updateHolddownTimer() {
        if(model.getGoldenScoreMode() != GoldenScoreMode.INACTIVE)
            updateGoldenScore();
        
        if(model.isHolddownPending()) {
            holddownTimer.setText(model.getPendingHolddownScore().toString().substring(0,1));
        } else {
            holddownTimer.setText(format.format(model.getHolddownTime()));        
        }
    }
    
    private void updatePendingScores() {
        
        if(model.getGoldenScoreMode() != GoldenScoreMode.INACTIVE)
            updateGoldenScore();

        for(int i=0; i<2; i++) {
            List<Score> scores = model.getPendingScores(swapPlayers?1-i:i);
            
            for(int j=0; j<3; j++) {
                if(j >= scores.size()) {
                    pendingScores[i][j].setVisible(false);
                } else {
                    pendingScores[i][j].setText(scores.get(j).toString().substring(0,1));
                    pendingScores[i][j].setVisible(true);
                }
            }
        }
    }
    
    private void updateResult() {
        if(model.getGoldenScoreMode() != GoldenScoreMode.INACTIVE)
            updateGoldenScore();

        if(model.getMode() == Mode.WIN) {
            if(model.getWinningPlayer() == 0) {
                result.setBackground(model.getColors().getColor(Area.PLAYER1_BACKGROUND));
                result.setForeground(model.getColors().getColor(Area.PLAYER1_FOREGROUND));
            } else {
                result.setBackground(model.getColors().getColor(Area.PLAYER2_BACKGROUND));
                result.setForeground(model.getColors().getColor(Area.PLAYER2_FOREGROUND));
            }
            if(model.getWin() != null) {
                Score winScore = model.getWin().score;
                String displayed;
                if(winScore == Score.IPPON) {
                        if(model.getShido(1 - model.getWinningPlayer()) == 4)
                            displayed = "H";
                        else if(model.getShido(1 - model.getWinningPlayer()) == 3 &&
                                model.getScore(model.getWinningPlayer(), Score.WAZARI) == 2)
                            displayed = "S";
                        else
                            displayed = "I";
                } else {
                    displayed = winScore.toString().substring(0, 1);
                }
                result.setText(displayed);
            } else {
                result.setText("?");
            }
            result.setVisible(true);
            if(interactive) endFight.setVisible(true);
        } else {
            result.setVisible(false);
            if(interactive) endFight.setVisible(false);
        }
    }
    
    private void updatePlayers() {
        for(int i=0; i<2; i++)
            player[i].setText(model.getPlayerName(swapPlayers?1-i:i));
    }
    
    private void updateDivision() {
        String divisionName = model.getDivisionName();
        boolean showDivision = !StringUtils.isEmpty(divisionName);
        for(ScalableLabel label : new ScalableLabel[] { division, vsDivision, pendingDivision} ) {
            label.setVisible(showDivision);
            label.setText(showDivision ? divisionName : " ");
        }
    }
    
    private void updateGoldenScore() {
        switch(model.getGoldenScoreMode()) {
            case ACTIVE:
            case FINISHED:
                goldenScore.setVisible(true);
                goldenScoreApprove.setVisible(false);
                break;
            case READY:
                goldenScore.setVisible(false);

                /* don't display golden rule approve if we have a winner, pending score or are in holddown */
                if(model.getWin() != null ||
                   model.isHolddownActivated() ||
                   model.getPendingScores(0).size() > 0 ||
                   model.getPendingScores(1).size() > 0) {
                    goldenScoreApprove.setVisible(false);
                } else {
                    goldenScoreApprove.setVisible(true);
                }
                
                break;
            default:
                goldenScore.setVisible(false);
                goldenScoreApprove.setVisible(false);
        }

        if(model.getGoldenScoreMode() == GoldenScoreMode.FINISHED &&
           model.getMode() != Mode.WIN) {
            if(interactive) decision.setVisible(true);
        } else {
            if(interactive) decision.setVisible(false);
        }
    }
    
    private void updateTimer() {
        int sec = model.getTime();
        String str = sec/60 + ":" + format.format(sec%60);
        timer.setText(str);        
    }
    
    private void updateNoFight() {
        boolean visible = (model.getMode() == Mode.NO_FIGHT) && imageFiles.length == 0;
        noFightLayer.setVisible(visible);
    }

    private synchronized void updateImages(boolean endOfCycle) {
        if(imageFiles.length > 0) {
            switch(model.getMode()) {
                case NO_FIGHT:
                    if(endOfCycle) loadNextImage();
                    else           startImageDisplay();
                    break;
                case FIGHT_PENDING:
                    if(endOfCycle) stopImageDisplay();
                    break;
                default:
                    stopImageDisplay();
            }
        } else {
            stopImageDisplay();
        }
    }

    private boolean imageDisplayed = false;
    private void startImageDisplay() {
        if(!imageDisplayed) {
            imageDisplayed = true;
            loadNextImage();
            imageLayer.setVisible(true);
            imageUpdateThread = new ImageUpdateThread();
            imageUpdateThread.start();
        }
    }

    private void stopImageDisplay() {
        if(imageDisplayed) {
            imageDisplayed = false;
            imageUpdateThread.shutdown();
            imageUpdateThread = null;
            imageLayer.setVisible(false);
        }
    }

    private void loadNextImage() {
        int imageIndex = (lastImageIndex + 1) % imageFiles.length;
        lastImageIndex = imageIndex;
        File imageFile = imageFiles[imageIndex];
        try {
            Image image = ImageIO.read(imageFile);
            imageLayer.setImage(image);
        } catch(IOException e) {
            log.error("Failed to load image " + imageFile.getAbsolutePath(), e);
        }
    }

    private class ImageUpdateThread extends Thread {
        private boolean run = true;
        @Override
        public void run() {
            while(run) {
                try {
                    Thread.sleep(imageDisplayTime);
                } catch(InterruptedException e) {}
                if(!run) break;
                updateImages(true);
            }
        }

        public void shutdown() {
            run = false;
            interrupt();
        }
    }

    private ImageUpdateThread imageUpdateThread;

    private static File[] findImageFiles() {
        File[] files = new File("resources/advertising").listFiles(new FileExtensionFilter("jpeg", "jpg", "png", "gif"));
        return files == null ? new File[0] : files;
    }

    private void updatePendingFight() {
        if(model.getMode().equals(Mode.FIGHT_PENDING)) {
            if(model.getPendingFightTime(0) > 0 ||
               model.getPendingFightTime(1) > 0) {
                for(int i=0; i<2; i++) {
                    int sec = model.getPendingFightTime(swapPlayers?1-i:i);
                    String str = "Ready";
                    if(sec > 0) str = sec/60 + ":" + format.format(sec%60);
                    pendingFightTimer[i].setText(str);
                    pendingPlayer[i].setText(model.getPlayerName(swapPlayers?1-i:i));
                }
                pendingFightLayer.setVisible(true);
            } else {
                pendingFightLayer.setVisible(false);
                if(!interactive) {
                    for(int i=0; i<2; i++)
//                        vsPlayer[i].setText(model.getPlayerName(swapPlayers?1-i:i));
                        vsPlayer[i].setText(model.getPlayerName(i));
                    vsLayer.setVisible(true);
                }
                if(interactive)
                    timer.setText("Click when Ready");
            }
        } else {
            pendingFightLayer.setVisible(false);
            vsLayer.setVisible(false);
        }
    }
    
    @Override
    public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
        switch(update) {
            case TIMER:
                updateTimer();
                break;
            case HOLDDOWN:
                updateHolddownTimer();
                break;
            case MODE:
                updateResult();
                showHolddownTimer(model.isHolddownActivated());
                if(model.isHolddownActivated())
                    updateHolddownTimer();
                updateColors();
                updateNoFight();
                updateImages(false);
                updatePendingFight();
                updateDivision();
                break;
            case SCORE:
                updateScore();
                updateResult();
                break;
            case PENDING_SCORE:
                updatePendingScores();
                break;
            case SHIDO:
                updateScore();
                updateShido();
                updateResult();
                break;
            case UNDO_AVAILABLE:
                if(interactive) {
                    undo.setVisible(model.undoCancelHolddownAvailable());
                }
                break;
            case GOLDEN_SCORE:
                updateGoldenScore();
                break;
            case SIREN:
                break;
            case FIGHT_PENDING:
                updatePendingFight();
                updateDivision();
                break;
            case ALL:
                updateTimer();
                updateHolddownTimer();
                updatePlayers();
                updateScore();
                updateShido();
                updatePendingScores();
                updateColors();
                updateNoFight();
                updateImages(false);
                updateGoldenScore();
                updatePendingFight();
                updateDivision();
                break;
            default:
                log.info("Unknown update event: " + update);
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
