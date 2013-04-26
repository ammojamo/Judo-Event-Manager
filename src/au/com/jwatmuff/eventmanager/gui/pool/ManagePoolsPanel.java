/*
 * ManagePoolsPanel.java
 *
 * Created on 25 March 2008, 13:26
 */
package au.com.jwatmuff.eventmanager.gui.pool;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.gui.player.PlayerDetailsDialog;
import au.com.jwatmuff.eventmanager.gui.wizard.DrawWizardWindow;
import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.misc.AutoAssign;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PoolChecker;
import au.com.jwatmuff.eventmanager.model.misc.PoolLocker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.print.PoolListHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.CheckboxListDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class ManagePoolsPanel extends javax.swing.JPanel {

    private static Logger log = Logger.getLogger(ManagePoolsPanel.class);
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private PoolListTableModel tableModel;
    private PoolPlayerListModel requestedListModel;
    private PoolPlayerListModel approvedListModel;
    private Date censusDate;
    private ConfigurationFile configurationFile;
    public static final Comparator<Pool> POOL_COMPARATOR = new Comparator<Pool>() {

        @Override
        public int compare(Pool p1, Pool p2) {
            if (p1.getMaximumAge() == p2.getMaximumAge()) {
                if (p2.getGender().equals(p1.getGender())) {
                    if (p1.getMaximumWeight() == p2.getMaximumWeight()) {
                        if (p1.getMinimumWeight() == p2.getMinimumWeight()) {
                            return p1.getDescription().compareTo(p2.getDescription());
                        } else {
                            if (p1.getMinimumWeight() == 0) {
                                return 1;
                            } else if (p2.getMinimumWeight() == 0) {
                                return -1;
                            } else {
                                return -Double.compare(p1.getMinimumWeight(), p2.getMinimumWeight());
                            }
                        }
                    } else {
                        if (p1.getMaximumWeight() == 0) {
                            return 1;
                        } else if (p2.getMaximumWeight() == 0) {
                            return -1;
                        } else {
                            return Double.compare(p1.getMaximumWeight(), p2.getMaximumWeight());
                        }
                    }
                } else {
                    return p2.getGender().compareTo(p1.getGender());
                }
            } else {
                if (p1.getMaximumAge() == 0) {
                    return 1;
                } else if (p2.getMaximumAge() == 0) {
                    return -1;
                } else {
                    return p1.getMaximumAge() - p2.getMaximumAge();
                }
            }
        }
    };

    /** Creates new form ManagePoolsPanel */
    public ManagePoolsPanel() {
        initComponents();
    }

    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }

    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }

    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }

    public void afterPropertiesSet() {
        tableModel = new PoolListTableModel(database, notifier);
        poolListTable.setModel(tableModel);

        censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
        configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());

        PlayerListCellRenderer playerListRenderer = new PlayerListCellRenderer(censusDate);

        requestedListModel = new PoolPlayerListModel(database, notifier, false);
        requestedList.setCellRenderer(playerListRenderer);
        requestedList.setModel(requestedListModel);
        approvedListModel = new PoolPlayerListModel(database, notifier, true);
        approvedList.setModel(approvedListModel);
        approvedList.setCellRenderer(playerListRenderer);

        poolListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        poolListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                Pool pool = getSelectedPool();

                requestedListModel.setSelectedPool(pool);
                approvedListModel.setSelectedPool(pool);

                if (pool != null) {
                    boolean unlocked = (pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED);
                    boolean nopool = (pool.getID() == 0);
                    requestedList.setEnabled(unlocked && !nopool);
                    approvedList.setEnabled(unlocked || nopool);
                    autoApproveButton.setEnabled(unlocked && !nopool);
                }

                approveButton.setEnabled(false);
                removeButton.setEnabled(false);
            }
        });

        requestedList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        requestedList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (evt.getFirstIndex() >= 0) {
                    approveButton.setEnabled(true);
                    removeButton.setEnabled(true);
                    approvedList.clearSelection();
                } else {
                    approveButton.setEnabled(false);
                    removeButton.setEnabled(requestedList.getSelectedIndex() >= 0);
                }
            }
        });

        approvedList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        approvedList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (evt.getFirstIndex() >= 0) {
                    approveButton.setEnabled(false);
                    removeButton.setEnabled(true);
                    requestedList.clearSelection();
                } else {
                    removeButton.setEnabled(approvedList.getSelectedIndex() >= 0);
                }
            }
        });

        notifier.addListener(new TransactionListener() {

            @Override
            public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
                censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
            }
        }, CompetitionInfo.class);
    }

    private Pool getSelectedPool() {
        if (poolListTable.getSelectedRows().length == 0) {
            return null;
        }
        int row = poolListTable.getSelectedRows()[0];
        row = poolListTable.getRowSorter().convertRowIndexToModel(row);
        Pool p = tableModel.getAtRow(row);

        return p;
    }

    private class PlayerListCellRenderer extends DefaultListCellRenderer {

        private Date censusDate;

        public PlayerListCellRenderer(Date censusDate) {
            super();
            this.censusDate = censusDate;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            final ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());
            if (value instanceof Player) {
                Player player = (Player) value;
                Pool pool = getSelectedPool();
                boolean ok = true;
                if (pool != null && pool.getID() != 0) {
                    ok = PoolChecker.checkPlayer(player, pool, censusDate, configurationFile);
                }
                String str = (ok ? "" : "(!) ") + player.getFirstName() + " " + player.getLastName() + "   (" + player.getTeam() + ")";
                JLabel label = (JLabel) super.getListCellRendererComponent(list, str, index, isSelected, hasFocus);
                if (player.getLockedStatus() != Player.LockedStatus.LOCKED) {
                    label.setIcon(Icons.UNLOCKED_PLAYER);
                } else {
                    label.setIcon(ok ? Icons.PLAYER : Icons.INVALID_PLAYER);
                }
                if (!ok) {
                    label.setForeground(Color.RED);
                }
                return label;
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        poolListTable = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        requestedList = new javax.swing.JList<Player>();
        approveButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        approvedList = new javax.swing.JList<Player>();
        jLabel2 = new javax.swing.JLabel();
        autoAssignButton = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        autoApproveButton = new javax.swing.JButton();
        lockButton = new javax.swing.JButton();
        customPoolButton = new javax.swing.JButton();
        drawWizardButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        customPoolButton1 = new javax.swing.JButton();

        setMinimumSize(new java.awt.Dimension(300, 0));

        poolListTable.setAutoCreateRowSorter(true);
        poolListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Pool", "# of Players", "Outstanding Requests"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        poolListTable.setGridColor(new java.awt.Color(237, 237, 237));
        poolListTable.setRowHeight(19);
        poolListTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                poolListTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(poolListTable);

        jLabel1.setText("Player Requests");

        requestedList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        requestedList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                requestedListMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(requestedList);

        approveButton.setText("Approve >");
        approveButton.setEnabled(false);
        approveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveButtonActionPerformed(evt);
            }
        });

        removeButton.setText("Remove");
        removeButton.setEnabled(false);
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        approvedList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        approvedList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                approvedListMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(approvedList);

        jLabel2.setText("Players in Division");

        autoAssignButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        autoAssignButton.setText("Auto Assign");
        autoAssignButton.setIconTextGap(8);
        autoAssignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoAssignButtonActionPerformed(evt);
            }
        });

        jToolBar1.setFloatable(false);

        autoApproveButton.setText("Auto Approve >");
        autoApproveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoApproveButtonActionPerformed(evt);
            }
        });

        lockButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/lock.png"))); // NOI18N
        lockButton.setText("Lock Division");
        lockButton.setIconTextGap(8);
        lockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockButtonActionPerformed(evt);
            }
        });

        customPoolButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wrench.png"))); // NOI18N
        customPoolButton.setText("Custom Division..");
        customPoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPoolButtonActionPerformed(evt);
            }
        });

        drawWizardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        drawWizardButton.setText("Draw Wizard");
        drawWizardButton.setFocusable(false);
        drawWizardButton.setIconTextGap(8);
        drawWizardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawWizardButtonActionPerformed(evt);
            }
        });

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        customPoolButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wrench.png"))); // NOI18N
        customPoolButton1.setText("Copy Division");
        customPoolButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPoolButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 761, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(removeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(approveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(autoApproveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 761, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(autoAssignButton)
                        .addGap(6, 6, 6)
                        .addComponent(customPoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(customPoolButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lockButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(printButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                        .addComponent(drawWizardButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(autoAssignButton)
                        .addComponent(customPoolButton)
                        .addComponent(customPoolButton1)
                        .addComponent(lockButton))
                    .addComponent(drawWizardButton)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(printButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(autoApproveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(approveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void autoApproveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoApproveButtonActionPerformed
        Pool pool = getSelectedPool();
        if (pool == null || pool.getID() == 0) {
            return;
        }
        if (!PermissionChecker.isAllowed(Action.APPROVE_DIVISION, database)) {
            return;
        }
        try {
            AutoAssign.autoApprovePlayers(database, pool);
        } catch (DatabaseStateException e) {
            GUIUtils.displayError(parentWindow, "Unabled to approve players: " + e.getMessage());
        }
    }//GEN-LAST:event_autoApproveButtonActionPerformed

    private void lockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockButtonActionPerformed
        Pool pool = getSelectedPool();
        if (pool != null && pool.getID() != 0) {
            if (!PermissionChecker.isAllowed(Action.LOCK_DIVISION, database)) {
                return;
            }
            if (database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true).isEmpty()) {
                JOptionPane.showMessageDialog(this.parentWindow, "Pool can not be locked without any approved players");
                return;
            }
            if (!GUIUtils.confirmLock(this.parentWindow, pool.getDescription())) {
                return;
            }

            try {
                PoolLocker.lockPoolPlayers(database, pool);
            } catch (DatabaseStateException e) {
                GUIUtils.displayError(parentWindow, e.getMessage());
            }
        }
    }//GEN-LAST:event_lockButtonActionPerformed

    private void autoAssignButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoAssignButtonActionPerformed
        if (JOptionPane.showConfirmDialog(
                parentWindow,
                "CAUTION: All players will be automatically assigned to all divisions they are eligible for.\n\nAre you sure?",
                "Confirm Auto Assign",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        if (!PermissionChecker.isAllowed(Action.AUTO_ASSIGN_DIVISIONS, database)) {
            return;
        }
        try {
            AutoAssign.assignPlayersToPools(database);
        } catch (DatabaseStateException e) {
            log.error(e);
            GUIUtils.displayError(parentWindow, "Assigning players to divisions failed: " + e.getMessage());
        }
    }//GEN-LAST:event_autoAssignButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        if (!PermissionChecker.isAllowed(Action.APPROVE_DIVISION, database)) {
            return;
        }
        Pool pool = getSelectedPool();
        if (pool == null || pool.getID() == 0) {
            return;
        }

        Player player;
        if (approvedList.getSelectedIndex() >= 0) {
            player = approvedListModel.getPlayerAt(approvedList.getSelectedIndex());
        } else if (requestedList.getSelectedIndex() >= 0) {
            player = requestedListModel.getPlayerAt(requestedList.getSelectedIndex());
        } else {
            return;
        }

        try {
            PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
            database.delete(pp);
        } catch (Exception e) {
            GUIUtils.displayError(this, "An error occured while removing the player from the selected division");
            log.error("Exception while removing player " + player.getID() + " from division " + pool.getID(), e);
        }
    }//GEN-LAST:event_removeButtonActionPerformed

    private void approvedListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_approvedListMouseClicked
        int index = approvedList.getSelectedIndex();
        //removeButton.setEnabled((index == -1)?false:true);
        //approveButton.setEnabled(false);
        if (evt.getClickCount() == 2 && index != -1) {
            if (!PermissionChecker.isAllowed(Action.OPEN_PLAYER, database)) {
                return;
            }
            Player player = approvedListModel.getPlayerAt(index);
            new PlayerDetailsDialog(parentWindow, true, database, notifier, player).setVisible(true);
        }
    }//GEN-LAST:event_approvedListMouseClicked

    private void approveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveButtonActionPerformed
        if (!PermissionChecker.isAllowed(Action.APPROVE_DIVISION, database)) {
            return;
        }
        Pool pool = getSelectedPool();
        if (pool == null) {
            return;
        }

        Collection<Player> players = new ArrayList<Player>();
        int[] indices = requestedList.getSelectedIndices();
        for (int index : indices) {
            players.add(requestedListModel.getPlayerAt(index));
        }


        for (Player player : players) {
            try {
                if (PoolChecker.checkPlayer(player, pool, censusDate, configurationFile)) {
                    PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                    pp.setApproved(true);
                    database.update(pp);
                }
            } catch (Exception e) {
                GUIUtils.displayError(this, "An error occured while approving the player for the selected division");
                log.error("Exception while updating requested/approved divisions for player " + player.getID(), e);
            }
        }
        //approveButton.setEnabled(false);
    }//GEN-LAST:event_approveButtonActionPerformed

    private void requestedListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_requestedListMouseClicked
        int index = requestedList.getSelectedIndex();
        approveButton.setEnabled((index == -1) ? false : true);
        if (evt.getClickCount() == 2 && index != -1) {
            if (!PermissionChecker.isAllowed(Action.OPEN_PLAYER, database)) {
                return;
            }
            Player player = requestedListModel.getPlayerAt(index);
            new PlayerDetailsDialog(parentWindow, true, database, notifier, player).setVisible(true);
        }
    }//GEN-LAST:event_requestedListMouseClicked

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        /* get all pools */
        List<Pool> pools = database.findAll(Pool.class, PoolDAO.ALL);

        /* remove pools with no players */
        Iterator<Pool> iter = pools.iterator();
        while (iter.hasNext()) {
            Pool nextPool = iter.next();
            if (database.findAll(Player.class, PlayerDAO.FOR_POOL, nextPool.getID(), false).isEmpty() && database.findAll(Player.class, PlayerDAO.FOR_POOL, nextPool.getID(), true).isEmpty()) {
                iter.remove();
            }
        }
        Collections.sort(pools, POOL_COMPARATOR);

        /* display pool selection dialog */
        CheckboxListDialog<Pool> cld = new CheckboxListDialog<Pool>(
                parentWindow, true, pools,
                "Select divisions to print", "Print Divisions");
        cld.setRenderer(new StringRenderer<Pool>() {

            public String asString(Pool p) {
                return p.getDescription();
            }
        }, Icons.POOL);
        cld.setVisible(true);

        /* print selected pools */
        if (cld.getSuccess() && !cld.getSelectedItems().isEmpty()) {
            new PoolListHTMLGenerator(database, cld.getSelectedItems()).openInBrowser();
        }
}//GEN-LAST:event_printButtonActionPerformed

private void poolListTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_poolListTableMouseClicked
        if (evt.getClickCount() == 2 && this.getSelectedPool() != null && this.getSelectedPool().getID() != 0) {
            if (!PermissionChecker.isAllowed(Action.EDIT_DIVISION, database)) {
                return;
            }
            new PoolDetailsDialog(parentWindow, true, database, this.getSelectedPool()).setVisible(true);
        }
}//GEN-LAST:event_poolListTableMouseClicked

private void customPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customPoolButtonActionPerformed
        if (!PermissionChecker.isAllowed(Action.ADD_DIVISION, database)) {
            return;
        }
        new PoolDetailsDialog(parentWindow, true, database, null).setVisible(true);
}//GEN-LAST:event_customPoolButtonActionPerformed

private void drawWizardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Pool pool = getSelectedPool();
        if (pool != null && pool.getID() != 0) {
            if (!PermissionChecker.isAllowed(Action.LOCK_DIVISION, database)) {
                return;
            }
            // TODO: disable the button instead of displaying this ugly message
            // .. or maybe we can start the wizard on a locked pool??
            if (pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED) {
                GUIUtils.displayMessage(null, "Please select an unlocked division", "Draw Wizard");
            } else {
                // TODO: it might be better if the draw wizard was modal, but I couldn't get
                // GUIUtils.runModalJFrame to work in this case.
                new DrawWizardWindow(database, notifier, getSelectedPool()).setVisible(true);
            }
        }
}//GEN-LAST:event_jButton1ActionPerformed

private void customPoolButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customPoolButton1ActionPerformed
        if (!PermissionChecker.isAllowed(Action.EDIT_DIVISION, database)) {
            return;
        }
        if (getSelectedPool() != null && getSelectedPool().getID() != 0) {
            Pool pool = getSelectedPool();

            final Pool poolCopy = new Pool();
            poolCopy.setDescription(pool.getDescription() + " Copy");
            poolCopy.setGender(pool.getGender());

            poolCopy.setMinimumAge(pool.getMinimumAge());
            poolCopy.setMaximumAge(pool.getMaximumAge());

            poolCopy.setMinimumWeight(pool.getMinimumWeight());
            poolCopy.setMaximumWeight(pool.getMaximumWeight());

            poolCopy.setMinimumGrade(pool.getMinimumGrade());
            poolCopy.setMaximumGrade(pool.getMaximumGrade());

            poolCopy.setMatchTime(pool.getMatchTime());
            poolCopy.setMinimumBreakTime(pool.getMinimumBreakTime());
            poolCopy.setGoldenScoreTime(pool.getGoldenScoreTime());
            final List<Player> selectedPlayers = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), false);
            selectedPlayers.addAll(database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true));
            database.perform(new Transaction() {

                @Override
                public void perform() {
                    database.add(poolCopy);

                    for (Player player : selectedPlayers) {
                        PlayerPool pp = new PlayerPool();
                        pp.setPlayerID(player.getID());
                        pp.setPoolID(poolCopy.getID());
                        database.add(pp);
                    }
                }
            });
        }
}//GEN-LAST:event_customPoolButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approveButton;
    private javax.swing.JList<Player> approvedList;
    private javax.swing.JButton autoApproveButton;
    private javax.swing.JButton autoAssignButton;
    private javax.swing.JButton customPoolButton;
    private javax.swing.JButton customPoolButton1;
    private javax.swing.JButton drawWizardButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton lockButton;
    private javax.swing.JTable poolListTable;
    private javax.swing.JButton printButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JList<Player> requestedList;
    // End of variables declaration//GEN-END:variables
}
