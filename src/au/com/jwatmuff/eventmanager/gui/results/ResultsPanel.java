/*
 * ScoringPanel.java
 *
 * Created on 28 August 2008, 14:54
 */

package au.com.jwatmuff.eventmanager.gui.results;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.print.MultipleDrawHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.CheckboxListDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class ResultsPanel extends javax.swing.JPanel {
    public static final Logger log = Logger.getLogger(ResultsPanel.class);
    
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private ResultInfoCache resultInfoCache;
    private DivisionResultCache divisionResultCache;

    /** Creates new form FightProgressionPanel */
    public ResultsPanel() {
        initComponents();
    }
    
    @Required
    public void setDatabase(TransactionalDatabase database) {
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
        divisionResultCache = new DivisionResultCache(database, notifier);
    }

    public void shutdown() {
        resultInfoCache.shutdown();
        divisionResultCache.shutdown();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        resultsSummaryButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        pointsButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        divisionResultsButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        printDrawResultsButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();

        resultsSummaryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table.png"))); // NOI18N
        resultsSummaryButton.setText("Results Summary..");
        resultsSummaryButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        resultsSummaryButton.setIconTextGap(8);
        resultsSummaryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resultsSummaryButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Display all fight results recorded in the competition");

        pointsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table.png"))); // NOI18N
        pointsButton.setText("Points Cards..");
        pointsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        pointsButton.setIconTextGap(8);
        pointsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pointsButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Display and print points cards");

        divisionResultsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table.png"))); // NOI18N
        divisionResultsButton.setText("Division Results..");
        divisionResultsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        divisionResultsButton.setIconTextGap(8);
        divisionResultsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                divisionResultsButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Display first, second and third positions for each division");

        printDrawResultsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printDrawResultsButton.setText("Draw Sheets..");
        printDrawResultsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        printDrawResultsButton.setIconTextGap(8);
        printDrawResultsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printDrawResultsButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("Print draw sheets showing results");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel5.setText("Results information");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
                    .addComponent(resultsSummaryButton)
                    .addComponent(jLabel1)
                    .addComponent(pointsButton)
                    .addComponent(jLabel2)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
                    .addComponent(divisionResultsButton)
                    .addComponent(jLabel3)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
                    .addComponent(printDrawResultsButton)
                    .addComponent(jLabel4))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {divisionResultsButton, pointsButton, printDrawResultsButton, resultsSummaryButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resultsSummaryButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pointsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(divisionResultsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printDrawResultsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addContainerGap(55, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

private void resultsSummaryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resultsSummaryButtonActionPerformed
    try {
        final ResultsWindow win = new ResultsWindow();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final ResultsSummaryPanel p = new ResultsSummaryPanel();
        p.setParentWindow(win);
        p.setDatabase(database);
        p.setNotifier(notifier);
        p.setResultInfoCache(resultInfoCache);
        p.afterPropertiesSet();
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
}//GEN-LAST:event_resultsSummaryButtonActionPerformed

private void pointsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pointsButtonActionPerformed
    try {
        final ResultsWindow win = new ResultsWindow();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final ResultsPointsPanel p = new ResultsPointsPanel();
        p.setParentWindow(win);
        p.setDatabase(database);
        p.setNotifier(notifier);
        p.setResultInfoCache(resultInfoCache);
        p.afterPropertiesSet();
        parentWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                win.dispose();
            }
        });

        win.getMainPanel().add(p);
        win.setTitle("Event Manager - Results - Grading Points");
        win.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                p.shutdown();
            }
        });
        win.setVisible(true);
    } finally {
        setCursor(Cursor.getDefaultCursor());
    }
}//GEN-LAST:event_pointsButtonActionPerformed

private void divisionResultsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_divisionResultsButtonActionPerformed
    try {
        final ResultsWindow win = new ResultsWindow();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DivisionResultsPanel p = new DivisionResultsPanel();
        p.setParentWindow(win);
        p.setDatabase(database);
        p.setNotifier(notifier);
        p.setDivisionResultCache(divisionResultCache);
        p.afterPropertiesSet();
        parentWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                win.dispose();
            }
        });

        win.getMainPanel().add(p);
        win.setTitle("Event Manager - Results - Divisions");
        win.setVisible(true);
    } finally {
        setCursor(Cursor.getDefaultCursor());
    }
}//GEN-LAST:event_divisionResultsButtonActionPerformed

private void printDrawResultsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printDrawResultsButtonActionPerformed
    List<Pool> pools = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED);
    if(pools.size() == 0) {
        GUIUtils.displayMessage(null, "At least one division with locked fights must exist to print results", "Print Results");
        return;
    }
    //ListDialog<Pool> dialog = new ListDialog<Pool>(parentWindow, true, pools, "Choose Division", "Print Results");
    CheckboxListDialog<Pool> dialog = new CheckboxListDialog<Pool>(parentWindow, true, pools, "Choose Division", "Print Results");
    dialog.setRenderer(new StringRenderer<Pool>() {
            @Override
            public String asString(Pool p) {
                return p.getDescription();
            }
    }, Icons.POOL);
    dialog.setVisible(true);
    if(!dialog.getSuccess()) return;

//    for(Pool pool : dialog.getSelectedItems()) {
        //int poolID = dialog.getSelectedItem().getID();

  //      new DrawHTMLGenerator(database, pool.getID(), true).openInBrowser();
    //}
    new MultipleDrawHTMLGenerator(database, dialog.getSelectedItems(), true).openInBrowser();
}//GEN-LAST:event_printDrawResultsButtonActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton divisionResultsButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton pointsButton;
    private javax.swing.JButton printDrawResultsButton;
    private javax.swing.JButton resultsSummaryButton;
    // End of variables declaration//GEN-END:variables

}
