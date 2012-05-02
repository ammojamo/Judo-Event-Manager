/*
 * ScoreboardWindow.java
 *
 * Created on 10 November 2008, 14:18
 */

package au.com.jwatmuff.eventmanager.gui.scoreboard;
import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.GoldenScoreMode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Mode;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoreboardModelListener;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoreboardUpdate;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoringSystem;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModelWrapper.ShutdownHandler;
import au.com.jwatmuff.eventmanager.gui.scoring.ManualFightDialog;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColorsDialog;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.misc.*;
import au.com.jwatmuff.eventmanager.model.vo.*;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.ListDialog;
import au.com.jwatmuff.eventmanager.util.gui.PanelDisplayFrame;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericp2p.NoSuchServiceException;
import au.com.jwatmuff.genericp2p.Peer;
import au.com.jwatmuff.genericp2p.PeerManager;
import au.com.jwatmuff.genericp2p.rmi.LookupService;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class ScoreboardWindow extends javax.swing.JFrame {
    private static final Logger log = Logger.getLogger(ScoreboardWindow.class);

    private ScoreboardDisplayPanel scoreboard;
    private ScoreboardDisplayPanel fullscreen;
    
    private Database database;
    private TransactionNotifier notifier;
    private Session mat;
    private Fight currentFight;
    private FightPlayer[] currentPlayers = new FightPlayer[2];
    
    private ScoreboardModel model;
    
    private Clip siren;
    
    private ScoringColors colors = new ScoringColors();
    
    private String serviceName;
    private PeerManager peerManager;
    
    private boolean interactive;
    private KeyListener keyListener;
    
    private String title;

    private boolean findThreadRunning;

    /* searches all connected peers for a scoreboard corresponding to current mat */
    private Runnable findPeerScoreboard = new Runnable() {
        @Override
        public void run() {
            findThreadRunning = true;
            while(findThreadRunning) {
                log.debug("Looking for scoreboard");
                for(Peer peer : peerManager.getPeers()) {
                    try {
                        for(String name : peer.getService(LookupService.class).getServices(ScoreboardModel.class)) {
                            if(name.matches("scoreboard" + mat.getID())) {
                                log.debug("Found scoreboard on " + peer);
                                setModel(peer.getService(name, ScoreboardModel.class), true);
                                findThreadRunning = false;
                                return;
                            }
                        }
                    } catch(Exception e) {
                        log.error("Exception while looking for scoreboard for mat " + mat.getMat(), e);
                    }
                }

                /* wait for 0.5 seconds */
                try { Thread.sleep(500); } catch (InterruptedException e) { }
            }
        }
    };

    private void setModel(ScoreboardModel model, boolean wrap) {
        assert(model != null);

        if(wrap) {
            ScoreboardModelWrapper wrapper = new ScoreboardModelWrapper(model);
            wrapper.setShutdownHandler(new ShutdownHandler() {
                @Override
                public void handleShutdown() {
                    new Thread(findPeerScoreboard,"findPeerScoreboard").start();
                }
            });
            model = wrapper;
        }

        this.model = model;
        scoreboard.setModel(model);
        fullscreen.setModel(model);

        model.addListener(new ScoreboardModelListener() {
            @Override
            public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
                if(update == ScoreboardUpdate.SIREN)
                    playSiren();
            }
        });
    }

    /*
     * DISPLAY the scoreboard over the network for which a scoreboard model
     * is already obtained. Wraps the given scoreboard model
     */
    private ScoreboardWindow(String title, ScoreboardModel model) {
        interactive = false;
        initComponents();
        setupMenu();
        this.title = title;
        setTitle(title);
        scoreboard = new ScoreboardPanel(interactive);
        fullscreen = new ScoreboardPanel(interactive);
        setModel(new ScoreboardModelWrapper(model), false);

        getContentPane().setLayout(new GridLayout(1,1));
        getContentPane().add(scoreboard);
        openSirenFile(new File("resources/sound/siren.wav"));
        this.setIconImage(Icons.SCOREBOARD.getImage());
        setTimeMenuItem.setEnabled(false);
    }

    /* creates a window to DISPLAY the scoreboard for the given mat */
    public ScoreboardWindow(Session mat, PeerManager peerManager, ScoringSystem system) {
        interactive = false;

        initComponents();
        setupMenu();
        
        title = "Event Manager - Scoreboard Display - [" + mat.getMat() + "]";
        setTitle(title);

        /* create scoreboard panels */
        scoreboard = new ScoreboardPanel(interactive);
        fullscreen = new ScoreboardPanel(interactive);
        
        /* put scoreboard panel into window */
        getContentPane().setLayout(new GridLayout(1,1));
        getContentPane().add(scoreboard);

        assert(mat != null);
        assert(peerManager != null);

        this.mat = mat;
        this.peerManager = peerManager;

        openSirenFile(new File("resources/sound/siren.wav"));
        this.setIconImage(Icons.SCOREBOARD.getImage());
        setTimeMenuItem.setEnabled(false);

        /* start thread to look for remote scoreboard to connect to */
        new Thread(findPeerScoreboard, "findPeerScoreboard").start();
    }

    private ScoreboardWindow(ScoringSystem system) {
        interactive = true;

        initComponents();
        setupMenu();

        optionsMenu.remove(showImagesMenuItem);

        /* create scoreboard panels (one for windowed, one for fullscreen) */
        scoreboard = new ScoreboardPanel(true, system);
        fullscreen = new ScoreboardPanel(true, system);

        /* add the scoreboard panel to the window */
        getContentPane().setLayout(new GridLayout(1,1));
        getContentPane().add(scoreboard);

        /* the fullscreen panel uses the same model as the normal scoreboard
         * panel */
        model = scoreboard.getModel();
        fullscreen.setModel(model);

        /* open default siren file */
        openSirenFile(new File("resources/sound/siren.wav"));

        /* register listener to play siren */
        model.addListener(new ScoreboardModelListener() {
            @Override
            public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
                if(update.equals(ScoreboardUpdate.SIREN)) playSiren();
            }
        });

        /* set up key stroke actions */
        keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                switch(evt.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        model.toggleTimer();
                        break;
                    case KeyEvent.VK_ENTER:
                        if(!model.isHolddownActivated())
                            model.startHolddownTimer();
                        else
                            model.cancelHolddownTimer();
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                        if(model.undoCancelHolddownAvailable())
                            model.undoCancelHolddown();
                        break;
                    default:
                        //do nothing
                }
            }
        };
        this.addKeyListener(keyListener);
    }

    public ScoreboardWindow(String title, ScoringSystem system, PeerManager peerManager, String serviceName) {
        this(title, system);
        this.peerManager = peerManager;
        this.serviceName = serviceName;
        peerManager.registerService(serviceName, ScoreboardModel.class, model);
    }

    public ScoreboardWindow(String title, ScoringSystem system) {
        this(system);

        setTitle(title);

        /* disable full screen because the 'are you sure?' end of fight dialog
         * and decision dialog can't be seen in full screen mode */
        fullScreenMenuItem.setEnabled(false);

        model.addListener(new ScoreboardModelListener() {
            @Override
            public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
                /* if mode has just changed to NO_FIGHT then a fight has ended */
                if(update.equals(ScoreboardUpdate.MODE) &&
                   (model.getMode() == Mode.NO_FIGHT)) {
                    ManualFightDialog mfd = new ManualFightDialog(ScoreboardWindow.this, true);
                    mfd.setVisible(true);
                    model.reset(mfd.getFightTime(), mfd.getGoldenScoreTime(), new String[] { mfd.getPlayerName1(), mfd.getPlayerName2()});
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ManualFightDialog mfd = new ManualFightDialog(ScoreboardWindow.this, true);
                mfd.setVisible(true);
                model.reset(mfd.getFightTime(), mfd.getGoldenScoreTime(), new String[] { mfd.getPlayerName1(), mfd.getPlayerName2()});
            }
        });
    }
    
    /** Creates new ScoreboardWindow which gets fights from the given database
      for the given mat. */
    public ScoreboardWindow(Database database, TransactionNotifier notifier, Session mat, PeerManager peerManager, String serviceName, ScoringSystem system) {
        this(system);
        interactive = true;
        this.mat = mat;
        this.database = database;
        this.notifier = notifier;
        this.serviceName = serviceName;

        /* set title and window icon */
        title = "Event Manager - Scoreboard Entry - [" + mat.getMat() + "]";
        setTitle(title);
        this.setIconImage(Icons.SCOREBOARD.getImage());

        /* set up listener to record results and show next fight */
        model.addListener(new ScoreboardModelListener() {
            @Override
            public void handleScoreboardUpdate(ScoreboardUpdate update, ScoreboardModel model) {
                /* if mode has just changed to NO_FIGHT then a fight has ended */
                if(update.equals(ScoreboardUpdate.MODE) &&
                   (model.getMode() == Mode.NO_FIGHT)) {
                    if(currentFight != null) {
                        /* record the fight result in the database */
                        Result r = new Result();
                        r.setFightID(currentFight.getID());

                        int[] playerIDs = new int[2];
                        for(int i=0; i<2; i++)
                            playerIDs[i] = (currentPlayers[i] != null && currentPlayers[i].player != null) ?  currentPlayers[i].player.getID() : -1;
                        r.setPlayerIDs(playerIDs);

                        boolean goldenScore = false;
                        switch(model.getGoldenScoreMode()) {
                            case ACTIVE:
                            case FINISHED:
                                goldenScore = true;
                        }

                        FullScore[] scores = new FullScore[2];
                        for(int i=0; i<2; i++) {
                            FullScore score = new FullScore();
                            score.setIppon(model.getScore(i, ScoreboardModel.Score.IPPON));
                            score.setWazari(model.getScore(i, ScoreboardModel.Score.WAZARI));
                            score.setYuko(model.getScore(i, ScoreboardModel.Score.YUKO));
                            score.setShido(model.getShido(i));
                            if(goldenScore && model.getWinningPlayer() == i) score.setDecision(1);
                            scores[i] = score;
                        }
                        
                        // Work out fight duration
                        Pool pool = ScoreboardWindow.this.database.get(Pool.class, currentFight.getPoolID());
                        int fightDuration = pool.getMatchTime() - model.getTime();
                        if(goldenScore) fightDuration += pool.getGoldenScoreTime();
                        r.setDuration(fightDuration);

                        //model.getTime()
                        r.setScores(scores);
                        r.setEventLog(model.getEventLog());
                        ResultRecorder.recordResult(ScoreboardWindow.this.database, r);
                    }
                    updateFightFromDatabase();
                }
            }
        });

        /* register service so peers on the network can connect to this scoreboard */
        this.peerManager = peerManager;
        peerManager.registerService(serviceName, ScoreboardModel.class, model);

        model.endFight();
        updateFightFromDatabase();

        /* add database listener to update the scoreboard if no fight is being
         * scored and a new fight is created in the database */
        notifier.addListener(new TransactionListener() {
            @Override
            public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
                if(model.getMode() == Mode.NO_FIGHT)
                    updateFightFromDatabase();
            }
        }, Result.class, Fight.class, SessionFight.class, Session.class);
    }
    
    private void openSirenFile(File sirenFile) {
        Clip oldSiren = siren;

        try {
            siren = AudioSystem.getClip();
            siren.open(AudioSystem.getAudioInputStream(sirenFile));
            testSirenMenuItem.setText("Test Siren [" + sirenFile.getName() + "]");
        } catch(Exception e) {
            log.error(e);
            GUIUtils.displayError(this, "Unable to open siren sound file");
            siren = oldSiren;
        }
    }
    
    private void playSiren() {
        if(siren != null) {
            siren.setFramePosition(0);
            siren.start();
        }
    }
    
    private void setupMenu() {
        scoreboardStyleMenu.setVisible(!interactive);
        for(final ScoreboardDisplayType type : ScoreboardDisplayType.values()) {
            JMenuItem menuItem = new JMenuItem();
            menuItem.setText(type.description);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    updateStyle(type);
                }
            });
            scoreboardStyleMenu.add(menuItem);
        }
    }
    
    private void updateStyle(ScoreboardDisplayType type) {
        // set models to null to de-register model listeners
        scoreboard.setModel(null);
        fullscreen.setModel(null);
        // remove from GUI
        getContentPane().remove(scoreboard);
        
        // create new panels
        scoreboard = ScoreboardDisplayType.getPanel(type);
        fullscreen = ScoreboardDisplayType.getPanel(type);
        scoreboard.setModel(model);
        fullscreen.setModel(model);
        
        // add to GUI
        getContentPane().add(scoreboard);
        
        // apply settings
        if(swapPlayersMenuItem.isSelected()) {
            scoreboard.swapPlayers();
            fullscreen.swapPlayers();
        }
        scoreboard.setImagesEnabled(showImagesMenuItem.isSelected());
        fullscreen.setImagesEnabled(showImagesMenuItem.isSelected());
    }
    
    private void updateFightFromDatabase() {
        if(mat == null) return;

        /* attempt to get the next upcoming fight */
        Collection<Fight> fights;
        try {
            fights = UpcomingFightFinder.findUpcomingFights(database, mat.getID(), 1);
            if(fights.size() > 0) {
                Fight nextFight = fights.iterator().next();
                for(int i=0; i<2; i++) {
                    FightPlayer nextPlayer = PlayerCodeParser.parseCode(database, nextFight.getPlayerCodes()[i], nextFight.getPoolID());
                    if(nextPlayer.type == PlayerType.UNDECIDED){
                        return;
                    }
                }
            }
        } catch (DatabaseStateException e) {
            log.error("Unable to get upcoming fights for mat " + mat.getMat(), e);
            return;
        }

        /* if we have a fight, update the scoreboard to score the new fight */
        if(fights.size() > 0) {
            currentFight = fights.iterator().next();

            String[] playerNames = new String[2];
            Date[] lastFightTimes = new Date[2];
            for(int i=0; i<2; i++) {
                try {
                    currentPlayers[i] = PlayerCodeParser.parseCode(database, currentFight.getPlayerCodes()[i], currentFight.getPoolID());
                    playerNames[i] = currentPlayers[i].toString();
                    if(currentPlayers[i].player != null)
                        lastFightTimes[i] = PlayerTimer.lastFightTime(currentPlayers[i].player.getID(), database);
                } catch(DatabaseStateException e) {
                    currentPlayers[i] = null;
                    playerNames[i] = currentFight.getPlayerCodes()[i];
                }
            }

            Pool pool = database.get(Pool.class, currentFight.getPoolID());

            model.reset(pool.getMatchTime(), pool.getGoldenScoreTime(), playerNames, lastFightTimes, pool.getMinimumBreakTime());

            SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, currentFight.getID());
            int fightNumber = SessionFightSequencer.getFightMatInfo(database, sf).fightNumber;
            setTitle(title + " - Fight " + fightNumber);
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

        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        closeMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        fullScreenMenuItem = new javax.swing.JMenuItem();
        swapPlayersMenuItem = new javax.swing.JCheckBoxMenuItem();
        chooseColorsMenuItem = new javax.swing.JMenuItem();
        showImagesMenuItem = new javax.swing.JCheckBoxMenuItem();
        scoreboardStyleMenu = new javax.swing.JMenu();
        jSeparator3 = new javax.swing.JSeparator();
        chooseSirenMenuItem = new javax.swing.JMenuItem();
        testSirenMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        setTimeMenuItem = new javax.swing.JMenuItem();

        jSeparator1.setForeground(new java.awt.Color(204, 204, 204));

        jSeparator2.setForeground(new java.awt.Color(204, 204, 204));

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        fileMenu.setText("File");

        closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        closeMenuItem.setText("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(closeMenuItem);

        jMenuBar1.add(fileMenu);

        optionsMenu.setText("Options");

        fullScreenMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.ALT_MASK));
        fullScreenMenuItem.setText("Fullscreen");
        fullScreenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullScreenMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(fullScreenMenuItem);
        optionsMenu.remove(fullScreenMenuItem);

        swapPlayersMenuItem.setText("Swap Player Sides");
        swapPlayersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                swapPlayersMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(swapPlayersMenuItem);

        chooseColorsMenuItem.setText("Choose Colors..");
        chooseColorsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseColorsMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(chooseColorsMenuItem);

        showImagesMenuItem.setSelected(true);
        showImagesMenuItem.setText("Show Advertising");
        showImagesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showImagesMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(showImagesMenuItem);

        scoreboardStyleMenu.setText("Scoreboard Style");
        optionsMenu.add(scoreboardStyleMenu);

        jSeparator3.setForeground(new java.awt.Color(204, 204, 204));
        optionsMenu.add(jSeparator3);

        chooseSirenMenuItem.setText("Choose Siren Sound..");
        chooseSirenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseSirenMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(chooseSirenMenuItem);

        testSirenMenuItem.setText("Test Siren");
        testSirenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testSirenMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(testSirenMenuItem);

        jSeparator4.setForeground(new java.awt.Color(204, 204, 204));
        optionsMenu.add(jSeparator4);

        setTimeMenuItem.setText("Set Time Manually..");
        setTimeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTimeMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(setTimeMenuItem);

        jMenuBar1.add(optionsMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 279, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        this.dispose();
    }//GEN-LAST:event_closeMenuItemActionPerformed

    private void fullScreenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullScreenMenuItemActionPerformed
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        if(gd.isFullScreenSupported()) {
            final ScoreboardDisplayPanel sp = fullscreen;
            final Frame w = new PanelDisplayFrame(sp);
            w.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent evt) {
                    if(evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        w.removeNotify();
                        w.setUndecorated(false);
                        w.addNotify();
                        gd.setFullScreenWindow(null);
                        w.dispose();
                    }
                }
            });
            if(interactive) {
                w.addKeyListener(keyListener);
            }
    
            w.removeNotify();
            w.setUndecorated(true);
            w.addNotify();
            gd.setFullScreenWindow(w);
        }
        else
            GUIUtils.displayMessage(this, "Full Screen Not Supported", "Full Screen Not Supported");
    }//GEN-LAST:event_fullScreenMenuItemActionPerformed

private void chooseSirenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseSirenMenuItemActionPerformed
    JFileChooser fileChooser = new JFileChooser();
    if(GUIUtils.lastSirenChooserDirectory != null)
        fileChooser.setCurrentDirectory(GUIUtils.lastSirenChooserDirectory);
    fileChooser.setFileFilter(new FileFilter() {
            String[] endings = new String[]{".wav", ".aiff", ".au"};
        
            @Override
            public boolean accept(File f) {
                if(f.isDirectory()) return true;
                for(String ending : endings)
                    if(f.getName().toLowerCase().endsWith(ending)) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "Sound Files (*.wav, *.aiff, *.au)";
            }
    });
    fileChooser.setMultiSelectionEnabled(false);
    int result = fileChooser.showOpenDialog(this);
    if(result == JFileChooser.APPROVE_OPTION) {
        openSirenFile(fileChooser.getSelectedFile());
    }       
}//GEN-LAST:event_chooseSirenMenuItemActionPerformed

private void testSirenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testSirenMenuItemActionPerformed
    playSiren();
}//GEN-LAST:event_testSirenMenuItemActionPerformed

private void chooseColorsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseColorsMenuItemActionPerformed
    ScoringColorsDialog scd = new ScoringColorsDialog(this, true);
    scd.setColors(colors);
    scd.setVisible(true);
    colors = scd.getColors();
    model.setColors(colors);
}//GEN-LAST:event_chooseColorsMenuItemActionPerformed

private void swapPlayersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_swapPlayersMenuItemActionPerformed
    scoreboard.swapPlayers();
    fullscreen.swapPlayers();
}//GEN-LAST:event_swapPlayersMenuItemActionPerformed

private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
    if(serviceName != null)
        peerManager.unregisterService(serviceName);
    // signal find peer scoreboard thread to stop if running
    findThreadRunning = false;
    if(model != null)
        model.shutdown();
}//GEN-LAST:event_formWindowClosed

private void setTimeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTimeMenuItemActionPerformed
    if(!interactive) return;
    model.stopTimer();
    SetTimeDialog dialog = new SetTimeDialog(this, true);
    // if scores are equal, enable golden score tick box
    boolean scoresEqual = true;
    for(ScoreboardModel.Score score : ScoreboardModel.Score.values()) {
        if(model.getScore(0, score) != model.getScore(1, score)) {
            scoresEqual = false;
            break;
        }
    }
    dialog.setGoldenScoreEnabled(scoresEqual);
    dialog.setGoldenScore(
            model.getGoldenScoreMode() == GoldenScoreMode.ACTIVE ||
            model.getGoldenScoreMode() == GoldenScoreMode.FINISHED);
    dialog.setVisible(true);
    if(dialog.isMainTimeSet()) {
        model.setTimer(dialog.getMainTime());
    }
    if(dialog.isHolddownTimeSet()) {
        model.setHolddownTimer(dialog.getHolddownTime());
    }
    if(scoresEqual) {
        model.setGoldenScoreMode(dialog.getGoldenScore() ? GoldenScoreMode.ACTIVE : GoldenScoreMode.INACTIVE);
    }
}//GEN-LAST:event_setTimeMenuItemActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    int status = JOptionPane.showConfirmDialog(this, "Are you sure you wish to close scoreboard?", "Close Scoreboard", JOptionPane.YES_NO_OPTION);
    if(status == JOptionPane.OK_OPTION) {
        this.dispose();
    }
}//GEN-LAST:event_formWindowClosing

private void showImagesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showImagesMenuItemActionPerformed
    scoreboard.setImagesEnabled(showImagesMenuItem.isSelected());
    fullscreen.setImagesEnabled(showImagesMenuItem.isSelected());
}//GEN-LAST:event_showImagesMenuItemActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem chooseColorsMenuItem;
    private javax.swing.JMenuItem chooseSirenMenuItem;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem fullScreenMenuItem;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JMenu scoreboardStyleMenu;
    private javax.swing.JMenuItem setTimeMenuItem;
    private javax.swing.JCheckBoxMenuItem showImagesMenuItem;
    private javax.swing.JCheckBoxMenuItem swapPlayersMenuItem;
    private javax.swing.JMenuItem testSirenMenuItem;
    // End of variables declaration//GEN-END:variables

    public static void selectAndDisplayRemoteScoreboard(Component parent, PeerManager peerManager) {
        selectAndDisplayRemoteScoreboard(parent, peerManager, null);
    }
    
    public static void selectAndDisplayRemoteScoreboard(Component parent, PeerManager peerManager,
                                                        Frame parentWindow) {
        Map<String, Peer> scoreboardNames = new HashMap<String, Peer>();

        for(Peer peer : peerManager.getPeers()) {
            try {
                LookupService lookup = peer.getService(LookupService.class);
                Collection<String> scoreboards = lookup.getServices(ScoreboardModel.class);
                for(String scoreboard : scoreboards)
                    scoreboardNames.put(scoreboard, peer);
            } catch(NoSuchServiceException e) {
                log.warn("Could not find lookup service for peer " + peer);
            }
        }

        if(scoreboardNames.isEmpty()) {
            GUIUtils.displayMessage(parent, "Could not find any scoreboards to display.\nAn entry scoreboard must be opened first.", "Display Scoreboard");
            return;
        }

        ListDialog<String> dialog = new ListDialog<String>(null, true, new ArrayList<String>(scoreboardNames.keySet()), "Select a scoreboard", "Scoreboard Display");
        dialog.setRenderer(new StringRenderer<String>() {
            public String asString(String o) {
                return o.toString();
            }
        }, Icons.SCOREBOARD);
        dialog.setVisible(true);
        if(!dialog.getSuccess()) return;

        try {
            String scoreboard = dialog.getSelectedItem();
            Peer peer = scoreboardNames.get(scoreboard);
            ScoreboardModel model = peer.getService(scoreboard, ScoreboardModel.class);
            final ScoreboardWindow win =
                    new ScoreboardWindow("Manual Scoreboard Display - " + scoreboard, model);
            if(parentWindow != null) {
                parentWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        win.dispose();
                    }
                });
            }
            win.setVisible(true);
        } catch(Exception e) {
            GUIUtils.displayError(parent, "An error occurred while connecting to the requested scoreboard");
            log.error(e.getMessage(), e);
        }
    }
}
