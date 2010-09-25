/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PlayerSelectionPanel.java
 *
 * Created on 18/08/2010, 7:22:46 PM
 */

package au.com.jwatmuff.eventmanager.gui.wizard;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.gui.wizard.DrawWizardWindow.Context;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PoolChecker;
import au.com.jwatmuff.eventmanager.model.misc.PoolLocker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
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

    /** Creates new form PlayerSelectionPanel */
    public PlayerSelectionPanel(final TransactionalDatabase database, TransactionNotifier notifier, Context context) {
        this.database = database;
        this.notifier = notifier;
        this.context = context;
        pool = context.pool;

        initComponents();

        teamList.setModel(new DefaultListModel());
        eligiblePlayerList.setModel(new DefaultListModel());
        playerList.setModel(new DefaultListModel());
        playerSelectionModel = playerList.getCheckBoxListSelectionModel();

        eligiblePlayerList.setCellRenderer(new PlayerListCellRenderer());
        playerList.setCellRenderer(new PlayerListCellRenderer());
        teamList.setCellRenderer(new TeamListCellRenderer());

        playerSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // TODO: this seems to get called twice when a team is selected and i'm not sure why
                final List<PlayerPool> playerPoolsToUpdate = new ArrayList<PlayerPool>();
                if(e.getFirstIndex() < 0 || e.getLastIndex() < 0)
                    return;

                for(int index = e.getFirstIndex(); index <= e.getLastIndex(); index++) {
                    boolean selected = playerSelectionModel.isSelectedIndex(index);
                    Player player = (Player)playerList.getModel().getElementAt(index);
                    if(selected && unapprovedPlayersInPool.contains(player)) {
                        // approve player
                        PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                        pp.setApproved(true);
                        playerPoolsToUpdate.add(pp);
                    } else if(!selected && approvedPlayersInPool.contains(player)) {
                        // unapprove player
                        PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                        pp.setApproved(false);
                        playerPoolsToUpdate.add(pp);
                    }
                }
                try {
                    if(!playerPoolsToUpdate.isEmpty()) {
                        database.perform(new Transaction() {
                            @Override
                            public void perform() {
                                for(PlayerPool pp : playerPoolsToUpdate) {
                                    database.update(pp);
                                }
                            }
                        });
                    }
                } catch(Exception ex) {
                    log.error("Exception updating player pool", ex);
                }
            }
        };

        listener = new TransactionListener() {
            @Override
            public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateFromDatabase();
                    }
                });
            }
        };
        
    }

    private void updateFromDatabase() {
        pool = database.get(Pool.class, pool.getID());
        eligiblePlayers = PoolChecker.findEligiblePlayers(pool, database);
        unapprovedPlayersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), false);
        approvedPlayersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
        playersInPool = new ArrayList<Player>();
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
        DefaultListModel model = (DefaultListModel)teamList.getModel();
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
        DefaultListModel model = (DefaultListModel)eligiblePlayerList.getModel();
        model.clear();

        for(Player player : eligiblePlayers)
            if(playerInTeam(player, getSelectedTeam()))
                model.addElement(player);
    }

    private void updatePlayerList() {
        DefaultListModel model = (DefaultListModel)playerList.getModel();

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
        PlayerDetails details = database.get(PlayerDetails.class, player.getDetailsID());
        if(details == null) return false;
        String team2 = details.getClub();
        if(team2 == null || team2.isEmpty())
            return team1.equals("Other");
        else
            return team1.equals(team2);
    }

    private void calculateTeams() {
        HashMap<String,Team> teamMap = new HashMap<String,Team>();
        /* Make sure we have teams of all eligible players */
        for(Player player : eligiblePlayers) {
            PlayerDetails details = database.get(PlayerDetails.class, player.getDetailsID());
            String name = details.getClub();
            if(name == null || name.isEmpty())
                continue;
            else if(!teamMap.containsKey(name))
                teamMap.put(name, new Team(name, 0));
        }

        /* Now collect team player counts of all players actually in each pool */
        int playersWithoutTeam = 0;
        for(Player player : playersInPool) {
            PlayerDetails details = database.get(PlayerDetails.class, player.getDetailsID());
            String name = details.getClub();
            if(name == null || name.isEmpty())
                playersWithoutTeam++;
            else if(teamMap.containsKey(name))
                teamMap.get(name).numPlayers++;
            else
                teamMap.put(name, new Team(name, 1));
        }

        /* Now sort the teams alphabetically and prepend 'All Teams' / 'Other' */
        List<Team> result = new ArrayList<Team>();
        result.addAll(teamMap.values());
        Collections.sort(result);
        if(playersWithoutTeam > 0)
            result.add(0, new Team("Other", playersWithoutTeam));
        result.add(0, new Team("All Teams", playersInPool.size()));
        teams = result;
    }

    private Team getSelectedTeam() {
        return (Team)teamList.getSelectedValue();
    }


    private void addSelectedEligiblePlayers() {
        database.perform(new Transaction() {
            @Override
            public void perform() {
                for(Object value : eligiblePlayerList.getSelectedValues()) {
                    Player player = (Player) value;
                    if (player != null) {
                        PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                        if (pp == null || !pp.isValid()) {
                            pp = new PlayerPool();
                            pp.setPlayerID(player.getID());
                            pp.setPoolID(pool.getID());
                            pp.setApproved(false);
                            database.add(pp);
                        }
                    }
                }
            }
        });
    }

    private void removeSelectedPlayers() {
        database.perform(new Transaction() {
            @Override
            public void perform() {
                for(Object value : playerList.getSelectedValues()) {
                    Player player = (Player) value;
                    if(player != null) {
                        PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                        if(pp != null || pp.isValid())
                            database.delete(pp);
                    }
                }
            }       
        });
    }

    @Override
    public boolean nextButtonPressed() {
        if(GUIUtils.confirmLock(null, "division")) {
            try {
                context.pool = PoolLocker.lockPoolPlayers(database, pool);
                return true;
            } catch(DatabaseStateException e) {
                GUIUtils.displayError(null, e.getMessage());
            }
        }
        return false;
    }

    @Override
    public boolean backButtonPressed() {
        log.error("Back button pressed on first panel of draw wizard!");
        return false;
    }

    @Override
    public boolean closedButtonPressed() {
        return true;
    }

    @Override
    public void beforeShow() {
        pool = context.pool;
        divisionNameLabel.setText(pool.getDescription() + ": Player Selection");
        censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
        updateFromDatabase();
        this.notifier.addListener(listener, Player.class, PlayerDetails.class, Pool.class, PlayerPool.class);
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
        eligiblePlayerList = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        teamList = new javax.swing.JList();
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(approveAllPlayersCheckBox)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(removePlayerButton)
                        .addContainerGap(181, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addContainerGap(206, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(approveAllPlayersCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
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
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addContainerGap())
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addPlayerButton))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addContainerGap())
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
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
                    .addComponent(divisionNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 586, Short.MAX_VALUE))
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
        if(evt.getClickCount() == 2) {
            addSelectedEligiblePlayers();
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPlayerButton;
    private javax.swing.JCheckBox approveAllPlayersCheckBox;
    private javax.swing.JLabel divisionNameLabel;
    private javax.swing.JList eligiblePlayerList;
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
    private javax.swing.JList teamList;
    // End of variables declaration//GEN-END:variables

    private class PlayerListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            if (value instanceof Player) {
                Player player = (Player) value;
                boolean ok = true;
                if (pool != null && pool.getID() != 0) {
                    ok = PoolChecker.checkPlayer(player, pool, censusDate);
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
