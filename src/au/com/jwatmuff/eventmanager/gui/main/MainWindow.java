/*
 * MainWindow.java
 *
 * Created on 25 February 2008, 18:14
 */

package au.com.jwatmuff.eventmanager.gui.main;

import au.com.jwatmuff.eventmanager.gui.admin.AdministrationPanel;
import au.com.jwatmuff.eventmanager.gui.fightorder.FightOrderPanel;
import au.com.jwatmuff.eventmanager.gui.player.ManagePlayersPanel;
import au.com.jwatmuff.eventmanager.gui.player.WeighInDialog;
import au.com.jwatmuff.eventmanager.gui.pool.ManagePoolsPanel;
import au.com.jwatmuff.eventmanager.gui.scoring.CompetitionInterfacesPanel;
import au.com.jwatmuff.eventmanager.gui.results.ResultsPanel;
import au.com.jwatmuff.eventmanager.gui.session.ManageSessionsPanel;
import au.com.jwatmuff.eventmanager.gui.session.SessionFightsPanel;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericp2p.ManualDiscoveryService;
import au.com.jwatmuff.genericp2p.PeerManager;
import au.com.jwatmuff.genericp2p.windows.WindowsNetUtil;
import java.awt.Cursor;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Collection;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class MainWindow extends javax.swing.JFrame {
    private static Logger log = Logger.getLogger(MainWindow.class);

    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private LicenseManager licenseManager;
    
    private ManagePlayersPanel managePlayersPanel;
    private ManagePoolsPanel managePoolsPanel;
    private AdministrationPanel administrationPanel;
    private FightOrderPanel fightOrderPanel;
    private ManageSessionsPanel manageSessionsPanel;
    private SessionFightsPanel sessionFightsPanel;
    private CompetitionInterfacesPanel competitionInterfacesPanel;
    private ResultsPanel resultsPanel;
    private ManualDiscoveryService manualDiscoveryService;
    
    private PeerManager peerManager;
    
    private boolean deleteOnExit = false;

    // Listens to changes in the competition master password
    private TransactionListener competitionInfoListener = new TransactionListener() {
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            if(dataClasses.contains(CompetitionInfo.class))
                updateMasterPasswordLock();
        }
    };
    
    /** Creates new form MainWindow */
    public MainWindow() {
        initComponents();
        setIconImage(Icons.MAIN_WINDOW.getImage());
    }
    
    @Override
    public void setVisible(boolean b) {
        if(b) this.setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        super.setVisible(b);
    }
    
    @Required
    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }
    
    @Required
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }

    @Required
    public void setLicenseManager(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }
    
    @Required
    public void setPeerManager(PeerManager peerManager) {
        this.peerManager = peerManager;
    }
    
    public void setManualDiscoveryService(ManualDiscoveryService manualDiscoveryService) {
        this.manualDiscoveryService = manualDiscoveryService;
    }
    
    private void createPanels() {
        resultsPanel = new ResultsPanel();
        resultsPanel.setParentWindow(this);
        resultsPanel.setDatabase(database);
        resultsPanel.setNotifier(notifier);
        resultsPanel.afterPropertiesSet();

        mainTabbedPane.insertTab("Results", null, resultsPanel, null, 1);
        
        competitionInterfacesPanel = new CompetitionInterfacesPanel();
        competitionInterfacesPanel.setParentWindow(this);
        competitionInterfacesPanel.setDatabase(database);
        competitionInterfacesPanel.setNotifier(notifier);
        competitionInterfacesPanel.setPeerManager(peerManager);
        competitionInterfacesPanel.afterPropertiesSet();
        
        mainTabbedPane.insertTab("Competition Interfaces", null, competitionInterfacesPanel, null, 1);

        sessionFightsPanel = new SessionFightsPanel();
        sessionFightsPanel.setParentWindow(this);
        sessionFightsPanel.setDatabase(database);
        sessionFightsPanel.setNotifier(notifier);
        sessionFightsPanel.afterPropertiesSet();
        
        mainTabbedPane.insertTab("Fight Order", null, sessionFightsPanel, null, 1);

        manageSessionsPanel = new ManageSessionsPanel();
        manageSessionsPanel.setParentWindow(this);
        manageSessionsPanel.setDatabase(database);
        manageSessionsPanel.setNotifier(notifier);
        manageSessionsPanel.afterPropertiesSet();
        
        mainTabbedPane.insertTab("Sessions", null, manageSessionsPanel, null, 1);
        
        fightOrderPanel = new FightOrderPanel();
        fightOrderPanel.setParentWindow(this);
        fightOrderPanel.setDatabase(database);
        fightOrderPanel.setNotifier(notifier);
        fightOrderPanel.afterPropertiesSet();
        
        mainTabbedPane.insertTab("Draw", null, fightOrderPanel, null, 1);

        managePoolsPanel = new ManagePoolsPanel();
        managePoolsPanel.setParentWindow(this);
        managePoolsPanel.setDatabase(database);
        managePoolsPanel.setNotifier(notifier);
        managePoolsPanel.afterPropertiesSet();

        mainTabbedPane.insertTab("Divisions", null, managePoolsPanel, null, 1);
        
        managePlayersPanel = new ManagePlayersPanel();
        managePlayersPanel.setParentWindow(this);
        managePlayersPanel.setDatabase(database);
        managePlayersPanel.setNotifier(notifier);
        managePlayersPanel.afterPropertiesSet();

        mainTabbedPane.insertTab("Players", null, managePlayersPanel, null, 1);


        administrationPanel = new AdministrationPanel();
        administrationPanel.setParentWindow(this);
        administrationPanel.setDatabase(database);
        administrationPanel.setNotifier(notifier);
        administrationPanel.setLicenseManager(licenseManager);
        administrationPanel.afterPropertiesSet();

        mainTabbedPane.insertTab("Administration", null, administrationPanel, null, 1);
        
        ChatPanel chatPanel = new ChatPanel(peerManager);
        chatPanel.setBorder(new EmptyBorder(0,0,0,0));
        
        javax.swing.GroupLayout chatPanelLayout = new javax.swing.GroupLayout(this.chatParentPanel);
        this.chatParentPanel.setLayout(chatPanelLayout);
        chatPanelLayout.setHorizontalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(chatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        chatPanelLayout.setVerticalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(chatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        
        this.chatParentPanel.repaint();
        this.chatParentPanel.revalidate();
    }

    public void toggleMasterUnlock() {
        if(PermissionChecker.isMasterUnlocked()) {
            PermissionChecker.lockMaster();
            masterUnlockMenuItem.setText("Unlock Master..");
            masterUnlockMenuItem.setIcon(Icons.UNLOCK);
        } else {
            if(PermissionChecker.isAllowed(Action.UNLOCK_MASTER, database)) {
                PermissionChecker.unlockMaster();
                masterUnlockMenuItem.setText("Lock Master");
                masterUnlockMenuItem.setIcon(Icons.LOCK);
            }
        }
    }

    private void updateMasterPasswordLock() {
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        masterUnlockMenuItem.setEnabled(ci != null && ci.getPasswordHash() != 0);
    }

    public void afterPropertiesSet() {
        createPanels();
        notifier.addListener(competitionInfoListener);
        updateMasterPasswordLock();
        pack();
    }
    
    /**
     * This method may be called after the window has closed to determine
     * whether the user has requested that the competition database be deleted.
     * (see window close handler in Main.java)
     */
    public boolean getDeleteOnExit() {
        return deleteOnExit;
    }
    
    private void manuallyAddHost(final String hostname) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                manualDiscoveryService.manuallyAddHost(hostname);
            }
        }, "Manual peer discovery").start();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        mainTabbedPane = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        resultsButton = new javax.swing.JButton();
        manageSessionsButton = new javax.swing.JButton();
        registerButton = new javax.swing.JButton();
        adminButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        weighInButton = new javax.swing.JButton();
        managePoolsButton = new javax.swing.JButton();
        fightOrderButton = new javax.swing.JButton();
        competitionInterfacesButton = new javax.swing.JButton();
        chatParentPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        masterUnlockMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        fileExitMenuItem = new javax.swing.JMenuItem();
        networkMenu = new javax.swing.JMenu();
        manualAddPeerMenuItem = new javax.swing.JMenuItem();
        firewallMenuItem = new javax.swing.JMenuItem();
        networkLogMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpAboutMenuItem = new javax.swing.JMenuItem();

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Event Manager");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        resultsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table.png"))); // NOI18N
        resultsButton.setText("Results..");
        resultsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        resultsButton.setIconTextGap(8);
        resultsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resultsButtonActionPerformed(evt);
            }
        });

        manageSessionsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/time.png"))); // NOI18N
        manageSessionsButton.setText("Manage Sessions..");
        manageSessionsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        manageSessionsButton.setIconTextGap(8);
        manageSessionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageSessionsButtonActionPerformed(evt);
            }
        });

        registerButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/user.png"))); // NOI18N
        registerButton.setText("Register Players..");
        registerButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        registerButton.setIconTextGap(8);
        registerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                registerButtonActionPerformed(evt);
            }
        });

        adminButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key.png"))); // NOI18N
        adminButton.setText("Administer Competition..");
        adminButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        adminButton.setIconTextGap(8);
        adminButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("Select a task");

        weighInButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/anchor.png"))); // NOI18N
        weighInButton.setText("Weigh In..");
        weighInButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        weighInButton.setIconTextGap(8);
        weighInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weighInButtonActionPerformed(evt);
            }
        });

        managePoolsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/group.png"))); // NOI18N
        managePoolsButton.setText("Divisions..");
        managePoolsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        managePoolsButton.setIconTextGap(8);
        managePoolsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                managePoolsButtonActionPerformed(evt);
            }
        });

        fightOrderButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/chart_organisation.png"))); // NOI18N
        fightOrderButton.setText("Draw..");
        fightOrderButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        fightOrderButton.setIconTextGap(8);
        fightOrderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fightOrderButtonActionPerformed(evt);
            }
        });

        competitionInterfacesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/application_view_tile.png"))); // NOI18N
        competitionInterfacesButton.setText("Competition Interfaces..");
        competitionInterfacesButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        competitionInterfacesButton.setIconTextGap(8);
        competitionInterfacesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                competitionInterfacesButtonActionPerformed(evt);
            }
        });

        chatParentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        chatParentPanel.setMaximumSize(new java.awt.Dimension(150, 32767));

        javax.swing.GroupLayout chatParentPanelLayout = new javax.swing.GroupLayout(chatParentPanel);
        chatParentPanel.setLayout(chatParentPanelLayout);
        chatParentPanelLayout.setHorizontalGroup(
            chatParentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 264, Short.MAX_VALUE)
        );
        chatParentPanelLayout.setVerticalGroup(
            chatParentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 452, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(resultsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(registerButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(weighInButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(managePoolsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(adminButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fightOrderButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(manageSessionsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(competitionInterfacesButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(chatParentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(registerButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(weighInButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(managePoolsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fightOrderButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manageSessionsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(competitionInterfacesButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resultsButton)
                .addContainerGap(126, Short.MAX_VALUE))
            .addComponent(chatParentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        mainTabbedPane.addTab("Main", mainPanel);

        fileMenu.setText("File");

        masterUnlockMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/lock_open.png"))); // NOI18N
        masterUnlockMenuItem.setText("Master Unlock..");
        masterUnlockMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                masterUnlockMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(masterUnlockMenuItem);
        fileMenu.add(jSeparator2);

        fileExitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/door_out.png"))); // NOI18N
        fileExitMenuItem.setText("Exit");
        fileExitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileExitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileExitMenuItem);

        menuBar.add(fileMenu);

        networkMenu.setText("Network");

        manualAddPeerMenuItem.setText("Manually Enter IP/Host..");
        manualAddPeerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualAddPeerMenuItemActionPerformed(evt);
            }
        });
        networkMenu.add(manualAddPeerMenuItem);

        firewallMenuItem.setText("Update Windows Firewall..");
        firewallMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firewallMenuItemActionPerformed(evt);
            }
        });
        networkMenu.add(firewallMenuItem);

        networkLogMenuItem.setText("Log Network Diagnostics..");
        networkLogMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                networkLogMenuItemActionPerformed(evt);
            }
        });
        networkMenu.add(networkLogMenuItem);

        menuBar.add(networkMenu);

        helpMenu.setText("Help");

        helpAboutMenuItem.setText("About..");
        helpAboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpAboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpAboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainTabbedPane)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                .addContainerGap())
        );

        mainTabbedPane.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void manageSessionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manageSessionsButtonActionPerformed
        mainTabbedPane.setSelectedComponent(manageSessionsPanel);
    }//GEN-LAST:event_manageSessionsButtonActionPerformed

    private void fightOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fightOrderButtonActionPerformed
        mainTabbedPane.setSelectedComponent(fightOrderPanel);
    }//GEN-LAST:event_fightOrderButtonActionPerformed

    private void weighInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weighInButtonActionPerformed
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        if(ci == null)
            GUIUtils.displayError(this, "Weigh-in information cannot be entered until competition details have been entered.");
        else if(PermissionChecker.isAllowed(Action.ENTER_WEIGH_IN, database))
            new WeighInDialog(this, true, database, notifier).setVisible(true);
    }//GEN-LAST:event_weighInButtonActionPerformed

    private void fileExitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileExitMenuItemActionPerformed
        this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_fileExitMenuItemActionPerformed

    private void managePoolsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_managePoolsButtonActionPerformed
        mainTabbedPane.setSelectedComponent(managePoolsPanel);
    }//GEN-LAST:event_managePoolsButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        notifier.removeListener(competitionInfoListener);
    }//GEN-LAST:event_formWindowClosing

    private void helpAboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpAboutMenuItemActionPerformed
        (new AboutDialog(this, true)).setVisible(true);
    }//GEN-LAST:event_helpAboutMenuItemActionPerformed

    private void registerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_registerButtonActionPerformed
       mainTabbedPane.setSelectedComponent(managePlayersPanel);
    }//GEN-LAST:event_registerButtonActionPerformed

    private void adminButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminButtonActionPerformed
        mainTabbedPane.setSelectedIndex(mainTabbedPane.indexOfTab("Administration"));
    }//GEN-LAST:event_adminButtonActionPerformed

private void competitionInterfacesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_competitionInterfacesButtonActionPerformed
        mainTabbedPane.setSelectedComponent(competitionInterfacesPanel);
}//GEN-LAST:event_competitionInterfacesButtonActionPerformed

private void resultsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resultsButtonActionPerformed
        mainTabbedPane.setSelectedComponent(resultsPanel);
}//GEN-LAST:event_resultsButtonActionPerformed

private void masterUnlockMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_masterUnlockMenuItemActionPerformed
    toggleMasterUnlock();
}//GEN-LAST:event_masterUnlockMenuItemActionPerformed

    private void firewallMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firewallMenuItemActionPerformed
        String rule = "EventManager Port 1199";
        String title = "Update Firewall";
        
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if(WindowsNetUtil.firewallRuleExists(rule)) {
                GUIUtils.displayMessage(null, "Firewall already configured", title);
                return;
            }
            WindowsNetUtil.openInboundFirewallPort(rule, 1199);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        if(WindowsNetUtil.firewallRuleExists(rule)) {
            GUIUtils.displayMessage(null, "Firewall Successfully Updated", title);
        } else {
            GUIUtils.displayError(null, "Failed to update firewall");
        }
    }//GEN-LAST:event_firewallMenuItemActionPerformed

    private void networkLogMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_networkLogMenuItemActionPerformed
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            WindowsNetUtil.logNetworkInfo();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
   
        GUIUtils.displayMessage(null, "Network diagnostic information has been logged.\nThis will assist the developers to resolve network related issues with EventManager.", "Log Network Info");
    }//GEN-LAST:event_networkLogMenuItemActionPerformed

    private void manualAddPeerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualAddPeerMenuItemActionPerformed
        String hostname = JOptionPane.showInputDialog("Enter an IP Address, Host or Computer Name on which EventManager is running.");
        
        if(!StringUtils.isBlank(hostname)) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                InetAddress.getByName(hostname);
                GUIUtils.displayMessage(null, "The computer '" + hostname + "' was successfully added.\nIf EventManager is able to connect to this computer, it will appear under the chat user list shortly", "Computer added");
                manuallyAddHost(hostname);
            } catch(UnknownHostException e) {
                GUIUtils.displayError(null, "EventManager was unable to identify any computer named '" + hostname + "' (Unknown host)");
            } finally {
                setCursor(Cursor.getDefaultCursor()); 
            }
        }
    }//GEN-LAST:event_manualAddPeerMenuItemActionPerformed

            
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton adminButton;
    private javax.swing.JPanel chatParentPanel;
    private javax.swing.JButton competitionInterfacesButton;
    private javax.swing.JButton fightOrderButton;
    private javax.swing.JMenuItem fileExitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem firewallMenuItem;
    private javax.swing.JMenuItem helpAboutMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JButton managePoolsButton;
    private javax.swing.JButton manageSessionsButton;
    private javax.swing.JMenuItem manualAddPeerMenuItem;
    private javax.swing.JMenuItem masterUnlockMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem networkLogMenuItem;
    private javax.swing.JMenu networkMenu;
    private javax.swing.JButton registerButton;
    private javax.swing.JButton resultsButton;
    private javax.swing.JButton weighInButton;
    // End of variables declaration//GEN-END:variables

}
