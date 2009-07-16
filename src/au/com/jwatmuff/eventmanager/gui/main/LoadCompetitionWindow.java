/*
 * LoadCompetitionWindow.java
 *
 * Created on 23 July 2008, 13:26
 */

package au.com.jwatmuff.eventmanager.gui.main;

import au.com.jwatmuff.eventmanager.Main;
import au.com.jwatmuff.eventmanager.gui.admin.EnterPasswordDialog;
import au.com.jwatmuff.eventmanager.gui.license.LicenseKeyDialog;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoringSystem;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardWindow;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.License;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.util.DirUtils;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.ZipUtils;
import au.com.jwatmuff.genericdb.p2p.DatabaseInfo;
import au.com.jwatmuff.genericdb.p2p.DatabaseManager;
import java.awt.Component;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.Logger;

/**
 * This interface is the entry point to the EventManager program. It lists
 * all local and network competition databases, allowing the user to launch
 * the EventManager for one of these competitions or a brand new competition.
 *
 * @author  James
 */
public class LoadCompetitionWindow extends javax.swing.JFrame {
    private static Logger log = Logger.getLogger(LoadCompetitionWindow.class);
    private SimpleDateFormat dateFormat = new SimpleDateFormat();
    
    private DatabaseManager dbManager;
    private LicenseManager licenseManager;
    
    private DatabaseInfo selected;
    private boolean isNew = false;
    private boolean success = false;
    
    private DefaultListModel dbListModel = new DefaultListModel();

    private static final int CHECK_DATABASES_PERIOD = 5000; //milliseconds

    private Runnable checkDatabasesTask = new Runnable() {
        @Override
        public void run() {
            updateDatabaseList();
        }
    };

    private ScheduledExecutorService checkDatabasesExecutor;
    
    /** Creates new form LoadCompetitionWindow */
    public LoadCompetitionWindow(DatabaseManager dbManager, LicenseManager licenseManager) {
        initComponents();
        setIconImage(Icons.MAIN_WINDOW.getImage());

        this.dbManager = dbManager;
        this.licenseManager = licenseManager;
        this.getRootPane().setDefaultButton(okButton);

        competitionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object obj, int arg2, boolean arg3, boolean arg4) {
                if(obj instanceof DatabaseInfo) {
                    DatabaseInfo di = (DatabaseInfo)obj;
                    JLabel label = (JLabel)super.getListCellRendererComponent(list, di.name, arg2, arg3, arg4);
                    label.setIcon((!Main.VERSION.equals(di.version)) ? Icons.NO :
                                  (di.peers > 0) ? Icons.REMOTE : Icons.LOCAL);
                    return label;
                }
                return super.getListCellRendererComponent(list, obj, arg2, arg3, arg4);
            }
            
        });
        
        this.competitionList.setModel(dbListModel);        
        updateDatabaseList();
        updateLicenseInfo();
        
        // center window on screen
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean visible) {
        if(visible) {
            success = false;
            updateLicenseInfo();
            checkDatabasesExecutor = Executors.newSingleThreadScheduledExecutor();
            checkDatabasesExecutor.scheduleAtFixedRate(
                    checkDatabasesTask,
                    0, CHECK_DATABASES_PERIOD, TimeUnit.MILLISECONDS);
            dbManager.setListener(new DatabaseManager.Listener() {
                @Override
                public void handleDatabaseManagerEvent() {
                    updateDatabaseList();
                }
            });
        }
        super.setVisible(visible);
    }
    
    private void updateDatabaseList() {
        log.debug("Attempting to update database list");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dbManager.updateAllDatabaseInfo();
                DatabaseInfo oldSelected = (DatabaseInfo)competitionList.getSelectedValue();
                dbListModel.clear();
                for(DatabaseInfo info : dbManager.getDatabases())
                    dbListModel.addElement(info);
                
                boolean compsPresent = dbListModel.size() > 0;
                existingCompRadioButton.setEnabled(compsPresent);

                if(oldSelected != null) {
                    competitionList.setSelectedValue(oldSelected, true);
                }
            }
        });   
    }

    private void updateLicenseInfo() {
        License license = licenseManager.getLicense();

        if(license == null) {
            licenseNameLabel.setText("N/A");
            licenseContactLabel.setText("N/A");
            licenseTypeLabel.setText("FREE");
            licenseExpiryLabel.setText("Never");
        } else {
            licenseNameLabel.setText(license.getName());
            licenseContactLabel.setText(license.getContactPhoneNumber());
            licenseTypeLabel.setText(license.getType().toString());
            licenseExpiryLabel.setText(license.getExpiry().toString());
        }
        this.pack();
    }
    
    public boolean getSuccess() {
        return success;
    }
    
    public DatabaseInfo getSelectedDatabaseInfo() {
        return selected;
    }
    
    public boolean isNewDatabase() {
        return isNew;
    }

    private void updateOkButton() {
        if(newCompRadioButton.isSelected()) {
            okButton.setText("Open");
            okButton.setEnabled(true);
        } else if(existingCompRadioButton.isSelected()) {
            okButton.setText("Open");
            okButton.setEnabled(competitionList.getSelectedIndex() >= 0);
        }
    }

    @Override
    public void dispose() {
        dbManager.setListener(null);
        if(checkDatabasesExecutor != null) checkDatabasesExecutor.shutdownNow();
        super.dispose();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        existingCompRadioButton = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        competitionList = new javax.swing.JList();
        loadBackupButton = new javax.swing.JButton();
        saveBackupButton = new javax.swing.JButton();
        deleteCompButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        newCompRadioButton = new javax.swing.JRadioButton();
        jPanel4 = new javax.swing.JPanel();
        manualScoreboardButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        licenseNameLabel = new javax.swing.JLabel();
        licenseContactLabel = new javax.swing.JLabel();
        licenseTypeLabel = new javax.swing.JLabel();
        licenseExpiryLabel = new javax.swing.JLabel();
        loadLicenseButton = new javax.swing.JButton();
        enterLicenseKeyButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Event Manager");
        setLocationByPlatform(true);
        setResizable(false);

        okButton.setText("Open");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Exit");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buttonGroup1.add(existingCompRadioButton);
        existingCompRadioButton.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        existingCompRadioButton.setText("Open Existing Competition");
        existingCompRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        existingCompRadioButton.setEnabled(false);
        existingCompRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        existingCompRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                existingCompRadioButtonActionPerformed(evt);
            }
        });

        competitionList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        competitionList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                competitionListMouseClicked(evt);
            }
        });
        competitionList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                competitionListValueChanged(evt);
            }
        });
        competitionList.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                competitionListFocusGained(evt);
            }
        });
        jScrollPane1.setViewportView(competitionList);

        loadBackupButton.setText("Load Backup..");
        loadBackupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadBackupButtonActionPerformed(evt);
            }
        });

        saveBackupButton.setText("Save Backup..");
        saveBackupButton.setEnabled(false);
        saveBackupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBackupButtonActionPerformed(evt);
            }
        });

        deleteCompButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/delete.png"))); // NOI18N
        deleteCompButton.setText("Delete Competition");
        deleteCompButton.setEnabled(false);
        deleteCompButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCompButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                    .addComponent(existingCompRadioButton)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(loadBackupButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveBackupButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteCompButton)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(existingCompRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadBackupButton)
                    .addComponent(saveBackupButton)
                    .addComponent(deleteCompButton))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buttonGroup1.add(newCompRadioButton);
        newCompRadioButton.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        newCompRadioButton.setSelected(true);
        newCompRadioButton.setText("Create New Competition");
        newCompRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        newCompRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        newCompRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newCompRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newCompRadioButton)
                .addContainerGap(243, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newCompRadioButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Start Competition", jPanel3);

        manualScoreboardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/application_view_tile.png"))); // NOI18N
        manualScoreboardButton.setText("Manual Scoreboard..");
        manualScoreboardButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        manualScoreboardButton.setIconTextGap(8);
        manualScoreboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualScoreboardButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(manualScoreboardButton)
                .addContainerGap(263, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(manualScoreboardButton)
                .addContainerGap(404, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Manual Interfaces", jPanel4);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Current License"));

        jLabel1.setText("Name:");

        jLabel2.setText("Contact Phone:");

        jLabel3.setText("Level:");

        jLabel4.setText("Expiry:");

        licenseNameLabel.setText("N/A");

        licenseContactLabel.setText("N/A");

        licenseTypeLabel.setFont(new java.awt.Font("Tahoma", 1, 11));
        licenseTypeLabel.setText("FREE");

        licenseExpiryLabel.setText("N/A");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(licenseNameLabel))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(licenseContactLabel))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(licenseTypeLabel))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(licenseExpiryLabel)))
                .addContainerGap(269, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(licenseNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(licenseContactLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(licenseTypeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(licenseExpiryLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        loadLicenseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key_add.png"))); // NOI18N
        loadLicenseButton.setText("Load License File..");
        loadLicenseButton.setIconTextGap(8);
        loadLicenseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadLicenseButtonActionPerformed(evt);
            }
        });

        enterLicenseKeyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key_add.png"))); // NOI18N
        enterLicenseKeyButton.setText("Enter License Key..");
        enterLicenseKeyButton.setIconTextGap(8);
        enterLicenseKeyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterLicenseKeyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(loadLicenseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(enterLicenseKeyButton)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadLicenseButton)
                    .addComponent(enterLicenseKeyButton))
                .addContainerGap(286, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Licenses", jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void competitionListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_competitionListMouseClicked
        // handle double click on a list the same as pressing OK
        if(evt.getClickCount() == 2 && existingCompRadioButton.isSelected())
            okButtonActionPerformed(null);
}//GEN-LAST:event_competitionListMouseClicked
    
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed
    
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if(newCompRadioButton.isSelected()) {
            if(!PermissionChecker.isAllowed(Action.CREATE_COMPETITION, null)) return;
            NewCompetitionDialog ncd = new NewCompetitionDialog(this, true);
            ncd.setVisible(true);
            if(ncd.getSuccess()) {
                selected = dbManager.createNewDatabase(ncd.getCompetitionName(), ncd.getPasswordHash());
                isNew = true;
                success = true;
                this.dispose();
                return;
            }
        }
        
        if(existingCompRadioButton.isSelected()) {
            selected = (DatabaseInfo)competitionList.getSelectedValue();
            if(selected == null) return;
            if(!Main.VERSION.equals(selected.version)) {
                GUIUtils.displayError(this, "This competition is only compatible with version " + selected.version + ".\nThis copy of EventManager is version " + Main.VERSION + ".");
                return;
            }

            /* password check */
            if(!dbManager.authenticate(selected, 0)) {
                EnterPasswordDialog epd = new EnterPasswordDialog(this, true);
                epd.setActionText("Load Competition '" + selected.name + "'");
                epd.setPromptText("Connect password required");
                while(true) {
                    epd.setVisible(true);
                    if(epd.getSuccess()) {
                        int passwordHash = epd.getPassword().hashCode();
                        if(dbManager.authenticate(selected, passwordHash)) {
                            selected.passwordHash = passwordHash;
                            break;
                        } else {
                            GUIUtils.displayError(this, "Incorrect password");
                        }
                    } else {
                        selected = null;
                        return;
                    }
                }
            }

            success = true;
            this.dispose();
            return;
        }
    }//GEN-LAST:event_okButtonActionPerformed
    
    private void competitionListFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_competitionListFocusGained
        if(existingCompRadioButton.isEnabled())
            existingCompRadioButton.setSelected(true);
}//GEN-LAST:event_competitionListFocusGained

    private void enterLicenseKeyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterLicenseKeyButtonActionPerformed
        LicenseKeyDialog dialog = new LicenseKeyDialog(null, true);
        dialog.setVisible(true);
        if(dialog.getSuccess()) {
            try {
                licenseManager.setLicense(dialog.getLicense());
            } catch(IOException e) {
                GUIUtils.displayError(null, "Unable to save license file. License will not be remembered after EventManager is closed");
            }
            updateLicenseInfo();
        }
    }//GEN-LAST:event_enterLicenseKeyButtonActionPerformed

    private void loadLicenseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadLicenseButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("License File", "lic"));
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            License license = License.loadFromFile(file);
            if(license != null)
                try {
                    licenseManager.setLicense(license);
                    updateLicenseInfo();
                } catch(IOException e) {
                    GUIUtils.displayError(this, "Error updating license. You may need to reload the license next time you start EventManager");
                }
            else
                GUIUtils.displayError(this, "Error while loading license file");
        }
    }//GEN-LAST:event_loadLicenseButtonActionPerformed

    private void manualScoreboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualScoreboardButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.MANUAL_SCOREBOARD, null)) return;
            new ScoreboardWindow("Manual Scoreboard", ScoringSystem.NEW).setVisible(true);
    }//GEN-LAST:event_manualScoreboardButtonActionPerformed

    private void competitionListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_competitionListValueChanged
        DatabaseInfo info = (DatabaseInfo)competitionList.getSelectedValue();
        boolean local = (info != null && info.local);
        saveBackupButton.setEnabled(local);
        deleteCompButton.setEnabled(local);
        updateOkButton();
    }//GEN-LAST:event_competitionListValueChanged

    private void saveBackupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBackupButtonActionPerformed
        DatabaseInfo info = (DatabaseInfo)competitionList.getSelectedValue();
        if(info == null || !info.local) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Event Manager Files", "evm"));
        if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if(!file.getName().toLowerCase().endsWith(".evm"))
                file = new File(file.getAbsolutePath() + ".evm");
            if(file.exists()) {
                int result = JOptionPane.showConfirmDialog(rootPane, file.getName() + " already exists. Overwrite file?", "Save Backup", JOptionPane.YES_NO_OPTION);
                if(result != JOptionPane.YES_OPTION) return;
            }
            try {
                ZipUtils.zipFolder(info.localDirectory, file, false);
            } catch(Exception e) {
                GUIUtils.displayError(this, "Failed to save file: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_saveBackupButtonActionPerformed

    private void loadBackupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBackupButtonActionPerformed
        File databaseStore = new File("comps");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Event Manager Files", "evm"));
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            /* input zip file */
            File file = chooser.getSelectedFile();
            /* construct output directory */
            File dir = new File(databaseStore, file.getName());
            int suffix = 0;
            while(dir.exists()) {
                suffix++;
                dir = new File(databaseStore, file.getName() + "_" + suffix);
            }
            /* unzip */
            try {
                ZipUtils.unzipFile(dir, file);

                /* change id */
                Properties props = new Properties();
                FileReader fr = new FileReader(new File(dir, "info.dat"));
                props.load(fr);
                fr.close();
                props.setProperty("UUID", UUID.randomUUID().toString());
                props.setProperty("name", props.getProperty("name") + " - " + dateFormat.format(new Date()));
                FileWriter fw = new FileWriter(new File(dir, "info.dat"));
                props.store(fw, "");
                fw.close();

                /* update gui */
                checkDatabasesExecutor.schedule(checkDatabasesTask, 0, TimeUnit.MILLISECONDS);
            } catch(Exception e) {
                GUIUtils.displayError(null, "Error while opening file: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_loadBackupButtonActionPerformed

    private void deleteCompButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteCompButtonActionPerformed
        DatabaseInfo info = (DatabaseInfo)competitionList.getSelectedValue();
        if(info == null || !info.local) return;
        int result = JOptionPane.showConfirmDialog(rootPane, "Delete " + info.name + " permanently?", "Delete Competition", JOptionPane.YES_NO_OPTION);
        if(result == JOptionPane.YES_OPTION) {
            DirUtils.deleteDir(info.localDirectory);
            checkDatabasesExecutor.schedule(checkDatabasesTask, 0, TimeUnit.MILLISECONDS);
        }
    }//GEN-LAST:event_deleteCompButtonActionPerformed

    private void newCompRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newCompRadioButtonActionPerformed
        competitionList.clearSelection();
        updateOkButton();
    }//GEN-LAST:event_newCompRadioButtonActionPerformed

    private void existingCompRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_existingCompRadioButtonActionPerformed
        updateOkButton();
    }//GEN-LAST:event_existingCompRadioButtonActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JList competitionList;
    private javax.swing.JButton deleteCompButton;
    private javax.swing.JButton enterLicenseKeyButton;
    private javax.swing.JRadioButton existingCompRadioButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel licenseContactLabel;
    private javax.swing.JLabel licenseExpiryLabel;
    private javax.swing.JLabel licenseNameLabel;
    private javax.swing.JLabel licenseTypeLabel;
    private javax.swing.JButton loadBackupButton;
    private javax.swing.JButton loadLicenseButton;
    private javax.swing.JButton manualScoreboardButton;
    private javax.swing.JRadioButton newCompRadioButton;
    private javax.swing.JButton okButton;
    private javax.swing.JButton saveBackupButton;
    // End of variables declaration//GEN-END:variables
    
}
