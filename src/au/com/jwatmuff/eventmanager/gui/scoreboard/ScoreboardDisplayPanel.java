/*
 * ScoreboardPanel.java
 *
 * Created on 10 November 2008, 12:42
 */

package au.com.jwatmuff.eventmanager.gui.scoreboard;

import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.ScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.GoldenScoreMode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Mode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoreboardUpdate;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.IJFScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors.Area;
import au.com.jwatmuff.eventmanager.util.FileExtensionFilter;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Rect;
import au.com.jwatmuff.eventmanager.util.gui.ScalableLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXImagePanel;

/**
 *
 * @author  James
 */
public class ScoreboardDisplayPanel extends ScoreboardPanel implements ScoreboardModel.ScoreboardModelListener {
    private static final Logger log = Logger.getLogger(ScoreboardEntryPanel.class);
    
    public static final Score[] SHIDO_TYPES = new Score[] { Score.SHIDO, Score.LEG_SHIDO, Score.HANSAKUMAKE };
    
    static final Color CLEAR = new Color(0,0,0,0);
    
    protected ScoreboardModel model = new ScoreboardModelImpl();
    
    protected ScoreboardLayout scoreboardLayout;
    
    protected final JLayeredPane layeredPane;
    
    protected final JPanel bottomLayer;
    protected final ScalableLabel timer;
    protected final ScalableLabel[] player;
    protected final ScalableLabel[] team;
    protected final ScalableLabel[][] scoreLabels;
    protected final ScalableLabel[][] score;
    protected final ScalableLabel division;
    
    protected final JPanel backgroundLayer;
    protected final ScalableLabel[] playerBackground;
    protected final ScalableLabel timerBackground;

    protected final ScalableLabel[] pendingPlayer;
    protected final ScalableLabel[] pendingFightTimer;
    protected final ScalableLabel pendingDivision;
        
    protected final ScalableLabel[][] shido;
    
    protected final ScalableLabel holddownTimer;
    
    protected final ScalableLabel[][] pendingScores;
    
    protected final JPanel resultLayer;
    protected final ScalableLabel result;
    protected final ScalableLabel goldenScore;
    protected final ScalableLabel goldenScoreApprove;
    
    protected final JPanel noFightLayer;
    protected final JPanel pendingFightLayer;

    protected final ScalableLabel vsPlayer[];
    protected final ScalableLabel vs;
    protected final ScalableLabel vsDivision;
    protected final JPanel vsLayer;
    
    protected boolean swapPlayers;

    protected final int imageDisplayTime = 5000;
    protected File[] imageFiles = findImageFiles();
    protected int lastImageIndex = -1;
    protected final JXImagePanel imageLayer;

    public static ScoreboardDisplayPanel getInstance() {
        return getInstance(new IJFScoreboardLayout());
    }
    
    public static ScoreboardDisplayPanel getInstance(ScoreboardLayout scoreboardLayout) {
        ScoreboardDisplayPanel panel = new ScoreboardDisplayPanel(scoreboardLayout);
        panel.init();
        return panel;
    }

    protected ScoreboardDisplayPanel(ScoreboardLayout scoreboardLayout) {
        this.scoreboardLayout = scoreboardLayout;
        
        ScalableAbsoluteLayout layout;
        
        setLayout(new GridLayout(1,1));
        
        layeredPane = new JLayeredPane();
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
        layout.addComponent(vsDivision, scoreboardLayout.getDivisionRect());

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
        layout.addComponent(pendingDivision, scoreboardLayout.getDivisionRect());
        
        /*
         * Winning result layer
         */
        resultLayer = new JPanel();
        resultLayer.setOpaque(false);
        layeredPane.add(resultLayer, new Integer(9));
        layout = new ScalableAbsoluteLayout(resultLayer, 16, 12);
        resultLayer.setLayout(layout);
        
        result = new ScalableLabel("");
        result.setVisible(false);
        layout.addComponent(result, scoreboardLayout.getResultRect());
        
        goldenScore = new ScalableLabel("Golden Score");
        goldenScore.setVisible(false);
        layout.addComponent(goldenScore, scoreboardLayout.getGoldenScoreRect());
        
        goldenScoreApprove = new ScalableLabel("Golden Score");
        goldenScoreApprove.setVisible(false);
        layout.addComponent(goldenScoreApprove, scoreboardLayout.getGoldenScoreApproveRect());
        
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
                
                layout.addComponent(label, scoreboardLayout.getHolddownScoreRect(j, i));
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
        layout.addComponent(holddownTimer, scoreboardLayout.getHolddownRect());

        /*
         * Bottom layer - background and core elements such as timer, player
         *                names and scores
         */
        bottomLayer = new JPanel();
        layeredPane.add(bottomLayer, new Integer(1));
        layout = new ScalableAbsoluteLayout(bottomLayer, 16, 12);
        bottomLayer.setLayout(layout);
        bottomLayer.setOpaque(false);

        timer = new ScalableLabel("5:00");
        player = new ScalableLabel[] {
            new ScalableLabel("Player 1"),
            new ScalableLabel("Player 2")
        };
        team = new ScalableLabel[] {
            new ScalableLabel("Team A"),
            new ScalableLabel("Team B")
        };
        
        if(scoreboardLayout instanceof IJFScoreboardLayout) {
            player[0].setHorizontalAlignment(JLabel.LEFT);
            player[1].setHorizontalAlignment(JLabel.LEFT);
            team[0].setHorizontalAlignment(JLabel.LEFT);
            team[1].setHorizontalAlignment(JLabel.LEFT);
        }

        layout.addComponent(timer, scoreboardLayout.getTimerRect());
        for(int i = 0; i < 2; i++) {
            layout.addComponent(player[i], scoreboardLayout.getPlayerLabelRect(i));
            layout.addComponent(team[i], scoreboardLayout.getTeamLabelRect(i));
        }
        
        scoreLabels = new ScalableLabel[2][2];
        score = new ScalableLabel[2][2];
        String[] iwyk = new String[]{"I","W"};
        for(int i=0;i<2;i++) {
            for(int j=0;j<2;j++) {
                Score s = Score.values()[j];
                scoreLabels[i][j] = new ScalableLabel(iwyk[j]);
                Rect rect = scoreboardLayout.getScoreLabelRect(i, s);
                if(rect != null) layout.addComponent(scoreLabels[i][j], rect);

                score[i][j] = new ScalableLabel("0");
                layout.addComponent(score[i][j], scoreboardLayout.getScoreRect(i, s));
            }
        }
        
        shido = new ScalableLabel[2][4];
        for(int i=0; i<2; i++) {
            for(int j=0; j<4; j++) {
                shido[i][j] = new ScalableLabel("");
                shido[i][j].setVisible(false);
                // shidos get laid out dynamically in updateShidos
//                layout.addComponent(shido[i][j], scoreboardLayout.getShidoRect(i, index, s, model));
            }
        }
        division = new ScalableLabel(" ");
        layout.addComponent(division, scoreboardLayout.getDivisionRect());
        
        // IJF Scoreboard has no borders on core components
        if(scoreboardLayout instanceof IJFScoreboardLayout) {
            // Remove borders
            for(Component c : bottomLayer.getComponents()) {
                if(c instanceof ScalableLabel) {
                    ScalableLabel l = (ScalableLabel)c;
                    l.setBorder(new EmptyBorder(0,0,0,0));
                }
            }
        }
        
        /*
         * Background layer - purely for background colours as used by IJF
         *                    scoreboard
         */
        backgroundLayer = new JPanel();
        layeredPane.add(backgroundLayer, new Integer(0));
        layout = new ScalableAbsoluteLayout(backgroundLayer, 16, 12);
        backgroundLayer.setLayout(layout);
        backgroundLayer.setOpaque(false);
        
        // Add background last
        playerBackground = new ScalableLabel[2];
        for(int i = 0; i < 2; i++) {
            playerBackground[i] = new ScalableLabel("");
            playerBackground[i].setBorder(new EmptyBorder(0,0,0,0));
            Rect r = scoreboardLayout.getPlayerBackgroundRect(i);
            if(r != null) layout.addComponent(playerBackground[i], r);
        }
        
        {
            Rect r = scoreboardLayout.getTimerBackgroundRect();
            timerBackground = new ScalableLabel("");
            timerBackground.setBorder(new EmptyBorder(0,0,0,0));
            if(r != null) layout.addComponent(timerBackground, r);
        }
    }
    
    protected void init() {
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
        updateImages(false); // X
    }
    
    void updateColors() {
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
                division, vsDivision, pendingDivision}) {
            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
        }
        
        for(ScalableLabel[] labels : shido) {
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
        }
        
        for(int i=0; i<2; i++) {
            int playerPnt = swapPlayers?(1-i):i;
            Color fg = colors.getColor((playerPnt==0)?Area.PLAYER1_FOREGROUND:Area.PLAYER2_FOREGROUND);
            Color bg = colors.getColor((playerPnt==0)?Area.PLAYER1_BACKGROUND:Area.PLAYER2_BACKGROUND);
            
            for(Component c : Arrays.asList(
                    player[i],
                    team[i],
                    playerBackground[i],
                    pendingFightTimer[i],
                    pendingPlayer[i],
                    vsPlayer[playerPnt])) {
                c.setForeground(fg);
                c.setBackground(bg);
            }

            for(int j=0; j<2; j++) {
                score[i][j].setForeground(fg);
                score[i][j].setBackground(bg);
                pendingScores[i][j].setForeground(fg);
                pendingScores[i][j].setBackground(bg);
            }
        }
        
        for(int i=0; i<2; i++) {
            for(int j=0; j<2; j++) {
                scoreLabels[i][j].setBackground(mainBg);
                scoreLabels[i][j].setForeground(mainFg);
                scoreLabels[i][j].setBorder(new EmptyBorder(0,0,0,0));
            }
        }
        
        // Special colours for IJF
        if(this.scoreboardLayout instanceof IJFScoreboardLayout) {
            clearBackground(timer);
            timerBackground.setBackground(Color.BLACK);
            timer.setForeground(model.getMode() == Mode.FIGHTING ? Color.GREEN : Color.RED);
            for(ScalableLabel[] ls : score) {
                for(ScalableLabel l : ls) {
                    clearBackground(l);
                }
            }
            for(int i = 0; i < 2; i++) {
                clearBackground(player[i], team[i]);
            }
        }
    }
    
    static void clearBackground(JLabel... labels) {
        for(JLabel label : labels) {
            label.setBackground(CLEAR);
            label.setOpaque(false);
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
            for(int j=0; j<2; j++)
                score[i][j].setText(String.valueOf(model.getScore(swapPlayers?1-i:i, Score.values()[j])));
    }
    
    void updateShido() {
        ScalableAbsoluteLayout layout = (ScalableAbsoluteLayout)bottomLayer.getLayout();

        for(int i=0; i<2; i++) {
            int j = 0;
            for(Score s : SHIDO_TYPES) {
                for(int k = 0; k < model.getScore(swapPlayers?1-i:i, s) && j < 4; k++, j++) {
                    log.info(i + ", " + j + ", " + k);
                    ScalableLabel label = shido[i][j];
                    label.setText("" + s.initial);
                    // It's OK to add something twice
                    layout.addComponent(label, scoreboardLayout.getShidoRect(i, k, j, s, model));
                    label.setVisible(true);
                }
            }
            // Hide remaining shidos
            while(j < 4) {
                shido[i][j++].setVisible(false);
            }
        }
    }
    
    void showHolddownTimer(boolean b) {
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
    
    void updateResult() {
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
                String displayed;
                if(model.isHansakumake(1 - model.getWinningPlayer())) {
                    displayed = "H";
                } else {
                    displayed = model.getWin().score.toString().substring(0, 1);
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
    
    String formatPlayerName(int player) {
        String name = model.getPlayerName(player);
        if(scoreboardLayout instanceof IJFScoreboardLayout && name != null) {
            name = name.toUpperCase();
        }
        return name;
    }
    
    void updatePlayers() {
        for(int i=0; i<2; i++) {
            player[i].setText(formatPlayerName(swapPlayers?1-i:i));
            String teamText = model.getTeamName(swapPlayers?1-i:i);
            if(teamText == null) {
                teamText = "";
            }
            team[i].setText(teamText);
            team[i].setVisible(model.showTeams() && !teamText.isEmpty());
        }
    }
    
    void updateDivision() {
        String divisionName = model.getDivisionName();
        boolean showDivision = !StringUtils.isEmpty(divisionName);
        for(ScalableLabel label : new ScalableLabel[] { division, vsDivision, pendingDivision} ) {
            label.setVisible(showDivision);
            label.setText(showDivision ? divisionName : " ");
        }
    }
    
    void updateGoldenScore() {
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
                updateImages(true); // X
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

    void updatePendingFight() {
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
        
//        log.info("handleScoreboardUpdate");
        
        // Force updates onto the GUI thread
        if(!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handleScoreboardUpdate(update, model));
            return;
        }

        updateColors();
        
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
                updateShido();
                updateResult();
                break;
            case PENDING_SCORE:
                updatePendingScores();
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
                updateDivision();
                break;
            case ALL:
                updateTimer();
                updateHolddownTimer();
                updatePlayers();
                updateDivision();
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
        this.repaint(); // Hoping this will fix intermittent delay issue - very hard to test
        // delay seems to be due to render issue because resizing window brings it into sync
        // only seems to affect entry scoreboard
        // maybe something else is updating gui off the main thread?
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
