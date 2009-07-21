/*
 * CompetitionInterfacesPanel.java
 *
 * Created on 5 September 2008, 14:17
 */

package au.com.jwatmuff.eventmanager.gui.scoring;

import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoringSystem;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardWindow;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.ListDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.awt.Frame;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class CompetitionInterfacesPanel extends javax.swing.JPanel {
    private static final Logger log = Logger.getLogger(CompetitionInterfacesPanel.class);

    private Database database;
    private TransactionNotifier notifier;
    private PeerManager peerManager;
    private Frame parentWindow;
    
    /** Creates new form CompetitionInterfacesPanel */
    public CompetitionInterfacesPanel() {
        initComponents();
    }
    
    public void setPeerManager(PeerManager peerManager) {
        this.peerManager = peerManager;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
    
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    public void afterPropertiesSet() {
    }
    
    public Session chooseMat(String title) {
        List<Session> mats = database.findAll(Session.class, SessionDAO.ALL_MATS);
        if(mats.size() == 0) {
            GUIUtils.displayMessage(null, "At least one contest area must be defined before competition interfaces can be used", title);
            return null;
        }
        ListDialog<Session> dialog = new ListDialog<Session>(parentWindow, true, mats, "Choose Contest Area", title);
        dialog.setRenderer(new StringRenderer<Session>() {
                @Override
                public String asString(Session o) {
                    return o.getMat();
                }
        }, Icons.CONTEST_AREA);
        dialog.setVisible(true);
        if(!dialog.getSuccess()) return null;
        return dialog.getSelectedItem();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fightProgressionButton = new javax.swing.JButton();
        scoringButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scoreboardButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        displayScoreboardButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        manualScoreboardButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();

        fightProgressionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/text_list_numbers.png"))); // NOI18N
        fightProgressionButton.setText("Show Fight Progression..");
        fightProgressionButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        fightProgressionButton.setIconTextGap(8);
        fightProgressionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fightProgressionButtonActionPerformed(evt);
            }
        });

        scoringButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/mouse.png"))); // NOI18N
        scoringButton.setText("Enter Scores (Winner By)..");
        scoringButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        scoringButton.setIconTextGap(8);
        scoringButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoringButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setText("Competition Interfaces");

        jLabel2.setText("Enter winning scores for fights as they are played");

        jLabel3.setText("Score fights using a fully featured scoreboard interface");

        scoreboardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/application_view_tile.png"))); // NOI18N
        scoreboardButton.setText("Scoreboard (Entry Mode)..");
        scoreboardButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        scoreboardButton.setIconTextGap(8);
        scoreboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoreboardButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("Display a list of upcoming fights for each contest area");

        displayScoreboardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/application_view_tile.png"))); // NOI18N
        displayScoreboardButton.setText("Scoreboard (Display Mode)..");
        displayScoreboardButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        displayScoreboardButton.setIconTextGap(8);
        displayScoreboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayScoreboardButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Display a non-interactive scoreboard by connecting to another scoreboard");

        manualScoreboardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/application_view_tile.png"))); // NOI18N
        manualScoreboardButton.setText("Manual Scoreboard..");
        manualScoreboardButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        manualScoreboardButton.setIconTextGap(8);
        manualScoreboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualScoreboardButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("Start a manual scoreboard (results will not be automatically recorded)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(scoringButton)
                    .addComponent(scoreboardButton)
                    .addComponent(jLabel4)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(displayScoreboardButton)
                    .addComponent(jLabel5)
                    .addComponent(jSeparator5, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(fightProgressionButton)
                    .addComponent(manualScoreboardButton)
                    .addComponent(jLabel6))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {displayScoreboardButton, fightProgressionButton, manualScoreboardButton, scoreboardButton, scoringButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scoringButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scoreboardButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(fightProgressionButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(displayScoreboardButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(manualScoreboardButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addContainerGap(41, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

private void fightProgressionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fightProgressionButtonActionPerformed
    Session mat = chooseMat("Fight Progression");
    if(mat == null) return;
    new FightProgressionWindow(database, notifier, mat).setVisible(true);
}//GEN-LAST:event_fightProgressionButtonActionPerformed

private void scoringButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoringButtonActionPerformed
    if(!PermissionChecker.isAllowed(Action.SCOREBOARD_ENTRY, database)) return;
    Session mat = chooseMat("Score Entry");
    if(mat == null) return;
    new SimpleScoringWindow(database, notifier, mat).setVisible(true);
}//GEN-LAST:event_scoringButtonActionPerformed

private void scoreboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoreboardButtonActionPerformed
    Session mat = chooseMat("Scoreboard");
    if(mat == null) return;
    String serviceName = "scoreboard" + mat.getID();
    if(peerManager.isRegistered(serviceName))
        GUIUtils.displayError(this.parentWindow, "A scoreboard is already open for this mat.");
    else
        new ScoreboardWindow(database, notifier, mat, peerManager, serviceName, ScoringSystem.NEW).setVisible(true);
}//GEN-LAST:event_scoreboardButtonActionPerformed

private void displayScoreboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayScoreboardButtonActionPerformed
    Session mat = chooseMat("Scoreboard");
    if(mat == null) return;
    new ScoreboardWindow(mat, peerManager, ScoringSystem.NEW).setVisible(true);
    /*
    List<ScoreboardModel> scoreboards = new ArrayList<ScoreboardModel>();
    List<String> names = new ArrayList<String>();
    for(Peer peer : peerManager.getPeers()) {
        try {
            for(String name : peer.getService(LookupService.class).getServices(ScoreboardModel.class)) {
                names.add(name + " - " + peer.getName());
                scoreboards.add(peer.getService(name, ScoreboardModel.class));
            }
        } catch(Exception e) {
            log.error("Exception while getting scoreboard info", e);
        }
    }
    
    if(names.size() == 0) {
        GUIUtils.displayMessage(parentWindow, "No scoreboards found. An entry-mode scoreboard must be created first.", "Scoreboard");
        return;
    }
    
    ListDialog<String> cbd = new ListDialog<String>(parentWindow, true, names, "Choose a scoreboard to connect to", "Scoreboard");
    cbd.setRenderer(new StringRenderer<String>() {
            @Override
            public String asString(String o) {
                return o;
            }
    }, Icons.SCOREBOARD);

    cbd.setVisible(true);
    if(!cbd.getSuccess()) return;
    
    String name = cbd.getSelectedItem();
    new ScoreboardWindow(scoreboards.get(names.indexOf(name)), name).setVisible(true);
   */
}//GEN-LAST:event_displayScoreboardButtonActionPerformed

private void manualScoreboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualScoreboardButtonActionPerformed
    if(!PermissionChecker.isAllowed(Action.MANUAL_SCOREBOARD, database)) return;
    new ScoreboardWindow("Manual Scoreboard", ScoringSystem.NEW).setVisible(true);
}//GEN-LAST:event_manualScoreboardButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton displayScoreboardButton;
    private javax.swing.JButton fightProgressionButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JButton manualScoreboardButton;
    private javax.swing.JButton scoreboardButton;
    private javax.swing.JButton scoringButton;
    // End of variables declaration//GEN-END:variables

}
