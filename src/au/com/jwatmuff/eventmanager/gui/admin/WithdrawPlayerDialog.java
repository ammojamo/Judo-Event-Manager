/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * WithdrawPlayerDialog.java
 *
 * Created on 26/01/2011, 10:15:50 PM
 */
package au.com.jwatmuff.eventmanager.gui.admin;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool.Status;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.DistributableBeanTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.JexlBeanTableModel;
import au.com.jwatmuff.eventmanager.util.gui.ComboBoxCellRenderer;
import au.com.jwatmuff.eventmanager.util.gui.NullSelectionModel;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class WithdrawPlayerDialog extends javax.swing.JDialog {

    private static final Logger log = Logger.getLogger(WithdrawPlayerDialog.class);
    private Database database;
    private TransactionNotifier notifier;
    private DivisionTableModel divisionTableModel;
    private PlayerTableModel playerTableModel;
    /* statuses which can be chosen by the user */
    private Status[] statuses = {
        Status.OK,
        Status.WITHDRAWN,
        Status.DISQUALIFIED
    };

    /** Creates new form WithdrawPlayerDialog */
    public WithdrawPlayerDialog(java.awt.Frame parent, boolean modal, Database database, TransactionNotifier notifier) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);

        this.database = database;
        this.notifier = notifier;

        divisionTableModel = new DivisionTableModel();
        divisionTableModel.updateFromDatabase();
        divisionTable.setModel(divisionTableModel);
        divisionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        divisionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                playerTableModel.updateFromDatabase();
            }
        });
        notifier.addListener(divisionTableModel, Pool.class);

        playerTableModel = new PlayerTableModel();
        playerTable.setModel(playerTableModel);
        playerTable.setSelectionModel(new NullSelectionModel()); // disable selection
        // set up cell editor for status column
        // see PlayerTableModel.setValueAt() for how edits to cells are handled
        playerTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox(statuses)));
        playerTable.getColumnModel().getColumn(1).setCellRenderer(new ComboBoxCellRenderer(Status.values()));
        // sort by name
        playerTable.getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        notifier.addListener(playerTableModel, PlayerPool.class);
    }

    private Pool getSelectedDivision() {
        int row = divisionTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        //row = divisionTable.getRowSorter().convertRowIndexToModel(row);
        return divisionTableModel.getAtRow(row);
    }

    // TODO: move to utility class
    private boolean hasMissedFights(PlayerPool playerPool) {
        // TODO: move this to utility class
        List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, playerPool.getPoolID());
        if (fights.isEmpty()) {
            return false;
        }

        // workout what player number we are
        // maybe we can just use playerPool.getPlayerPosition() + 1, but I wasn't 100% sure without spending time more time to check
        List<PlayerPoolInfo> playerList = PoolPlayerSequencer.getPlayerSequence(database, playerPool.getPoolID());
        int position = -1;
        for (PlayerPoolInfo playerPoolInfo : playerList) {
            if (playerPoolInfo.getPlayer().getID().equals(playerPool.getPlayerID())) {
                position = playerList.indexOf(playerPoolInfo) + 1;
                break;
            }
        }

        // not sure if this can ever happen, but it's probably safe to assume the player hasn't missed any fights
        if (position == -1) {
            return false;
        }

        PlayerCodeParser playerCodeParser = PlayerCodeParser.getInstance(database, playerPool.getPoolID());

        // now we are looking for fights that may involve our player
        // i.e. ewith code Pn where n = position
        String playerCode = "P" + position;
        boolean previousNonByeFightNotFought = false;
        for (Fight fight : fights) {
            if(previousNonByeFightNotFought) {
                List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, fight.getID());
                if(!results.isEmpty()) {
                    // we have missed a fight because this fight has been fought, and a previous
                    // fight involving our player vs a non-bye player was not fought! phew!
                    return true;
                }
            } else {
                for (int i = 0; i < 2; i++) {
                    String code = fight.getPlayerCodes()[i];
                    if (playerCode.equals(code)) {
                        // if this fight has no result then it may have been missed
                        List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, fight.getID());
                        if (!results.isEmpty()) {
                            // was the other player was a bye/disqualified/etc?
                            FightPlayer opponent = playerCodeParser.parseCode(fight.getPlayerCodes()[1 - i]);
                            if (opponent.type != PlayerType.NORMAL) {
                                previousNonByeFightNotFought = true;
                            }
                        }
                    }
                }
            }
        }

        // yay, we didn't find any missed fights
        return false;
    }

    private class PlayerTableModel extends BeanMapperTableModel<PlayerPool> implements TransactionListener {

        public PlayerTableModel() {
            super();
            this.addColumn("Player", "player");
            this.addColumn("Status", "status");
            this.setBeanMapper(new BeanMapper<PlayerPool>() {

                public Map<String, Object> mapBean(PlayerPool bean) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    Player player = database.get(Player.class, bean.getPlayerID());
                    row.put("player", player.getLastName() + ", " + player.getFirstName());
                    row.put("status", bean.getStatus());
                    return row;
                }
            });
        }

        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            updateFromDatabase();
        }

        public void updateFromDatabase() {
            Pool division = getSelectedDivision();
            List<PlayerPool> playerPools;
            if (division == null) {
                playerPools = Collections.<PlayerPool>emptyList();
            } else {
                playerPools = database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, division.getID());
                CollectionUtils.filter(playerPools, new Predicate() {
                    public boolean evaluate(Object o) {
                        return (o instanceof PlayerPool) && ((PlayerPool)o).isApproved();
                    }
                });
            }
            playerTableModel.setBeans(playerPools);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // make status column editable
            return columnIndex == 1;
        }

        // this gets called when a status is changed by the user
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != 1 || !(value instanceof Status)) {
                log.warn("This should never happen");
                return;
            }
            PlayerPool playerPool = getAtRow(rowIndex);
            Status status = (Status) value;
            if (playerPool.getStatus() != status) {
                if (status == Status.OK && hasMissedFights(playerPool)) {
                    // we are going from NOT OK to OK - must check if this player has missed any fights
                    GUIUtils.displayMessage(null, "This player has already missed fights in this division.", "Cannot change status");
                } else {
                    playerPool.setStatus(status);
                    database.update(playerPool);
                }
            }
        }
    }

    private class DivisionTableModel extends JexlBeanTableModel<Pool> implements TransactionListener {

        DistributableBeanTableModel<Pool> dbtm;

        public DivisionTableModel() {
            super();
            this.addColumn("Division", "description");
            dbtm = new DistributableBeanTableModel<Pool>(this);
        }

        public void updateFromDatabase() {
            Collection<Pool> divisions = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.PLAYERS_LOCKED);
            divisions.addAll(database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED));
            dbtm.setBeans(divisions);
        }

        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            updateFromDatabase();
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

        jScrollPane1 = new javax.swing.JScrollPane();
        divisionTable = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        playerTable = new javax.swing.JTable();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Withdraw/Disqualify Players");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        divisionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Division"
            }
        ));
        divisionTable.setGridColor(new java.awt.Color(237, 237, 237));
        divisionTable.setRowHeight(19);
        jScrollPane1.setViewportView(divisionTable);

        playerTable.setAutoCreateRowSorter(true);
        playerTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Player", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        playerTable.setGridColor(new java.awt.Color(237, 237, 237));
        playerTable.setRowHeight(19);
        jScrollPane2.setViewportView(playerTable);

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(closeButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_closeButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        notifier.removeListener(divisionTableModel);
        notifier.removeListener(playerTableModel);
    }//GEN-LAST:event_formWindowClosed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JTable divisionTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable playerTable;
    // End of variables declaration//GEN-END:variables
}
