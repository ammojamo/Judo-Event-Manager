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

package au.com.jwatmuff.eventmanager.gui.player;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerLocker;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.print.PlayerListHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.RowSorter.SortKey;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jdesktop.xswingx.PromptSupport;

/**
 *
 * @author  James
 */
public class WeighInDialog extends javax.swing.JDialog {
    private static final Logger log = Logger.getLogger(WeighInDialog.class);

    private PlayerListTableModel tableModel;
    private Frame parent;
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    
    /** Creates new form WeighInDialog */
    public WeighInDialog(java.awt.Frame parent, boolean modal, TransactionalDatabase database, TransactionNotifier notifier) {
        super(parent, modal);
        initComponents();
        this.parent = parent;
        this.database = database;
        this.notifier = notifier;
        
        tableModel = new PlayerListTableModel(database, notifier);
        tableModel.addColumn("Player ID", "visibleID");
        tableModel.addColumn("Last Name", "lastName");
        tableModel.addColumn("First Name", "firstName");
        tableModel.addColumn("Gender", "gender");
        tableModel.addColumn("Weight", "weight");
        tableModel.updateTableFromDatabase();
        notifier.addListener(tableModel);
        playerListTable.setModel(tableModel);
        playerListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        SortKey key0 = new SortKey(3, SortOrder.ASCENDING);
        SortKey key1 = new SortKey(1, SortOrder.ASCENDING);
        SortKey key2 = new SortKey(2, SortOrder.ASCENDING);
        SortKey key3 = new SortKey(0, SortOrder.ASCENDING);
        playerListTable.getRowSorter().setSortKeys(Arrays.asList(key0, key1, key2, key3));

        List<Pool> divisions = database.findAll(Pool.class, PoolDAO.ALL);

        // Fake entry to show 'All Divisions' option
        Pool allDivisions = new Pool();
        allDivisions.setDescription("All Divisions");
        allDivisions.setID(-1);

        divisions.add(0, allDivisions);

        DefaultComboBoxModel<Pool> divisionComboBoxModel = new DefaultComboBoxModel<>(divisions.toArray(new Pool[0]));
        divisionComboBox.setModel(divisionComboBoxModel);
        divisionComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                value = ((Pool) value).getDescription();
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        PromptSupport.setPrompt("First/Last Name", searchTextField);
    }
    
    private Player getSelectedPlayer()
    {
        if(playerListTable.getSelectedRows().length == 0)
            return null;
        int row = playerListTable.getSelectedRows()[0];
        row = playerListTable.getRowSorter().convertRowIndexToModel(row);
        Player p = tableModel.getAtRow(row);

        return p;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        playerListTable = new javax.swing.JTable();
        enterWeightButton = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        printButton = new javax.swing.JButton();
        divisionComboBox = new javax.swing.JComboBox<Pool>();
        searchTextField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Weigh In");
        setLocationByPlatform(true);
        setResizable(false);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        playerListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        playerListTable.setAutoCreateRowSorter(true);
        playerListTable.setGridColor(new java.awt.Color(204, 204, 204));
        playerListTable.setRowHeight(19);
        playerListTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playerListTableMouseClicked(evt);
            }
        });
        playerListTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                playerListTableKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(playerListTable);

        enterWeightButton.setText("Enter Weight..");
        enterWeightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterWeightButtonActionPerformed(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printButton.setFocusable(false);
        printButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        printButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(printButton);

        divisionComboBox.setToolTipText("Filter by Division");
        divisionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                divisionComboBoxActionPerformed(evt);
            }
        });

        searchTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchTextFieldKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(enterWeightButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(divisionComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchTextField)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(enterWeightButton)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(divisionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void playerListTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_playerListTableKeyPressed
        if(evt.getKeyCode() == KeyEvent.VK_ENTER)
            enterWeightButtonActionPerformed(null);
    }//GEN-LAST:event_playerListTableKeyPressed

    private void playerListTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListTableMouseClicked
        // Double clicking on a table entry is handled the same as clicking
        // the enter weight button.
        if(evt.getClickCount() == 2)
            enterWeightButtonActionPerformed(null);
    }//GEN-LAST:event_playerListTableMouseClicked

    private void enterWeightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterWeightButtonActionPerformed
        int selectedIndex = playerListTable.getSelectedRow();
        Player player = getSelectedPlayer();
        if(player!= null) {
            if(player.getWeight() != 0.0) {
                if(!PermissionChecker.isAllowed(Action.CHANGE_WEIGH_IN, database)) return;
            }
            
            EnterWeightDialog ewd = new EnterWeightDialog(parent, true, database, notifier, player);
            ewd.setVisible(true);
            if(ewd.getSuccess()) {
                player.setWeight(ewd.getWeight());
                database.update(player);
                
                if(player.getLockedStatus() != Player.LockedStatus.LOCKED) {
                    try {
                        PlayerLocker.lockPlayer(database, player);
                    } catch(DatabaseStateException e) {
                        GUIUtils.displayError(parent, e.getMessage());
                    }
                }
            }
            selectedIndex = Math.min(selectedIndex++, playerListTable.getRowCount()-1);
            playerListTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);            
        } else {
            GUIUtils.displayMessage(parent, "No player selected", "Enter Weight");
        }
    }//GEN-LAST:event_enterWeightButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        if(playerListTable.getRowCount() == 0)
        JOptionPane.showMessageDialog(this, "No players to print");

        List<Player> players = new ArrayList<>();

        for(int row = 0; row < playerListTable.getRowCount(); row++) {
            int mrow = playerListTable.getRowSorter().convertRowIndexToModel(row);
            players.add(tableModel.getAtRow(mrow));
        }
    new PlayerListHTMLGenerator(database, players).openInBrowser();
}//GEN-LAST:event_printButtonActionPerformed

    private void divisionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_divisionComboBoxActionPerformed
        tableModel.setDivisionFilter(divisionComboBox.getItemAt(divisionComboBox.getSelectedIndex()));
    }//GEN-LAST:event_divisionComboBoxActionPerformed

    private void searchTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchTextFieldKeyTyped
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tableModel.setNameFilter(searchTextField.getText());
            }
        });
    }//GEN-LAST:event_searchTextFieldKeyTyped

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Pool> divisionComboBox;
    private javax.swing.JButton enterWeightButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton okButton;
    private javax.swing.JTable playerListTable;
    private javax.swing.JButton printButton;
    private javax.swing.JTextField searchTextField;
    // End of variables declaration//GEN-END:variables
    
}
