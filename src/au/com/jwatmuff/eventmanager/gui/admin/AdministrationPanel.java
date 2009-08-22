/*
 * AdministrationPanel.java
 *
 * Created on 19 March 2008, 18:36
 */

package au.com.jwatmuff.eventmanager.gui.admin;

import au.com.jwatmuff.eventmanager.gui.results.ResultsSummaryPanel;
import au.com.jwatmuff.eventmanager.gui.results.ResultsWindow;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author  James
 */
public class AdministrationPanel extends javax.swing.JPanel {
    private Frame parentWindow;
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private LicenseManager licenseManager;

    private ResultInfoCache resultInfoCache;

    /** Creates new form AdministrationPanel */
    public AdministrationPanel() {
        initComponents();
    }
    
    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }
    
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }

    public void setLicenseManager(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }
    
    public void afterPropertiesSet() {
        resultInfoCache = new ResultInfoCache(database, notifier);
    }
          
    public void showCompetitionDetailsDialog() {
        (new CompetitionDetailsDialog(parentWindow,true,database,notifier,licenseManager)).setVisible(true);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        enterCompDetailsButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        managePasswordsButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        reviewDataButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setText("Administration tasks");

        enterCompDetailsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/page_edit.png"))); // NOI18N
        enterCompDetailsButton.setText("Edit Competition Details..");
        enterCompDetailsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        enterCompDetailsButton.setIconTextGap(8);
        enterCompDetailsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterCompDetailsButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Update competition name, location and available divisions");

        managePasswordsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key.png"))); // NOI18N
        managePasswordsButton.setText("Manage Passwords..");
        managePasswordsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        managePasswordsButton.setIconTextGap(8);
        managePasswordsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                managePasswordsButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("Set and change passwords for various tasks");

        reviewDataButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wrench_orange.png"))); // NOI18N
        reviewDataButton.setText("Change Results..");
        reviewDataButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        reviewDataButton.setIconTextGap(8);
        reviewDataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reviewDataButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Manually enter or update results");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(320, 320, 320))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jSeparator4, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(managePasswordsButton)
                            .addComponent(jLabel6))
                        .addContainerGap(226, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                            .addComponent(enterCompDetailsButton, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(reviewDataButton)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap(282, Short.MAX_VALUE))))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {enterCompDetailsButton, managePasswordsButton, reviewDataButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enterCompDetailsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(managePasswordsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reviewDataButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addContainerGap(74, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void managePasswordsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_managePasswordsButtonActionPerformed
        ManagePasswordsDialog mpd = new ManagePasswordsDialog(parentWindow, true);
        mpd.setDatabase(database);
        mpd.setNotifier(notifier);
        mpd.setVisible(true);
    }//GEN-LAST:event_managePasswordsButtonActionPerformed

    private void enterCompDetailsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterCompDetailsButtonActionPerformed
        showCompetitionDetailsDialog();
    }//GEN-LAST:event_enterCompDetailsButtonActionPerformed

private void reviewDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reviewDataButtonActionPerformed
    if(!PermissionChecker.isAllowed(Action.CHANGE_RESULTS, database)) return;
    try {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final ResultsSummaryPanel p = new ResultsSummaryPanel(true);
        p.setParentWindow(parentWindow);
        p.setDatabase(database);
        p.setNotifier(notifier);
        p.setResultInfoCache(resultInfoCache);
        p.afterPropertiesSet();

        final ResultsWindow win = new ResultsWindow();
        parentWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                win.dispose();
            }
        });
        win.getMainPanel().add(p);
        win.setTitle("Event Manager - Results - Summary");
        win.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                p.shutdown();
            }
        });
        win.setVisible(true);
    } finally {
        setCursor(Cursor.getDefaultCursor());
    }
}//GEN-LAST:event_reviewDataButtonActionPerformed
    
    public void setParentWindow(Frame parentWindow)
    {
        this.parentWindow = parentWindow;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton enterCompDetailsButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton managePasswordsButton;
    private javax.swing.JButton reviewDataButton;
    // End of variables declaration//GEN-END:variables
    
}
