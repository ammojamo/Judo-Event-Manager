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
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXImagePanel;

/**
 *
 * @author  James
 */
public class DefaultScoreboardDisplayPanel extends ScoreboardPanel implements ScoreboardModel.ScoreboardModelListener {
    private static final Logger log = Logger.getLogger(ScoreboardEntryPanel.class);
    
//    private ScoreboardModel model = new ScoreboardModelImpl(ScoringSystem.OLD);
    private ScoreboardModel model = new ScoreboardModelImpl(ScoringSystem.NEW);
    
    protected static class LayoutValues {
        double[] SCORE_GROUP_X = new double[]{0.25, 8.25}; // Player 1, Player 2
        double[] SCORE_GROUP_Y = new double[]{4, 4}; // P1, P2
        
        double[] SCORE_X = new double[] {0, 1.5, 4}; // I, W, Y
        double[] SCORE_Y = new double[] {1, 1, 1}; // I, W, Y
        double[] SCORE_WIDTHS = new double[] {1, 2, 2}; // I, W, Y
        double[] SCORE_HEIGHTS = new double[] {2, 4, 4}; // I, W, Y

        double[] SCORE_LABEL_X = new double[] {0, 1.5, 4}; // I, W, Y
        double[] SCORE_LABEL_Y = new double[] {0, 0, 0}; // I, W, Y
        double[] SCORE_LABEL_WIDTHS = new double[] {1, 2, 2}; // I, W, Y
        double[] SCORE_LABEL_HEIGHTS = new double[] {1, 1, 1}; // I, W, Y
        
        boolean SCORE_SHOW_IPON = false; 

        double[] PLAYER_LABEL_Y = new double[] {2.5, 2.5}; // P1, P2
        double[] PLAYER_LABEL_X = new double[] {0.25, 8.25}; // P1, P2
        double PLAYER_LABEL_WIDTH = 7.5;
        double PLAYER_LABEL_HEIGHT = 1.5;

        double[] HD_SCORE_GROUP_X = new double[]{0.5, 10.5}; // P1, P2
        double[] HD_SCORE_GROUP_Y = new double[]{8, 8}; // P1, P2
        double[] HD_SCORE_X = new double[]{0, 1.5, 3}; // S,S,S,H
        double[] HD_SCORE_Y = new double[]{0, 0, 0}; // S,S,S,H
        double HD_SCORE_WIDTH = 2;
        double HD_SCORE_HEIGHT = 2;

        double[] SHIDO_GROUP_X = new double[]{1.5, 9.5}; // P1, P2
        double[] SHIDO_GROUP_Y = new double[]{9.5, 9.5}; // P1, P2
        double[] SHIDO_X = new double[]{4, 2, 0, 0}; // S,S,S,H
        double[] SHIDO_Y = new double[]{0, 0, 0, 0}; // S,S,S,H
        double SHIDO_WIDTH = 1;
        double SHIDO_HEIGHT = 2;

        double TIMER_X = 5;
        double TIMER_Y = 0;
        double TIMER_WIDTH = 6;
        double TIMER_HEIGHT = 2.5;

        double HOLD_DOWN_X = 8;
        double HOLD_DOWN_Y = 6;
        double HOLD_DOWN_WIDTH = 4;
        double HOLD_DOWN_HEIGHT = 4;
    }

    private ScalableLabel timer;
    private ScalableLabel[] player;
    private ScalableLabel[][] scoreLabels;
    private ScalableLabel[][] score;

    private ScalableLabel[] pendingPlayer;
    private ScalableLabel[] pendingFightTimer;
        
    private ScalableLabel[][] shido;
    
    private ScalableLabel holddownTimer;
    
    private ScalableLabel[][] pendingScores;
    
    private ScalableLabel result;
    private ScalableLabel goldenScore;
    private ScalableLabel goldenScoreApprove;
    
    private JPanel noFightLayer;
    private JPanel pendingFightLayer;

    private ScalableLabel vsPlayer[];
    private ScalableLabel vs;
    private JPanel vsLayer;
    
    private boolean swapPlayers;

    private int imageDisplayTime = 5000;
    private File[] imageFiles = findImageFiles();
    private int lastImageIndex = -1;
    private JXImagePanel imageLayer;

    /** Creates new form ScoreboardPanel */
    public DefaultScoreboardDisplayPanel() {
        this(new LayoutValues());
    }

    public DefaultScoreboardDisplayPanel(LayoutValues lv) {
        ScalableAbsoluteLayout layout;
        
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
        layout.addComponent(vsPlayer[0], 1, 1, 11, 3);
        layout.addComponent(vsPlayer[1], 4, 8, 11, 3);
        layout.addComponent(vs, 7, 5, 2, 2);

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
        layout.addComponent(pendingPlayer[0], 1, 2.5, 6, 1.5);
        layout.addComponent(pendingPlayer[1], 9, 2.5, 6, 1.5);
        layout.addComponent(pendingFightTimer[0], 2, 5, 4, 2);
        layout.addComponent(pendingFightTimer[1], 10, 5, 4, 2);
        
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
        layout.addComponent(goldenScoreApprove, 4, 0, 8, 2.5);
        
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
                ScalableLabel label = new ScalableLabel("");
                label.setVisible(false);
                layout.addComponent(label, lv.HD_SCORE_GROUP_X[j] + lv.HD_SCORE_X[i], lv.HD_SCORE_GROUP_Y[j] + lv.HD_SCORE_Y[i], lv.HD_SCORE_WIDTH, lv.HD_SCORE_HEIGHT);
                pendingScores[j][i] = label;
            }
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
        layout.addComponent(holddownTimer, lv.HOLD_DOWN_X, lv.HOLD_DOWN_Y, lv.HOLD_DOWN_WIDTH, lv.HOLD_DOWN_HEIGHT);

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

        layout.addComponent(timer, lv.TIMER_X, lv.TIMER_Y, lv.TIMER_WIDTH, lv.TIMER_HEIGHT);
        for(int i = 0; i < 2; i++) {
            layout.addComponent(player[i], lv.PLAYER_LABEL_X[i], lv.PLAYER_LABEL_Y[i], lv.PLAYER_LABEL_WIDTH, lv.PLAYER_LABEL_HEIGHT);
        }
        
        scoreLabels = new ScalableLabel[2][3];
        score = new ScalableLabel[2][3];
        String[] iwyk = new String[]{"I","W","Y"};
        for(int i=0;i<2;i++) {
            for(int j=0;j<3;j++) {
                scoreLabels[i][j] = new ScalableLabel(iwyk[j]);
                layout.addComponent(scoreLabels[i][j], lv.SCORE_GROUP_X[i] + lv.SCORE_LABEL_X[j], lv.SCORE_GROUP_Y[i] + lv.SCORE_LABEL_Y[j], lv.SCORE_LABEL_WIDTHS[j], lv.SCORE_LABEL_HEIGHTS[j]);
                score[i][j] = new ScalableLabel("0");
                layout.addComponent(score[i][j], lv.SCORE_GROUP_X[i] + lv.SCORE_X[j], lv.SCORE_GROUP_Y[i] + lv.SCORE_Y[j], lv.SCORE_WIDTHS[j], lv.SCORE_HEIGHTS[j]);
            }
        }
        
        shido = new ScalableLabel[2][4];
        for(int i=0; i<2; i++) {
            for(int j=0; j<4; j++) {
                shido[i][j] = new ScalableLabel(j<3?"S":"H");
                shido[i][j].setVisible(false);
                layout.addComponent(shido[i][j], lv.SHIDO_GROUP_X[i] + lv.SHIDO_X[j], lv.SHIDO_GROUP_Y[i] + lv.SHIDO_Y[j], lv.SHIDO_WIDTH, lv.SHIDO_HEIGHT);
            }
        }
        
        // remove ippon
        if(!lv.SCORE_SHOW_IPON) {
            scoreLabels[0][0].setVisible(false);
            scoreLabels[1][0].setVisible(false);
            score[0][0].setVisible(false);
            score[1][0].setVisible(false);
        }

        updateColors();
        
        model.addListener(this);
    }

    // for overriding in subclasses
    protected void setLayoutValues() {
    }
    
    @Override
    public void swapPlayers() {
        swapPlayers = !swapPlayers;

        this.handleScoreboardUpdate(ScoreboardUpdate.ALL, model);
    }
        
    public void setPlayerName(String playerName, int n) {
        player[swapPlayers?1-n:n].setText(playerName);
    }

    @Override
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
                goldenScore, goldenScoreApprove, vs} ) {
            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
        }
        
        for(ScalableLabel[] labels : shido)
            for(ScalableLabel label : labels) {
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);
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

            for(int j=0; j<3; j++) {
                score[i][j].setForeground(fg);
                score[i][j].setBackground(bg);
            }
            
            for(int j=0; j<3; j++) {
                pendingScores[i][j].setForeground(fg);
                pendingScores[i][j].setBackground(bg);
            }
        }
        
        for(int i=0; i<2; i++)
            for(int j=0; j<3; j++) {
                scoreLabels[i][j].setBackground(mainBg);
                scoreLabels[i][j].setForeground(mainFg);
                scoreLabels[i][j].setBorder(new EmptyBorder(0,0,0,0));
            }
    }
    
    @Override
    public ScoreboardModel getModel() {
        return model;
    }
    
    @Override
    public void setModel(ScoreboardModel m) {
        if(model != null)
            model.removeListener(this);
        if(m != null) {
            model = m;
            model.addListener(this);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                handleScoreboardUpdate(ScoreboardUpdate.ALL, model);
            }
        });
    }
    
    NumberFormat format = new DecimalFormat("00");

    private void updateScore() {
        for(int i=0; i<2; i++)
            for(int j=0; j<3; j++)
                score[i][j].setText(String.valueOf(model.getScore(swapPlayers?1-i:i, Score.values()[j])));
    }
    
    private void updateShido() {
        for(int i=0; i<2; i++) {
            int s = model.getShido(swapPlayers?1-i:i);

            for(int j=0; j<4; j++) {
                shido[i][j].setVisible((s>=j+1) && (j!=2 || s!=4)); /* complicated stuff to make H appear, gah! */
            }
            
        }
    }
    
    private void showHolddownTimer(boolean b) {
        holddownTimer.setVisible(b);
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
        } else {
            result.setVisible(false);
        }
    }
    
    private void updatePlayers() {
        for(int i=0; i<2; i++)
            player[i].setText(model.getPlayerName(swapPlayers?1-i:i));
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
    }
    
    public void updateTimer() {
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

                for(int i=0; i<2; i++)
                    vsPlayer[i].setText(model.getPlayerName(i));
                vsLayer.setVisible(true);
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
                break;
            case GOLDEN_SCORE:
                updateGoldenScore();
                break;
            case SIREN:
                break;
            case FIGHT_PENDING:
                updatePendingFight();
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
