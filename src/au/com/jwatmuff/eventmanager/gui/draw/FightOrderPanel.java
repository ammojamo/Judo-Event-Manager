/*
 * FightOrderPanel.java
 *
 * Created on 2 August 2008, 03:24
 */

package au.com.jwatmuff.eventmanager.gui.draw;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter.TooFewPlayersException;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PoolLocker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.print.MultipleDrawHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.CheckboxListDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class FightOrderPanel extends javax.swing.JPanel {
    private static final Logger log = Logger.getLogger(FightOrderPanel.class);

    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    
    private LockedPoolListTableModel poolTableModel;
    private PlayerTableModel playerTableModel;
    private FightTableModel fightTableModel;

    private JFrame parentWindow;
    
    public static final Comparator<Pool> POOL_COMPARATOR = new Comparator<Pool>() {
        @Override
        public int compare(Pool p1, Pool p2) {
            if(p1.getMaximumAge() == p2.getMaximumAge()){
                if(p2.getGender().equals(p1.getGender())){
                    if(p1.getMaximumWeight() == p2.getMaximumWeight()){
                        if(p1.getMinimumWeight() == p2.getMinimumWeight()){
                            return  p1.getDescription().compareTo(p2.getDescription());
                        } else {
                            if(p1.getMinimumWeight() == 0) {
                                return 1;
                            } else if(p2.getMinimumWeight() == 0) {
                                return -1;
                            } else {
                                return -Double.compare(p1.getMinimumWeight(), p2.getMinimumWeight());
                            }
                        }
                    } else {
                        if(p1.getMaximumWeight() == 0) {
                            return 1;
                        } else if(p2.getMaximumWeight() == 0) {
                            return -1;
                        } else {
                            return Double.compare(p1.getMaximumWeight(), p2.getMaximumWeight());
                        }
                    }
                } else {
                    return  p2.getGender().compareTo(p1.getGender());
                }
            } else {
                if(p1.getMaximumAge() == 0) {
                    return 1;
                } else if(p2.getMaximumAge() == 0) {
                    return -1;
                } else {
                    return p1.getMaximumAge() - p2.getMaximumAge();
                }
            }
        }
    };

    

    /** Creates new form FightOrderPanel */
    public FightOrderPanel() {
        initComponents();
    }
    
    public void setParentWindow(JFrame parentWindow) {
        this.parentWindow = parentWindow;
    }

    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }

    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void afterPropertiesSet() {
        /**** Set up pool table ****/
        poolTableModel = new LockedPoolListTableModel(notifier);
        poolTable.setModel(poolTableModel);
        poolTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);                
        poolTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Pool pool = getSelectedPool();
                if(pool != null) {
                    boolean locked = (pool.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED);
                    playerTable.setEnabled(!locked);
                    fightTable.setEnabled(!locked);
                    autoAssignButton.setEnabled(!locked);
                    lockButton.setEnabled(!locked);
                    importButton.setEnabled(!locked);
                    upButton.setEnabled(!locked);
                    downButton.setEnabled(!locked);
                }
                playerTableModel.updateFromDatabase();
                fightTableModel.updateFromDatabase();
            }
        });
        
        /**** Set up player table ****/
        playerTableModel = new PlayerTableModel(database, notifier) {
            @Override
            public Pool getPool() {
                return getSelectedPool();
            }
        };
        playerTable.setModel(playerTableModel);
        playerTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        
        /**** Set up fight table ****/
        fightTableModel = new FightTableModel(database, notifier) {
            @Override
            public Pool getPool() {
                return getSelectedPool();
            }
        };

        fightTable.setModel(fightTableModel);
        fightTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fightTableModel.updateFromDatabase();

        GUIUtils.leftAlignTable(poolTable);
        GUIUtils.leftAlignTable(fightTable);
    }
    
    private Pool getSelectedPool() {
        int row = poolTable.getSelectedRow();
        if(row >= 0) {
            row = poolTable.getRowSorter().convertRowIndexToModel(row);
            return poolTableModel.getAtRow(row);
        } else {
            return null;
        }
        
    }
    
    private class LockedPoolListTableModel extends BeanMapperTableModel<Pool> implements TransactionListener {
        public LockedPoolListTableModel(TransactionNotifier notifier) {
            super();
            notifier.addListener(this, Pool.class, Player.class, PlayerPool.class, Fight.class);
            
            setBeanMapper(new BeanMapper<Pool>() {
                @Override
                public Map<String, Object> mapBean(Pool p) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("description", p.getDescription());
                    int playersInPool = database.findAll(Player.class, PlayerDAO.FOR_POOL, p.getID(), true).size();
                    map.put("playersInPool", playersInPool);
                    PlayerCodeParser parser = PlayerCodeParser.getInstance(database, p.getID());
                    List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, p.getID());
                    int fightsInPool = parser.totalNumberOfFights();
                    int nonTieBreakFightsInPool = parser.minimumNumberOfFights();
                    map.put("maxFightsInPool", fightsInPool);
                    map.put("minFightsInPool", nonTieBreakFightsInPool);
                    map.put("fightsLocked", p.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED);
                    return map;
                }
            });
            addColumn("Division", "description");
            addColumn("Players In Division", "playersInPool");
            addColumn("Fights In Draw", "maxFightsInPool");
            addColumn("Minimum Fights In Division", "minFightsInPool");
            addColumn("Fights Locked", "fightsLocked");
            updateTableFromDatabase();
        }

        public void updateTableFromDatabase() {
            int index = poolTable.getSelectedRow();
            Collection<Pool> pools = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.PLAYERS_LOCKED);
            pools.addAll(database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED));
            setBeans(pools);
            // restore selection
            if((index >= 0) && (index < pools.size()))
                poolTable.setRowSelectionInterval(index, index);
        }

        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            updateTableFromDatabase();
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
        poolTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        playerTable = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        printButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane3 = new javax.swing.JScrollPane();
        fightTable = new javax.swing.JTable();
        autoAssignButton = new javax.swing.JButton();
        lockButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        upButton = new javax.swing.JButton();

        jScrollPane1.setDoubleBuffered(true);

        poolTable.setAutoCreateRowSorter(true);
        poolTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Pool"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        poolTable.setGridColor(new java.awt.Color(204, 204, 204));
        poolTable.setRowHeight(19);
        jScrollPane1.setViewportView(poolTable);

        playerTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Player"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        playerTable.setGridColor(new java.awt.Color(204, 204, 204));
        playerTable.setRowHeight(19);
        jScrollPane2.setViewportView(playerTable);

        jToolBar1.setFloatable(false);

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(printButton);

        fightTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Fight"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        fightTable.setGridColor(new java.awt.Color(204, 204, 204));
        fightTable.setRowHeight(19);
        jScrollPane3.setViewportView(fightTable);

        autoAssignButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        autoAssignButton.setText("Auto Generate");
        autoAssignButton.setIconTextGap(8);
        autoAssignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoAssignButtonActionPerformed(evt);
            }
        });

        lockButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/lock.png"))); // NOI18N
        lockButton.setText("Lock Draw");
        lockButton.setIconTextGap(8);
        lockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockButtonActionPerformed(evt);
            }
        });

        importButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        importButton.setText("Import Draw..");
        importButton.setIconTextGap(8);
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        downButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_down.png"))); // NOI18N
        downButton.setText("Player Down");
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });

        upButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_up.png"))); // NOI18N
        upButton.setText("Player Up");
        upButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lockButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(upButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downButton))
                            .addComponent(jScrollPane2, 0, 0, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(autoAssignButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(importButton)))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {downButton, upButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lockButton)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(autoAssignButton)
                        .addComponent(importButton))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(upButton)
                        .addComponent(downButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void lockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockButtonActionPerformed
        Pool pool = getSelectedPool();

        if(pool == null) return;
        if(!GUIUtils.confirmLock(parentWindow, pool.getDescription())) return;
        if(!PermissionChecker.isAllowed(Action.LOCK_DRAW, database)) return;
        try {
            playerTableModel.savePlayerSequence();
            PoolLocker.lockPoolFights(database, pool);
        } catch(DatabaseStateException e) {
            GUIUtils.displayError(parentWindow, e.getMessage());
        }
    }//GEN-LAST:event_lockButtonActionPerformed

    private void autoAssignButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoAssignButtonActionPerformed
        Pool pool = getSelectedPool();
        /*
        try {
            FightGenerator.generateFightsForPool(database, pool);
        } catch(DatabaseStateException e) {
            GUIUtils.displayError(parentWindow, e.getMessage());
        }*/
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        if(configurationFile == null) {
            GUIUtils.displayMessage(parentWindow, "Could not load a valid draw configuration.\nPlease set a draw configuration in Competition Details or assign draws manually", "Auto Assign");
            return;
        }

        int numPlayers = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true).size();            
        if(numPlayers < 1) {
            GUIUtils.displayMessage(parentWindow, "At least 1 player is required to generate a fight draw.", "Auto Assign");
            return;
        }

        String fileName = configurationFile.getDrawName(numPlayers);
        if(fileName == null) {
            GUIUtils.displayMessage(parentWindow, "The draw configuration " + configurationFile.getName() + " does not support divisions with " + numPlayers + " players", "Auto Assign");
            return;
        }

        File csvFile = new File("resources/draw/" + fileName + ".csv");

        try {
            int result = CSVImporter.importFightDraw(csvFile, database, pool, numPlayers);
            //GUIUtils.displayMessage(this, result + " entries succesfully imported.", "Import Complete");
            playerTableModel.shuffle();
        } catch(TooFewPlayersException tfpe) {
            GUIUtils.displayError(this, "The specified fight draw is to small for the number of players in this pool");
        } catch(Exception e) {
            log.error("Exception while importing fights from CSV file", e);
            GUIUtils.displayError(this, "Automatic fight import failed:" + e.getMessage());
        }
    }//GEN-LAST:event_autoAssignButtonActionPerformed

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
    /* get list of pools */
    List<Pool> pools;
    pools = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.PLAYERS_LOCKED);
    pools.addAll(database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED));
    if(pools.size() == 0) {
        GUIUtils.displayMessage(null, "At least one division with locked players must exist to print results", "Print Results");
        return;
    }
    Collections.sort(pools, POOL_COMPARATOR);

    /* display selection dialog */
    CheckboxListDialog<Pool> dialog = new CheckboxListDialog<Pool>(parentWindow, true, pools, "Choose Division", "Print Draws");
    dialog.setRenderer(new StringRenderer<Pool>() {
            @Override
            public String asString(Pool p) {
                return p.getDescription();
            }
    }, Icons.POOL);
    dialog.setVisible(true);

    /* print selected pools */
    if(dialog.getSuccess() && !dialog.getSelectedItems().isEmpty()) {
        new MultipleDrawHTMLGenerator(database, dialog.getSelectedItems(), false).openInBrowser();
    }
}//GEN-LAST:event_printButtonActionPerformed

private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upButtonActionPerformed
    int index = playerTable.getSelectedRow();
    if(index < 1) return;
    playerTableModel.moveUp(index);
    playerTable.setRowSelectionInterval(index-1, index-1);
}//GEN-LAST:event_upButtonActionPerformed

private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
    int index = playerTable.getSelectedRow();
    if((index < 0) || (index >= playerTableModel.getRowCount()-1)) return;
    playerTableModel.moveDown(index);
    playerTable.setRowSelectionInterval(index+1, index+1);
}//GEN-LAST:event_downButtonActionPerformed

private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        Pool pool = getSelectedPool();
        if(pool == null) return;
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));

        if(GUIUtils.lastDrawChooserDirectory != null)
            fileChooser.setCurrentDirectory(GUIUtils.lastDrawChooserDirectory);
        
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            GUIUtils.lastDrawChooserDirectory = fileChooser.getCurrentDirectory();
            
            File csvFile = fileChooser.getSelectedFile();
            if(csvFile == null)
                return;

            int numPlayers = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true).size();

            try {
                int result = CSVImporter.importFightDraw(csvFile, database, pool, numPlayers);
                GUIUtils.displayMessage(this, result + " entries succesfully imported.", "Import Complete");
                playerTableModel.shuffle();
            } catch(TooFewPlayersException tfpe) {
                GUIUtils.displayError(this, "The specified fight draw is to small for the number of players in this pool");
            } catch(Exception e) {
                log.error("Exception while importing fights from CSV file", e);
                GUIUtils.displayError(this, "CSV import failed:" + e.getMessage());
            }
        }
}//GEN-LAST:event_importButtonActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton autoAssignButton;
    private javax.swing.JButton downButton;
    private javax.swing.JTable fightTable;
    private javax.swing.JButton importButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton lockButton;
    private javax.swing.JTable playerTable;
    private javax.swing.JTable poolTable;
    private javax.swing.JButton printButton;
    private javax.swing.JButton upButton;
    // End of variables declaration//GEN-END:variables
    
}
