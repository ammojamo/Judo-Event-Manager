/*
 * CompetitionDetailsDialog.java
 *
 * Created on 19 March 2008, 19:37
 */

package au.com.jwatmuff.eventmanager.gui.admin;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.gui.pool.PoolDetailsDialog;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import foxtrot.Job;
import foxtrot.Worker;
import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class CompetitionDetailsDialog extends javax.swing.JDialog {
    private static Logger log = Logger.getLogger(CompetitionDetailsDialog.class);
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    
    private PoolListTableModel tableModel;

    private boolean newCompetition = true;
    private CompetitionInfo compInfo = null;
    
    /** Creates new form CompetitionDetailsDialog */
    public CompetitionDetailsDialog(Frame parent, boolean modal, TransactionalDatabase database, TransactionNotifier notifier) {
        super(parent, modal);
        this.database = database;
        this.parentWindow = parent;
        this.notifier = notifier;
        initComponents();
        
        this.setLocationRelativeTo(parent);
        
        compInfo = database.get(CompetitionInfo.class, null);
        if(compInfo != null) {
            newCompetition = false;
        }
        
        updateFromCompetitionInfo(compInfo);

        tableModel = new PoolListTableModel(database);
        notifier.addListener(tableModel, Pool.class, Player.class);
        poolListTable.setModel(tableModel);
        poolListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        tableSelectionChanged();
    }
    
    @Override
    public void dispose() {
        super.dispose();
        notifier.removeListener(tableModel);
    }
    
    private void tableSelectionChanged()
    {
        if(poolListTable.getSelectedRows().length == 1) {
            deletePoolButton.setEnabled(true);
            editPoolButton.setEnabled(true);
        }
        else {
            deletePoolButton.setEnabled(false);
            editPoolButton.setEnabled(false);
        }
        
        Pool pool = getSelectedPool();
    }
    
    
    private Pool getSelectedPool()
    {
        if(poolListTable.getSelectedRows().length == 0)
            return null;
        int row = poolListTable.getSelectedRows()[0];
        row = poolListTable.getRowSorter().convertRowIndexToModel(row);
        Pool p = tableModel.getAtRow(row);
        return p;
    }

    public boolean validateInput()
    {
        List<String> errors = new ArrayList<String>();
        
        Date startDate = startDatePicker.getDate();
        if(startDate == null)
            errors.add("Start date is required");
        
        Date finishDate = finishDatePicker.getDate();
        if(finishDate == null)
            errors.add("Finish date is required");
        else if(finishDate.before(startDate)) {
            errors.add("Finish date may not come before start date");
        }
                
        if(errors.size() > 0) {
            GUIUtils.displayErrors(this, errors);
            return false;
        }
        
        return true;
    }
    
    public void updateFromCompetitionInfo(CompetitionInfo ci) {
        if(ci != null) {
            competitionNameTextField.setText(ci.getName());
            locationTextField.setText(ci.getLocation());
            try {
                startDatePicker.setDate(ci.getStartDate());
                finishDatePicker.setDate(ci.getEndDate());
            } catch(Exception e) {
            }
        }
    }
 
    private class PoolListTableModel extends BeanMapperTableModel<Pool> implements TransactionListener {    
        private Database database;
        
        private BeanMapper<Pool> beanMapper = new BeanMapper<Pool>() {
            @Override
            public Map<String,Object> mapBean(Pool p) {
                Map<String,Object> map = new HashMap<String,Object>();
                
                double maxWeight = p.getMaximumWeight();
                double minWeight = p.getMinimumWeight();
                int maxAge = p.getMaximumAge();
                int minAge = p.getMinimumAge();
                
                String weight;
                if(maxWeight > 0 && minWeight > 0)
                    weight = minWeight + " - " + maxWeight;
                else if(maxWeight > 0)
                    weight = "< " + maxWeight;
                else if(minWeight > 0)
                    weight = "> " + minWeight;
                else
                    weight = "";
                
                String age;
                if(maxAge > 0 && minAge > 0)
                    age = minAge + " - " + maxAge;
                else if(maxAge > 0)
                    age = "< " + maxAge;
                else if(minAge > 0)
                    age = "> " + minAge;
                else
                    age = "";
                
                map.put("description", p.getDescription());
                map.put("weight", weight);
                map.put("age", age);
            
                return map;
            }
        };

        /** Creates a new instance of PoolListTableModel */
        public PoolListTableModel(Database database) {
            super();
            this.database = database;
            setBeanMapper(beanMapper);
            addColumn("Division", "description");
            addColumn("Weight", "weight");
            addColumn("Age", "age");
            updateTableFromDatabase();
        }

        private void updateTableFromDatabase() {
            setBeans(database.findAll(Pool.class, PoolDAO.ALL));
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

        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        addPoolButton = new javax.swing.JButton();
        deletePoolButton = new javax.swing.JButton();
        editPoolButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        poolListTable = new javax.swing.JTable();
        importButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        startDatePicker = new com.michaelbaranov.microba.calendar.DatePicker();
        locationTextField = new javax.swing.JTextField();
        competitionNameTextField = new javax.swing.JTextField();
        finishDatePicker = new com.michaelbaranov.microba.calendar.DatePicker();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Competition Details");
        setLocationByPlatform(true);
        setResizable(false);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Divisions"));

        addPoolButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/group_add.png"))); // NOI18N
        addPoolButton.setText("Add Division..");
        addPoolButton.setIconTextGap(8);
        addPoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPoolButtonActionPerformed(evt);
            }
        });

        deletePoolButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/group_delete.png"))); // NOI18N
        deletePoolButton.setText("Remove");
        deletePoolButton.setToolTipText("Delete Pool");
        deletePoolButton.setIconTextGap(8);
        deletePoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deletePoolButtonActionPerformed(evt);
            }
        });

        editPoolButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/group_edit.png"))); // NOI18N
        editPoolButton.setText("Edit..");
        editPoolButton.setToolTipText("Edit Pool");
        editPoolButton.setIconTextGap(8);
        editPoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editPoolButtonActionPerformed(evt);
            }
        });

        poolListTable.setAutoCreateRowSorter(true);
        poolListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Pool", "Gender", "Max Weight", "Min Weight", "Max Age", "Min Age"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        poolListTable.setGridColor(new java.awt.Color(204, 204, 204));
        poolListTable.setRowHeight(19);
        poolListTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                poolListTableMouseClicked(evt);
            }
        });
        poolListTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                poolListTableKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(poolListTable);

        importButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        importButton.setText("Import..");
        importButton.setIconTextGap(8);
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(addPoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editPoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deletePoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                        .addComponent(importButton)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addPoolButton)
                    .addComponent(editPoolButton)
                    .addComponent(deletePoolButton)
                    .addComponent(importButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("General"));

        jLabel1.setText("Competition Name");

        jLabel2.setText("Location");

        jLabel3.setText("Start Date");

        jLabel4.setText("Finish Date");

        competitionNameTextField.setEditable(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(startDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(locationTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
                    .addComponent(competitionNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
                    .addComponent(finishDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(competitionNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(locationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(finishDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
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
                int succeeded = CSVImporter.importPools(csvFile, database);

                if(succeeded == 0)
                    GUIUtils.displayError(this, "No divisions imported. Check that the CSV file is properly formatted.");
                else
                    GUIUtils.displayMessage(this, succeeded + " divisions succesfully imported", "Import Completed");

            } catch(FileNotFoundException e) {
                GUIUtils.displayError(this, "Could not find file " + csvFile);
            } catch(Exception e) {
                log.error("Error while reading CSV file", e);
                GUIUtils.displayError(this, "CSV Import Failed: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_importButtonActionPerformed

    private void deletePoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deletePoolButtonActionPerformed
        Pool selected = getSelectedPool();
        if(selected != null) {
            database.delete(selected);
        }
        tableSelectionChanged();
    }//GEN-LAST:event_deletePoolButtonActionPerformed

    private void editPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editPoolButtonActionPerformed
        Pool selected = getSelectedPool();
        if(selected != null)
            new PoolDetailsDialog(parentWindow, true, database, selected).setVisible(true);
    }//GEN-LAST:event_editPoolButtonActionPerformed

    private void addPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPoolButtonActionPerformed
        new PoolDetailsDialog(parentWindow, true, database, null).setVisible(true);
        tableSelectionChanged();
    }//GEN-LAST:event_addPoolButtonActionPerformed

    private void poolListTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_poolListTableKeyPressed
        tableSelectionChanged();
    }//GEN-LAST:event_poolListTableKeyPressed

    private void poolListTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_poolListTableMouseClicked
        tableSelectionChanged();
        if(evt.getClickCount() == 2) {
            editPoolButtonActionPerformed(null);
        }
    }//GEN-LAST:event_poolListTableMouseClicked

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if(validateInput()) {
            final CompetitionInfo comp;
            if(newCompetition)
                comp = new CompetitionInfo();
            else
                comp = compInfo;
            comp.setName(competitionNameTextField.getText().trim());
            comp.setLocation(locationTextField.getText().trim());
            comp.setStartDate(startDatePicker.getDate());
            comp.setEndDate(finishDatePicker.getDate());
            
            if(newCompetition) {
                int result = JOptionPane.showConfirmDialog(parentWindow,"Do you wish to set a master password for this competition?","Master Password",JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION) {
                    ChangePasswordDialog cpd = new ChangePasswordDialog(parentWindow, true);
                    cpd.setTitle("Master Password");
                    cpd.setVisible(true);
                    if(cpd.getSuccess())
                        comp.setPasswordHash(cpd.getPasswordHash());
                }
            } else {
                if(!GUIUtils.checkPassword(parentWindow, "Master password required", comp.getPasswordHash()))
                    return;
            }
            
            Worker.post(new Job() {
                @Override
                public Object run() {
                    database.update(comp);
                    return null;
                }
            });
            
            this.dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPoolButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField competitionNameTextField;
    private javax.swing.JButton deletePoolButton;
    private javax.swing.JButton editPoolButton;
    private com.michaelbaranov.microba.calendar.DatePicker finishDatePicker;
    private javax.swing.JButton importButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField locationTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JTable poolListTable;
    private com.michaelbaranov.microba.calendar.DatePicker startDatePicker;
    // End of variables declaration//GEN-END:variables
 
}
