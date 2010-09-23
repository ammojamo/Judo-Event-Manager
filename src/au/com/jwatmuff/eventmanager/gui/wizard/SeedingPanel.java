/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SeedingPanel.java
 *
 * Created on 14/09/2010, 8:25:15 PM
 */

package au.com.jwatmuff.eventmanager.gui.wizard;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.gui.wizard.DrawWizardWindow.Context;
import au.com.jwatmuff.eventmanager.model.draw.DrawConfiguration;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class SeedingPanel extends javax.swing.JPanel implements DrawWizardWindow.Panel {
    private static final Logger log = Logger.getLogger(SeedingPanel.class);

    private DefaultTableModel model;
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private TransactionListener listener;
    private Pool pool;
    private List<PlayerPoolInfo> players = new ArrayList<PlayerPoolInfo>();
    private Map<PlayerPoolInfo, Integer> seeds = new HashMap<PlayerPoolInfo, Integer>();
    private Context context;

    /** Creates new form SeedingPanel */
    public SeedingPanel(TransactionalDatabase database, TransactionNotifier notifier, Context context) {
        this.database = database;
        this.notifier = notifier;
        this.context = context;

        initComponents();

        model = new DefaultTableModel();
        model.addColumn("Player");
        model.addColumn("Team");
        model.addColumn("Seed");

        model.setColumnIdentifiers(new Object[] { "Player", "Team", "Seed" });

        seedingTable.setModel(model);

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

    private TableCellEditor getSeedingCellEditor(int numPlayers) {
        Object[] values =  new Object[numPlayers + 1];
        values[0] = "None";
        for(int i = 1; i <= numPlayers; i++) values[i] = "" + i;

        return new DefaultCellEditor(new JComboBox(values));
    }

    private void updateFromDatabase() {
        List<PlayerPoolInfo> newPlayers = PoolPlayerSequencer.getPlayerSequence(database, pool.getID());

        seedingTable.getColumn("Seed").setCellEditor(getSeedingCellEditor(newPlayers.size()));

        // step 1. delete old players and update existing players
        int row = 0;
        Iterator<PlayerPoolInfo> iterator = players.iterator();
        while(iterator.hasNext()) {
            PlayerPoolInfo player = iterator.next();
            if(!newPlayers.contains(player)) {
                model.removeRow(row);
                seeds.remove(player);
                iterator.remove();
            }
            else {
                Object[] rowData = getRowData(player);
                for(int col = 0; col < 3; col++)
                    model.setValueAt(rowData[col], row, col);
                row++;
                newPlayers.remove(player);
            }
        }
        
        // step 2. add any new players
        for(PlayerPoolInfo player : newPlayers) {
            players.add(player);
            model.addRow(getRowData(player));
        }
    }

    private Object[] getRowData(PlayerPoolInfo player) {
        PlayerDetails playerDetails = database.get(PlayerDetails.class, player.getPlayer().getDetailsID());
        return new Object[] {
            player.getPlayer().getLastName() + ", " + player.getPlayer().getFirstName(),
            playerDetails.getClub(),
            seeds.get(player) == null ? "None" : seeds.get(player)
        };
    }

    private int getNumberOfPlayerPositions() {
        List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        int max = 0;
        for(Fight fight : fights) {
            for(String code : fight.getPlayerCodes()) {
                if(code.startsWith("P")) {
                    try {
                        max = Math.max(max, Integer.parseInt(code.substring(1)));
                    } catch(NumberFormatException e) {
                        log.warn("Number format exception while parsing code: " + code);
                    }
                }
            }
        }
        return max;
    }

    private List<PlayerPoolInfo> orderPlayers() {
        /********************************************************************************/
        /* Ordering of players based on seeds - TODO: it might be possible to simplify this */
        /* TODO: if this code needs to be used elsewhere, it should be moved to a shared utility class */
        /********************************************************************************/

        int numPlayers = getNumberOfPlayerPositions();

        // create a list of all players, unordered
        List<PlayerPoolInfo> unorderedPlayers = new ArrayList<PlayerPoolInfo>(players);
        // add null (bye) players to fill in all available position in the
        while(unorderedPlayers.size() < numPlayers)
            unorderedPlayers.add(null);
        // create a list to hold the players after they have been
        List<PlayerPoolInfo> orderedPlayers = new ArrayList<PlayerPoolInfo>();

        // get the set of all seeds specified, in order from lowest to highest
        List<Integer> seedSet = new ArrayList<Integer>();
        for(Integer seed : seeds.values())
            if(seed != null && !seedSet.contains(seed)) seedSet.add(seed);
        Collections.sort(seedSet);

        // build up ordered list of players from those players that had seeds specified
        for(Integer seed : seedSet) {
            for(PlayerPoolInfo player : seeds.keySet()) {
                if(seeds.get(player) == seed) {
                    unorderedPlayers.remove(player);
                    orderedPlayers.add(player);
                }
            }
        }

        // randomize remaining unordered players, and add them to the ordered players
        Collections.shuffle(unorderedPlayers);
        orderedPlayers.addAll(unorderedPlayers);

        return orderedPlayers;
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
        seedingTable = new javax.swing.JTable();
        divisionNameLabel = new javax.swing.JLabel();

        seedingTable.setModel(new javax.swing.table.DefaultTableModel(
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
        seedingTable.setGridColor(new java.awt.Color(237, 237, 237));
        seedingTable.setRowHeight(19);
        jScrollPane1.setViewportView(seedingTable);

        divisionNameLabel.setFont(new java.awt.Font("Tahoma", 1, 24));
        divisionNameLabel.setText("Division Name");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                    .addComponent(divisionNameLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(divisionNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel divisionNameLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable seedingTable;
    // End of variables declaration//GEN-END:variables

    @Override
    public boolean nextButtonPressed() {
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        DrawConfiguration drawConfig = DrawConfiguration.getDrawConfiguration(ci.getDrawConfiguration());
        if(drawConfig == null) {
            GUIUtils.displayError(this, "Unable to load draw configuration.");
            return false;
        }

        String drawName = drawConfig.getDrawName(players.size());
        if(drawName == null) {
            GUIUtils.displayError(this, "The current draw configuration does not support divisions with " + players.size() + " players");
            return false;
        }

        File csvFile = new File("resources/draw/" + drawName + ".csv");

        try {
            CSVImporter.importFightDraw(csvFile, database, pool, players.size());
        } catch(Exception e) {
            GUIUtils.displayError(this, "Failed to import fight draw (" + drawName + ")");
            log.error("Error importing fight draw", e);
        }

        // construct list of ordered players based on seeds, with null entries to
        // represent bye players
        List<PlayerPoolInfo> orderedPlayers = orderPlayers();

        PoolPlayerSequencer.savePlayerSequence(database, pool.getID(), orderedPlayers);

        return true;
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
        pool = context.pool;
        divisionNameLabel.setText(pool.getDescription() + ": Seeding");
        updateFromDatabase();
        notifier.addListener(listener, Pool.class, PlayerPool.class);
    }

    @Override
    public void afterHide() {
        notifier.removeListener(listener);
    }
}
