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

package au.com.jwatmuff.eventmanager.gui.wizard;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.gui.player.PlayerDetailsDialog;
import au.com.jwatmuff.eventmanager.gui.wizard.DrawWizardWindow.Context;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.misc.PoolChecker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import java.awt.Color;
import java.awt.Component;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerSelectionPanel extends javax.swing.JPanel implements DrawWizardWindow.Panel {
    private static final Logger log = Logger.getLogger(PlayerSelectionPanel.class);

    private static final Comparator<Player> PLAYERS_COMPARATOR = new Comparator<Player>() {
        @Override
        public int compare(Player p1, Player p2) {
            String n1 = p1.getLastName() + p1.getFirstName();
            String n2 = p2.getLastName() + p2.getFirstName();
            return n1.compareTo(n2);
        }
    };

    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private TransactionListener listener;
    private Pool pool;
    private List<Player> eligiblePlayers;
    private List<Player> playersInPool;
    private List<Player> approvedPlayersInPool;
    private List<Player> unapprovedPlayersInPool;
    private List<Team> teams;
    private CheckBoxListSelectionModel playerSelectionModel;
    private ListSelectionListener playerSelectionListener;
    private final Context context;

    private Date censusDate;
    private ConfigurationFile configurationFile;

    /** Creates new form PlayerSelectionPanel */
    @SuppressWarnings("unchecked")
    public PlayerSelectionPanel(final TransactionalDatabase database, TransactionNotifier notifier, Context context) {
        this.database = database;
        this.notifier = notifier;
        this.context = context;
        pool = context.pool;

        initComponents();

        teamList.setModel(new DefaultListModel<Team>());
        eligiblePlayerList.setModel(new DefaultListModel<Player>());
        playerList.setModel(new DefaultListModel());
        playerSelectionModel = playerList.getCheckBoxListSelectionModel();

        // Type cast remove compile warnings...
        eligiblePlayerList.setCellRenderer((ListCellRenderer<Object>)new PlayerListCellRenderer());
        playerList.setCellRenderer(new PlayerListCellRenderer());
        teamList.setCellRenderer(new TeamListCellRenderer());

        playerSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // TODO: this seems to get called twice when a team is selected and i'm not sure why
                if(e.getFirstIndex() < 0 || e.getLastIndex() < 0)
                    return;

                for(int index = 0; index <= playerList.getModel().getSize() - 1; index++) {
                    boolean selected = playerSelectionModel.isSelectedIndex(index);
                    Player player = (Player)playerList.getModel().getElementAt(index);

                    boolean ok = false;
                    if (pool != null && pool.getID() != 0) {
                        if (player.getLockedStatus() == Player.LockedStatus.LOCKED && PoolChecker.checkPlayer(player, pool, censusDate, configurationFile))
                            ok = true;
                    }

                    if(selected && unapprovedPlayersInPool.contains(player) && ok) {
                        unapprovedPlayersInPool.remove(player);
                        approvedPlayersInPool.add(player);
                    } else if(!selected && approvedPlayersInPool.contains(player)) {
                        approvedPlayersInPool.remove(player);
                        unapprovedPlayersInPool.add(player);
                    }
                }
                updatePlayerList();
            }
        };

        listener = new TransactionListener() {
            @Override
            public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //updateFromDatabase();
                    }
                });
            }
        };
        
        configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());
        pool = context.pool;
        divisionNameLabel.setText(pool.getDescription() + ": Player Selection");
        censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
        updateFromDatabase();
    }

    private void updateFromDatabase() {
        pool = database.get(Pool.class, pool.getID());
        eligiblePlayers = PoolChecker.findEligiblePlayers(pool, database);
        unapprovedPlayersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), false);
        approvedPlayersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
        playersInPool = new ArrayList<>();
        playersInPool.addAll(unapprovedPlayersInPool);
        playersInPool.addAll(approvedPlayersInPool);

        /* filter out eligible players that already belong to pool */
        Iterator<Player> iter = eligiblePlayers.iterator();
        while(iter.hasNext()) {
            Player p1 = iter.next();
            for(Player p2 : playersInPool)
                if(p1.getID().equals(p2.getID()))
                    iter.remove();
        }

        Collections.sort(playersInPool, PLAYERS_COMPARATOR);
        Collections.sort(eligiblePlayers, PLAYERS_COMPARATOR);

        calculateTeams();

        updateTeamList();
        updateEligiblePlayerList();
        updatePlayerList();
    }

    private void updateTeamList() {
        Team selectedTeam = getSelectedTeam();
        DefaultListModel<Team> model = (DefaultListModel<Team>)teamList.getModel();
        model.clear();
        model.setSize(teams.size());
        int i = 0;
        int newSelectIndex = 0;
        for(Team team : teams) {
            if(selectedTeam != null && selectedTeam.name.equals(team.name)) newSelectIndex = i;
            model.set(i++, team);
        }
        teamList.setSelectedIndex(newSelectIndex);
    }

    private void updateEligiblePlayerList() {
        @SuppressWarnings("unchecked")
        DefaultListModel<Player> model = (DefaultListModel<Player>)eligiblePlayerList.getModel();
        model.clear();

        for(Player player : eligiblePlayers)
            if(playerInTeam(player, getSelectedTeam()))
                model.addElement(player);
    }

    private void updatePlayerList() {
        @SuppressWarnings("unchecked")
        DefaultListModel<Player> model = (DefaultListModel<Player>)playerList.getModel();

        // we don't want to process list selection events while we are building
        // the player list
        playerSelectionModel.removeListSelectionListener(playerSelectionListener);

        model.clear();
        playerSelectionModel.clearSelection();

        for(Player player : playersInPool)
            if(playerInTeam(player, getSelectedTeam()))
                model.addElement(player);

        for(Player player : approvedPlayersInPool) {
            int index = model.indexOf(player);
            playerSelectionModel.addSelectionInterval(index, index);
        }

        // register for list selection events to detect when players are ticked
        playerSelectionModel.addListSelectionListener(playerSelectionListener);
    }

    private boolean playerInTeam(Player player, Team team) {
        if(team == null) return true;
        String team1 = team.name;
        if(team1.equals("All Teams")) return true;
        String team2 = player.getTeam();
        if(team2 == null || team2.isEmpty())
            return team1.equals("Other");
        else
            return team1.equals(team2);
    }

    private void calculateTeams() {
        HashMap<String,Team> teamMap = new HashMap<>();
        /* Make sure we have teams of all eligible players */
        for(Player player : eligiblePlayers) {
            String name = player.getTeam();
            if(name == null || name.isEmpty())
                continue;
            else if(!teamMap.containsKey(name))
                teamMap.put(name, new Team(name, 0));
        }

        /* Now collect team player counts of all players actually in each pool */
        int playersWithoutTeam = 0;
        for(Player player : playersInPool) {
            String name = player.getTeam();
            if(name == null || name.isEmpty())
                playersWithoutTeam++;
            else if(teamMap.containsKey(name))
                teamMap.get(name).numPlayers++;
            else
                teamMap.put(name, new Team(name, 1));
        }

        /* Now sort the teams alphabetically and prepend 'All Teams' / 'Other' */
        List<Team> result = new ArrayList<>();
        result.addAll(teamMap.values());
        Collections.sort(result);
        if(playersWithoutTeam > 0)
            result.add(0, new Team("Other", playersWithoutTeam));
        result.add(0, new Team("All Teams", playersInPool.size()));
        teams = result;
    }

    private Team getSelectedTeam() {
        return teamList.getSelectedValue();
    }


    private void addSelectedEligiblePlayers() {
        for(Player player : eligiblePlayerList.getSelectedValuesList()) {
            eligiblePlayers.remove(player);
            unapprovedPlayersInPool.add(player);
            playersInPool.add(player);
        }
        Collections.sort(playersInPool, PLAYERS_COMPARATOR);
        Collections.sort(eligiblePlayers, PLAYERS_COMPARATOR);
        updateEligiblePlayerList();
        updatePlayerList();

    }

    private void removeSelectedPlayers() {
        for(Object value : playerList.getSelectedValuesList()) {
            Player player = (Player) value;
            eligiblePlayers.add(player);
            unapprovedPlayersInPool.remove(player);
            approvedPlayersInPool.remove(player);
            playersInPool.remove(player);
        }
        Collections.sort(playersInPool, PLAYERS_COMPARATOR);
        Collections.sort(eligiblePlayers, PLAYERS_COMPARATOR);
        updateEligiblePlayerList();
        updatePlayerList();
    }

    @Override
    public boolean nextButtonPressed() {
        if(approvedPlayersInPool.size() == 0) {
            int status = JOptionPane.showConfirmDialog(
                null,
                "No players have been approved for this division. Are you sure you wish to continue?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);
            if (status != JOptionPane.YES_OPTION) return false;
        }
        context.players = new ArrayList<>(approvedPlayersInPool);
        context.unapprovedPlayers = new ArrayList<>(unapprovedPlayersInPool);
        updateSeeds();
        return true;
    }

    private void updateSeeds() {
        for(PlayerPool pp : database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, pool.getID())) {
            if(!context.seeds.containsKey(pp.getPlayerID()))
                context.seeds.put(pp.getPlayerID(), pp.getSeed());
        }
    }

    @Override
    public boolean backButtonPressed() {
        return true;
    }

    @Override
    public boolean closedButtonPressed() {
        return true;
    }

    @Override
    public void beforeShow() {
        this.notifier.addListener(listener, Player.class, Pool.class, PlayerPool.class);
    }

    @Override
    public void afterHide() {
        notifier.removeListener(listener);
    }

    private class Team implements Comparable<Team> {
        public String name;
        public int numPlayers;

        public Team(String name, int numPlayers) {
            this.name = name;
            this.numPlayers = numPlayers;
        }

        @Override
        public int compareTo(Team t) {
            return name.compareTo(t.name);
        }

        @Override
        public String toString() {
            return name;// + " (" + numPlayers + ")";
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

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        approveAllPlayersCheckBox = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        playerList = new com.jidesoft.swing.CheckBoxList();
        removePlayerButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        eligiblePlayerList = new javax.swing.JList<Player>();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        teamList = new javax.swing.JList<Team>();
        addPlayerButton = new javax.swing.JButton();
        divisionNameLabel = new javax.swing.JLabel();

        jLabel1.setText("Registered Players");

        approveAllPlayersCheckBox.setText("Approve all");
        approveAllPlayersCheckBox.setMargin(new java.awt.Insets(2, 4, 2, 2));
        approveAllPlayersCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveAllPlayersCheckBoxActionPerformed(evt);
            }
        });

        playerList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        playerList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playerListMouseClicked(evt);
            }
        });
        playerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                playerListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(playerList);

        removePlayerButton.setText("< Remove Player");
        removePlayerButton.setEnabled(false);
        removePlayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePlayerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(approveAllPlayersCheckBox)
                            .addComponent(removePlayerButton)
                            .addComponent(jLabel1))
                        .addContainerGap(198, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(approveAllPlayersCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(removePlayerButton)
                .addContainerGap())
        );

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Filter by Team");

        eligiblePlayerList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        eligiblePlayerList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                eligiblePlayerListMouseClicked(evt);
            }
        });
        eligiblePlayerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                eligiblePlayerListValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(eligiblePlayerList);

        jLabel3.setText("Eligible Players");

        teamList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        teamList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        teamList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                teamListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(teamList);

        addPlayerButton.setText("Add Player >");
        addPlayerButton.setEnabled(false);
        addPlayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPlayerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addPlayerButton))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addPlayerButton)
                .addContainerGap())
        );

        divisionNameLabel.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        divisionNameLabel.setText("Division Name");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(divisionNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 605, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(divisionNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void teamListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_teamListValueChanged
        updateEligiblePlayerList();
        updatePlayerList();
    }//GEN-LAST:event_teamListValueChanged

    private void eligiblePlayerListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_eligiblePlayerListMouseClicked
        int index = eligiblePlayerList.getSelectedIndex();
        if(evt.getClickCount() == 1) {
            addPlayerButton.setEnabled((index == -1)?false:true);
        }
        
        if(evt.getClickCount() == 2 && index != -1) {
            if(!PermissionChecker.isAllowed(Action.OPEN_PLAYER, database)) return;
            Player player = eligiblePlayerList.getSelectedValue();
            new PlayerDetailsDialog(null, true, database, notifier, player).setVisible(true);
        }
    }//GEN-LAST:event_eligiblePlayerListMouseClicked

    private void approveAllPlayersCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveAllPlayersCheckBoxActionPerformed
        if(approveAllPlayersCheckBox.isSelected()) {
            playerSelectionModel.setSelectionInterval(0, playerList.getModel().getSize() - 1);
        } else {
            playerSelectionModel.clearSelection();
        }
    }//GEN-LAST:event_approveAllPlayersCheckBoxActionPerformed

    private void addPlayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPlayerButtonActionPerformed
        addSelectedEligiblePlayers();
    }//GEN-LAST:event_addPlayerButtonActionPerformed

    private void eligiblePlayerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_eligiblePlayerListValueChanged
        addPlayerButton.setEnabled(eligiblePlayerList.getSelectedIndex() != -1);
    }//GEN-LAST:event_eligiblePlayerListValueChanged

    private void playerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_playerListValueChanged
        removePlayerButton.setEnabled(playerList.getSelectedIndex() != -1);
    }//GEN-LAST:event_playerListValueChanged

    private void removePlayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePlayerButtonActionPerformed
        removeSelectedPlayers();
    }//GEN-LAST:event_removePlayerButtonActionPerformed

    private void playerListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListMouseClicked
        int index = playerList.getSelectedIndex();
        if(evt.getClickCount() == 1) {
            removePlayerButton.setEnabled((index == -1)?false:true);
        }

        if(evt.getClickCount() == 2 && index != -1) {
            if(!PermissionChecker.isAllowed(Action.OPEN_PLAYER, database)) return;
            Player player = (Player) playerList.getSelectedValue();
            new PlayerDetailsDialog(null, true, database, notifier, player).setVisible(true);
        }
    }//GEN-LAST:event_playerListMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPlayerButton;
    private javax.swing.JCheckBox approveAllPlayersCheckBox;
    private javax.swing.JLabel divisionNameLabel;
    private javax.swing.JList<Player> eligiblePlayerList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private com.jidesoft.swing.CheckBoxList playerList;
    private javax.swing.JButton removePlayerButton;
    private javax.swing.JList<Team> teamList;
    // End of variables declaration//GEN-END:variables

    private class PlayerListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            if (value instanceof Player) {
                Player player = (Player) value;
                boolean ok = true;
                if (pool != null && pool.getID() != 0) {
                    ok = PoolChecker.checkPlayer(player, pool, censusDate, configurationFile);
                }
                String str =  player.getLastName() + ", " + player.getFirstName();
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

    private class TeamListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            label.setIcon(Icons.TEAM);
            return label;
        }
    }
}
