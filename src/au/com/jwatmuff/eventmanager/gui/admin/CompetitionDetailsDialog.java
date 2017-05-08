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

package au.com.jwatmuff.eventmanager.gui.admin;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.gui.license.LicenseKeyDialog;
import au.com.jwatmuff.eventmanager.gui.divisions.DivisionsDetailsDialog;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.permissions.License;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
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
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
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
    private LicenseManager licenseManager;
    private Frame parentWindow;
    
    private PoolListTableModel tableModel;

    private CompetitionInfo compInfo = null;
    
    /** Creates new form CompetitionDetailsDialog */
    public CompetitionDetailsDialog(Frame parent, boolean modal, TransactionalDatabase database, TransactionNotifier notifier, LicenseManager licenseManager) {
        super(parent, modal);
        this.database = database;
        this.parentWindow = parent;
        this.notifier = notifier;
        this.licenseManager = licenseManager;
        initComponents();

        drawConfigurationComboBox.setModel(new DefaultComboBoxModel<String>(ConfigurationFile.getConfigurationNames()));

        this.setLocationRelativeTo(parent);
        
        compInfo = database.get(CompetitionInfo.class, null);
        if(compInfo == null)
            throw new RuntimeException("Competition Info must not be null");
        
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

        Date ageThresholdDate = ageThresholdDatePicker.getDate();
        if(ageThresholdDate == null)
          errors.add("Age threshold date is required");
                
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
                ageThresholdDatePicker.setDate(ci.getAgeThresholdDate());
            } catch(Exception e) {
            }
            directorNameTextField.setText(ci.getDirectorName());
            directorContactTextField.setText(ci.getDirectorContact());
            licenseNameLabel.setText(ci.getLicenseName());
            licenseTypeLabel.setText(ci.getLicenseType());
            licenseContactLabel.setText(ci.getLicenseContact());
            drawConfigurationComboBox.setSelectedItem(ci.getDrawConfiguration());
            // if the draw configuration does not exist, default to the first item in the list,
            // which is expected to be the 'default' draw configuration
            if(drawConfigurationComboBox.getSelectedIndex() < 0)
                drawConfigurationComboBox.setSelectedIndex(0);
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

    private void setCompetitionLicense(License license) {
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        ci.setLicenseName(license.getName());
        ci.setLicenseType(license.getType().toString());
        ci.setLicenseContact(license.getContactPhoneNumber());
        database.update(ci);
        updateFromCompetitionInfo(ci);
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        startDatePicker = new com.michaelbaranov.microba.calendar.DatePicker();
        locationTextField = new javax.swing.JTextField();
        competitionNameTextField = new javax.swing.JTextField();
        finishDatePicker = new com.michaelbaranov.microba.calendar.DatePicker();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        directorNameTextField = new javax.swing.JTextField();
        directorContactTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        ageThresholdDatePicker = new com.michaelbaranov.microba.calendar.DatePicker();
        jLabel11 = new javax.swing.JLabel();
        drawConfigurationComboBox = new javax.swing.JComboBox<String>();
        jPanel1 = new javax.swing.JPanel();
        addPoolButton = new javax.swing.JButton();
        deletePoolButton = new javax.swing.JButton();
        editPoolButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        poolListTable = new javax.swing.JTable();
        importButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        licenseNameLabel = new javax.swing.JLabel();
        licenseContactLabel = new javax.swing.JLabel();
        licenseTypeLabel = new javax.swing.JLabel();
        loadLicenseButton = new javax.swing.JButton();
        enterLicenseKeyButton = new javax.swing.JButton();

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

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jLabel1.setText("Competition Name");

        jLabel2.setText("Location");

        jLabel3.setText("Start Date");

        jLabel4.setText("Finish Date");

        competitionNameTextField.setEditable(false);

        jLabel5.setText("Director Name");

        jLabel6.setText("Director Contact");

        jLabel10.setText("Age Threshold Date");

        ageThresholdDatePicker.setToolTipText("Competitor's ages will be calculated on this date");

        jLabel11.setText("Draw Configuration");

        drawConfigurationComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawConfigurationComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(competitionNameTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE)
                            .addComponent(locationTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE)
                            .addComponent(finishDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(startDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ageThresholdDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(directorNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(directorContactTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE)
                            .addComponent(drawConfigurationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel10, jLabel11, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {ageThresholdDatePicker, finishDatePicker, startDatePicker});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(competitionNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(locationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(startDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(finishDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(ageThresholdDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(directorNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(directorContactTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(drawConfigurationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(122, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", jPanel2);

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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(addPoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editPoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deletePoolButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 145, Short.MAX_VALUE)
                        .addComponent(importButton)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addPoolButton)
                    .addComponent(editPoolButton)
                    .addComponent(deletePoolButton)
                    .addComponent(importButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Divisions", jPanel1);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Current License"));

        jLabel7.setText("Name:");

        jLabel8.setText("Contact Number:");

        jLabel9.setText("Level:");

        licenseNameLabel.setText("<name>");

        licenseContactLabel.setText("<number>");

        licenseTypeLabel.setText("<type>");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(licenseNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 443, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(licenseContactLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(licenseTypeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 443, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel7, jLabel8, jLabel9});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(licenseNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(licenseContactLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(licenseTypeLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        loadLicenseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key_add.png"))); // NOI18N
        loadLicenseButton.setText("Load License File..");
        loadLicenseButton.setIconTextGap(8);
        loadLicenseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadLicenseButtonActionPerformed(evt);
            }
        });

        enterLicenseKeyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/key_add.png"))); // NOI18N
        enterLicenseKeyButton.setText("Enter License Key..");
        enterLicenseKeyButton.setIconTextGap(8);
        enterLicenseKeyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterLicenseKeyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(loadLicenseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(enterLicenseKeyButton)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadLicenseButton)
                    .addComponent(enterLicenseKeyButton))
                .addContainerGap(271, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("License", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.IMPORT_DIVISIONS, database)) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        if(GUIUtils.lastDivisionChooserDirectory != null)
            fileChooser.setCurrentDirectory(GUIUtils.lastDivisionChooserDirectory);

        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            GUIUtils.lastDivisionChooserDirectory = fileChooser.getCurrentDirectory();
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
            if(!PermissionChecker.isAllowed(Action.REMOVE_DIVISION, database)) return;
            database.delete(selected);
        }
        tableSelectionChanged();
    }//GEN-LAST:event_deletePoolButtonActionPerformed

    private void editPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editPoolButtonActionPerformed
        Pool selected = getSelectedPool();
        if(selected != null) {
            if(!PermissionChecker.isAllowed(Action.EDIT_DIVISION, database)) return;
            new DivisionsDetailsDialog(parentWindow, true, database, selected).setVisible(true);
        }
    }//GEN-LAST:event_editPoolButtonActionPerformed

    private void addPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPoolButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.ADD_DIVISION, database)) return;
        new DivisionsDetailsDialog(parentWindow, true, database, null).setVisible(true);
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

            comp = compInfo;
            comp.setName(competitionNameTextField.getText().trim());
            comp.setLocation(locationTextField.getText().trim());
            comp.setStartDate(startDatePicker.getDate());
            comp.setEndDate(finishDatePicker.getDate());
            comp.setAgeThresholdDate(ageThresholdDatePicker.getDate());
            comp.setDirectorContact(directorContactTextField.getText().trim());
            comp.setDirectorName(directorNameTextField.getText().trim());
            comp.setDrawConfiguration((String)drawConfigurationComboBox.getSelectedItem());
            
            if(!PermissionChecker.isAllowed(Action.UPDATE_COMPETITION_DETAILS, database))
                return;
            
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

    private void loadLicenseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadLicenseButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.UPDATE_COMPETITION_LICENSE, database)) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("License File", "lic"));
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            License license = License.loadFromFile(file);
            if(license != null)
                try {
                    licenseManager.setLicense(license);
                    setCompetitionLicense(license);
                } catch(IOException e) {
                    GUIUtils.displayError(this, "Error updating license. You may need to reload the license next time you start EventManager");
                }
            else
                GUIUtils.displayError(this, "Error while loading license file");
        }
    }//GEN-LAST:event_loadLicenseButtonActionPerformed

    private void enterLicenseKeyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterLicenseKeyButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.UPDATE_COMPETITION_LICENSE, database)) return;
        LicenseKeyDialog dialog = new LicenseKeyDialog(null, true);
        dialog.setVisible(true);
        if(dialog.getSuccess()) {
            try {
                License license = dialog.getLicense();
                licenseManager.setLicense(license);
                setCompetitionLicense(license);
            } catch(IOException e) {
                GUIUtils.displayError(null, "Unable to save license file. License will not be remembered after EventManager is closed");
            }
        }
    }//GEN-LAST:event_enterLicenseKeyButtonActionPerformed

    private void drawConfigurationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawConfigurationComboBoxActionPerformed


        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration((String)drawConfigurationComboBox.getSelectedItem());
        String defaultDivisionsFileName = configurationFile.getProperty("defaultDivisions");
        String defaultDirectorName = configurationFile.getProperty("defaultDirectorName");
        String defaultDirectorContact = configurationFile.getProperty("defaultDirectorContact");
        if(defaultDirectorName != null && !defaultDirectorName.isEmpty() && directorNameTextField.getText().trim().isEmpty()){
            directorNameTextField.setText(defaultDirectorName);
        }
        if(defaultDirectorContact != null && !defaultDirectorContact.isEmpty() && directorContactTextField.getText().trim().isEmpty()){
            directorContactTextField.setText(defaultDirectorContact);
        }
        if(defaultDivisionsFileName == null || defaultDivisionsFileName.isEmpty() || !database.findAll(Pool.class, PoolDAO.ALL).isEmpty())
            return;
        int response = JOptionPane.showConfirmDialog( null,
                "Do you want to import the default divisions from : " + defaultDivisionsFileName,
                "Default Divisions Import",
                JOptionPane.WARNING_MESSAGE);
        if(response == JOptionPane.OK_OPTION) {
            File csvFile = new File("resources/division/" + defaultDivisionsFileName + ".csv");

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
    }//GEN-LAST:event_drawConfigurationComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPoolButton;
    private com.michaelbaranov.microba.calendar.DatePicker ageThresholdDatePicker;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField competitionNameTextField;
    private javax.swing.JButton deletePoolButton;
    private javax.swing.JTextField directorContactTextField;
    private javax.swing.JTextField directorNameTextField;
    private javax.swing.JComboBox<String> drawConfigurationComboBox;
    private javax.swing.JButton editPoolButton;
    private javax.swing.JButton enterLicenseKeyButton;
    private com.michaelbaranov.microba.calendar.DatePicker finishDatePicker;
    private javax.swing.JButton importButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel licenseContactLabel;
    private javax.swing.JLabel licenseNameLabel;
    private javax.swing.JLabel licenseTypeLabel;
    private javax.swing.JButton loadLicenseButton;
    private javax.swing.JTextField locationTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JTable poolListTable;
    private com.michaelbaranov.microba.calendar.DatePicker startDatePicker;
    // End of variables declaration//GEN-END:variables
 
}
