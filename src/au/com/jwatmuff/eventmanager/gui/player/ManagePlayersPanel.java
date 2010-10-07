/*
 * ManagePlayersPanel.java
 *
 * Created on 19 March 2008, 15:13
 */

package au.com.jwatmuff.eventmanager.gui.player;

import au.com.jwatmuff.eventmanager.export.CSVExporter;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.print.PlayerListHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.ColorIcon;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class ManagePlayersPanel extends javax.swing.JPanel {
    private static Logger log = Logger.getLogger(ManagePlayersPanel.class);
    
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private PlayerListTableModel tableModel;
    
    private Frame parentWindow;
    
    /** Creates new form ManagePlayersPanel */
    public ManagePlayersPanel() {
        initComponents();
    }
    
    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }
    
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void afterPropertiesSet() {
        tableModel = new PlayerListTableModel(database, notifier);
        tableModel.addColumn("Player ID", "visibleID");
        tableModel.addColumn("Last Name", "lastName");
        tableModel.addColumn("First Name", "firstName");
        tableModel.addColumn("DOB", "dob");
//        tableModel.addColumn("Age", "age");
        tableModel.addColumn("Weight", "weight");
        tableModel.addColumn("Grade", "grade");
        tableModel.addColumn("Gender", "gender");
        tableModel.addColumn("Locked", "lockedStatus == 'LOCKED'");
        tableModel.updateTableFromDatabase();
        playerListTable.setModel(tableModel);
        playerListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        playerListTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
            DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            @Override
            public Component getTableCellRendererComponent(JTable table, Object obj, boolean arg2, boolean arg3, int arg4, int arg5) {
                try {obj = format.format(obj); } catch(Exception e) {}
                return super.getTableCellRendererComponent(table, obj, arg2, arg3, arg4, arg5);
            }
        });
        
        playerListTable.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            NumberFormat format = new DecimalFormat("0.0");
            @Override
            public Component getTableCellRendererComponent(JTable table, Object obj, boolean arg2, boolean arg3, int arg4, int arg5) {
                try { obj = format.format(obj); } catch(Exception e) {}
                return super.getTableCellRendererComponent(table, obj, arg2, arg3, arg4, arg5);
            }
        });
        
        playerListTable.setDefaultRenderer(Player.Gender.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object obj, boolean arg2, boolean arg3, int arg4, int arg5) {
                Player.Gender gender = (Player.Gender)obj;

                JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", arg2, arg3, arg4, arg5);
                switch(gender) {
                    case MALE:
                        label.setIcon(Icons.MALE);
                        break;
                    case FEMALE:
                        label.setIcon(Icons.FEMALE);
                        break;
                    default:
                        label.setIcon(Icons.UNKNOWN);
                }
                label.setHorizontalAlignment(JLabel.CENTER);

                return label;
            }
        });
        
        playerListTable.setDefaultRenderer(Player.Grade.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object obj, boolean arg2, boolean arg3, int arg4, int arg5) {
                Player.Grade grade = (Player.Grade)obj;

                JLabel label = (JLabel) super.getTableCellRendererComponent(table, obj, arg2, arg3, arg4, arg5);
                Color c;
                switch(grade) {
                    case WHITE:
                        c = Color.WHITE; break;
                    case YELLOW:
                        c = Color.YELLOW; break;
                    case GREEN:
                        c = Color.GREEN; break;
                    case ORANGE:
                        c = Color.ORANGE; break;
                    case BLUE:
                        c = Color.BLUE; break;
                    case BROWN:
                        c = new Color(0.7f, 0.2f, 0.2f); break;
                    //case UNSPECIFIED:
                        //c = new Color(0.9f, 0.9f, 0.9f); break;
                    default:
                        c = Color.BLACK; break;
                }
                if(grade == Player.Grade.UNSPECIFIED)
                    label.setIcon(Icons.UNKNOWN);
                else
                    label.setIcon(new ColorIcon(c, 16, 12));

                label.setText(grade.belt);
                return label;
            }
        });
    }

    // called when any mouse or key event occurs on the table
    // (as a lightweight alternative to implementing a ListSelectionListener etc.)
    private void tableSelectionChanged()
    {
        Player player = getSelectedPlayer();
        if(player != null) {
            editButton.setEnabled(true);
            if(player.getLockedStatus() != Player.LockedStatus.LOCKED)
                deleteButton.setEnabled(true);
            else
                deleteButton.setEnabled(false);
        }
        else {
            deleteButton.setEnabled(false);
            editButton.setEnabled(false);
        }
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

    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        newButton = new javax.swing.JButton();
        playerListScrollPane = new javax.swing.JScrollPane();
        playerListTable = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        printButton = new javax.swing.JButton();
        exportCSVButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();

        newButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/user_add.png"))); // NOI18N
        newButton.setText("Add Player..");
        newButton.setIconTextGap(8);
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        playerListScrollPane.setMaximumSize(new java.awt.Dimension(452, 4000));

        playerListTable.setAutoCreateRowSorter(true);
        playerListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Player ID", "Last Name", "First Name"
            }
        ));
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
        playerListScrollPane.setViewportView(playerListTable);

        jToolBar1.setFloatable(false);

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printButton.setToolTipText("Print");
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(printButton);

        exportCSVButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table_go.png"))); // NOI18N
        exportCSVButton.setToolTipText("Export CSV");
        exportCSVButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCSVButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(exportCSVButton);

        importButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        importButton.setText("Import Players..");
        importButton.setIconTextGap(8);
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        editButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/user_edit.png"))); // NOI18N
        editButton.setText("Edit Player..");
        editButton.setToolTipText("Edit selected player");
        editButton.setEnabled(false);
        editButton.setIconTextGap(8);
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });

        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/user_delete.png"))); // NOI18N
        deleteButton.setText("Remove Player");
        deleteButton.setToolTipText("Remove selected player");
        deleteButton.setEnabled(false);
        deleteButton.setIconTextGap(8);
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(playerListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(newButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                        .addComponent(importButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(newButton)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(importButton))
                    .addComponent(editButton)
                    .addComponent(deleteButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playerListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void exportCSVButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCSVButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.EXPORT_PLAYERS, database)) return;
        try {

            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
            if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if(!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                OutputStream os = new FileOutputStream(file);
                CSVExporter.generatePlayerList(database, os);
                os.close();
            }
        } catch(Exception e) {
            log.error("Exception while writing to text file", e);
            GUIUtils.displayError(parentWindow, "Unable to print to text file");
        }
    }//GEN-LAST:event_exportCSVButtonActionPerformed

    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        if(playerListTable.getRowCount() == 0)
            JOptionPane.showMessageDialog(this.parentWindow, "No players to print");

        List<Player> players = new ArrayList<Player>();

        for(int row = 0; row < playerListTable.getRowCount(); row++) {
            int mrow = playerListTable.getRowSorter().convertRowIndexToModel(row);
            players.add(tableModel.getAtRow(mrow));
        }
        new PlayerListHTMLGenerator(database, players).openInBrowser();
    }//GEN-LAST:event_printButtonActionPerformed

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.IMPORT_PLAYERS, database)) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));

        if(GUIUtils.lastChooserDirectory != null)
            fileChooser.setCurrentDirectory(GUIUtils.lastChooserDirectory);
        
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            GUIUtils.lastChooserDirectory = fileChooser.getCurrentDirectory();
            
            File csvFile = fileChooser.getSelectedFile();
            if(csvFile == null)
                return;

            try {
                int result = CSVImporter.importPlayers(csvFile, database);
                GUIUtils.displayMessage(this, result + " entries succesfully imported.", "Import Complete");
            } catch(Exception e) {
                log.error("Exception while importing players from CSV file", e);
                GUIUtils.displayError(this, "CSV import failed: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_importButtonActionPerformed

    private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.OPEN_PLAYER, database)) return;
        Player player = getSelectedPlayer();
        new PlayerDetailsDialog(parentWindow, true, database, notifier, player).setVisible(true);
    }//GEN-LAST:event_editButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.REMOVE_PLAYER, database)) return;
        Player player = getSelectedPlayer();
        if(player != null && player.getLockedStatus() != Player.LockedStatus.LOCKED) {
            database.delete(player);
        }
        this.tableSelectionChanged();
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void playerListTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_playerListTableKeyPressed
        this.tableSelectionChanged();
    }//GEN-LAST:event_playerListTableKeyPressed

    private void playerListTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListTableMouseClicked
        this.tableSelectionChanged();
        if(evt.getClickCount() == 2) {
            editButtonActionPerformed(null);
        }
    }//GEN-LAST:event_playerListTableMouseClicked

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.ADD_PLAYER, database)) return;
        if(database.get(CompetitionInfo.class, null) == null)
            JOptionPane.showMessageDialog(
                    this,
                    "Competition details must be entered under the Administration tab before players can be registered.",
                    "Unable to register players",
                    JOptionPane.ERROR_MESSAGE);
        else
        //    (new AddPlayerDialog(parentWindow, true, database)).setVisible(true);
            (new PlayerDetailsDialog(parentWindow, true, database, notifier, null)).setVisible(true);
    }//GEN-LAST:event_newButtonActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editButton;
    private javax.swing.JButton exportCSVButton;
    private javax.swing.JButton importButton;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton newButton;
    private javax.swing.JScrollPane playerListScrollPane;
    private javax.swing.JTable playerListTable;
    private javax.swing.JButton printButton;
    // End of variables declaration//GEN-END:variables
    
}
