/*
 * PoolDetailsDialog.java
 *
 * Created on 25 April 2008, 17:19
 */

package au.com.jwatmuff.eventmanager.gui.pool;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.misc.PoolChecker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import com.jidesoft.swing.DefaultSelectable;
import com.jidesoft.swing.Selectable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JOptionPane;

/**
 *
 * @author  James
 */
public class PoolDetailsDialog extends javax.swing.JDialog {
    private TransactionalDatabase database;
    
    private boolean newPool = true;
    private Pool pool;
    
    private String poolName;
    private double minWeight, maxWeight;
    private int minAge, maxAge;
    private int matchTime;
    private int minBreakTime;
    private int goldenScoreTime;
    private Grade minGrade, maxGrade;
    
    private DefaultListModel playerListModel = new DefaultListModel();
    private Set<Integer> selectedPlayers = new HashSet<Integer>();
    
    /** Creates new form PoolDetailsDialog */
    public PoolDetailsDialog(java.awt.Frame parent, boolean modal, TransactionalDatabase database, Pool pool) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);
        
        this.database = database;
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for(Player.Grade grade : Player.Grade.values())
            model.addElement(grade);
        minGradeComboBox.setModel(model);
        minGradeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int i, boolean b, boolean b0) {
                if(object instanceof Player.Grade)
                    return super.getListCellRendererComponent(jList, ((Player.Grade)object).belt, i, b, b0);
                else
                    return super.getListCellRendererComponent(jList, object, i, b, b0);
            }
        });
        minGradeComboBox.setSelectedItem(Player.Grade.UNSPECIFIED);
        
        model = new DefaultComboBoxModel();
        for(Player.Grade grade : Player.Grade.values())
            model.addElement(grade);
        maxGradeComboBox.setModel(model);
        maxGradeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int i, boolean b, boolean b0) {
                if(object instanceof Player.Grade)
                    return super.getListCellRendererComponent(jList, ((Player.Grade)object).belt, i, b, b0);
                else
                    return super.getListCellRendererComponent(jList, object, i, b, b0);
            }
        });
        maxGradeComboBox.setSelectedItem(Player.Grade.UNSPECIFIED);
        
        model = new DefaultComboBoxModel();
        for(Player.Gender gender : Player.Gender.values())
            model.addElement(gender);
        genderComboBox.setModel(model);
        genderComboBox.setSelectedItem(Player.Gender.UNSPECIFIED);
        
        playerList.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent evt) {
               DefaultSelectable ds = ((DefaultSelectable)evt.getItem());
               Player player = (Player)ds.getObject();
               if(evt.getStateChange() == ItemEvent.SELECTED)
                   selectedPlayers.add(player.getID());
               else if(evt.getStateChange() == ItemEvent.DESELECTED)
                   selectedPlayers.remove(player.getID());
            }
        });
            
        playerList.setModel(playerListModel);
        playerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
                Player player = (Player)((DefaultSelectable)value).getObject();
                value = player.getFirstName() + " " + player.getLastName();
                return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            }
        });
        
        if(pool != null) {
            newPool = false;
            this.pool = pool;
            updateFromPool(pool);
        } else {
            this.pool = new Pool();
        }
        
        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                updatePlayerList();
            }
        };
        
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                updatePlayerList();
            }
        };

        minAgeSpinner.addChangeListener(cl);
        maxAgeSpinner.addChangeListener(cl);
        minWeightSpinner.addChangeListener(cl);
        maxWeightSpinner.addChangeListener(cl);
        maxGradeComboBox.addActionListener(al);
        minGradeComboBox.addActionListener(al);
        minAgeCheckBox.addActionListener(al);
        maxAgeCheckBox.addActionListener(al);
        minWeightCheckBox.addActionListener(al);
        maxWeightCheckBox.addActionListener(al);
        minGradeCheckBox.addActionListener(al);
        maxGradeCheckBox.addActionListener(al);
        genderComboBox.addActionListener(al);
        
        updatePlayerList();
    }
    
    private void updateFromPool(Pool pool) {
        poolNameTextField.setText(pool.getDescription());

        minWeightCheckBox.setSelected(pool.getMinimumWeight() > 0);
        minWeightSpinner.setValue(pool.getMinimumWeight());
        maxWeightCheckBox.setSelected(pool.getMaximumWeight() > 0);
        maxWeightSpinner.setValue(pool.getMaximumWeight());

        minAgeCheckBox.setSelected(pool.getMinimumAge() > 0);
        minAgeSpinner.setValue(pool.getMinimumAge());
        maxAgeCheckBox.setSelected(pool.getMaximumAge() > 0);
        maxAgeSpinner.setValue(pool.getMaximumAge());

        if(pool.getMinimumGrade() != Grade.UNSPECIFIED) {
            minGradeCheckBox.setSelected(true);
            minGradeComboBox.setSelectedItem(pool.getMinimumGrade());
        } else
            minGradeCheckBox.setSelected(false);
        
        if(pool.getMaximumGrade() != Grade.UNSPECIFIED) {
            maxGradeCheckBox.setSelected(true);
            maxGradeComboBox.setSelectedItem(pool.getMaximumGrade());
        } else
            maxGradeCheckBox.setSelected(false);

        genderComboBox.setSelectedItem(pool.getGender());

        matchTimeMinSpinner.setValue(pool.getMatchTime()/60);
        matchTimeSecSpinner.setValue(pool.getMatchTime()%60);
        breakTimeMinSpinner.setValue(pool.getMinimumBreakTime()/60);
        breakTimeSecSpinner.setValue(pool.getMinimumBreakTime()%60);
        goldenScoreTimeMinSpinner.setValue(pool.getGoldenScoreTime()/60);
        goldenScoreTimeSecSpinner.setValue(pool.getGoldenScoreTime()%60);

        boolean unlocked = (pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED);
        maxWeightCheckBox.setEnabled(unlocked);
        minWeightCheckBox.setEnabled(unlocked);
        maxAgeCheckBox.setEnabled(unlocked);
        minAgeCheckBox.setEnabled(unlocked);
        genderComboBox.setEnabled(unlocked);
        maxGradeCheckBox.setEnabled(unlocked);
        minGradeCheckBox.setEnabled(unlocked);
        playerList.setEnabled(unlocked);
    }
    
    private void populate() {
        poolName = poolNameTextField.getText();
        
        minAge = minAgeCheckBox.isSelected()?(Integer)minAgeSpinner.getValue():0;
        maxAge = maxAgeCheckBox.isSelected()?(Integer)maxAgeSpinner.getValue():0;
        
        minWeight = minWeightCheckBox.isSelected()?(Double)minWeightSpinner.getValue():0;
        maxWeight = maxWeightCheckBox.isSelected()?(Double)maxWeightSpinner.getValue():0;
        
        minGrade = minGradeCheckBox.isSelected()?(Grade)minGradeComboBox.getSelectedItem():Grade.UNSPECIFIED;
        maxGrade = maxGradeCheckBox.isSelected()?(Grade)maxGradeComboBox.getSelectedItem():Grade.UNSPECIFIED;
        
        matchTime = (Integer)matchTimeMinSpinner.getValue() * 60 + (Integer)matchTimeSecSpinner.getValue();
        minBreakTime = (Integer)breakTimeMinSpinner.getValue() * 60 + (Integer)breakTimeSecSpinner.getValue();
        goldenScoreTime = (Integer)goldenScoreTimeMinSpinner.getValue() * 60 + (Integer)goldenScoreTimeSecSpinner.getValue();
    }
    
    private boolean validateInput() {
        ArrayList<String> errors = new ArrayList<String>();
        populate();
        
        if(poolName.equals(""))
            errors.add("Pool name required");
        
        if(matchTime == 0)
            errors.add("Match time must be greater than 0");

        if(goldenScoreTime == 0)
            errors.add("Golden score time must be greater than 0");
        
        if(errors.size() > 0) {
            GUIUtils.displayErrors(this,errors);
            return false;
        }
        
        return true;
    }
    
    private void populate(Pool p) {
        populate();
        
        p.setDescription(poolName);
        p.setGender((Player.Gender)genderComboBox.getSelectedItem());

        p.setMinimumAge(minAge);
        p.setMaximumAge(maxAge);
        
        p.setMinimumWeight(minWeight);
        p.setMaximumWeight(maxWeight);
        
        p.setMinimumGrade(minGrade);
        p.setMaximumGrade(maxGrade);
        
        p.setMatchTime(matchTime);
        p.setMinimumBreakTime(minBreakTime);
        p.setGoldenScoreTime(goldenScoreTime);
    }
    
    private void updatePlayerList() {
        populate();
        populate(pool);
        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        playerListModel.clear();

        List<Player> players = database.findAll(Player.class, PlayerDAO.ALL);
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player p1, Player p2) {
                String n1 = p1.getLastName() + p1.getFirstName();
                String n2 = p2.getLastName() + p2.getFirstName();
                return n1.compareTo(n2);
            }
        });

        for(Player player : players)
            if(PoolChecker.checkPlayer(player, pool, ci.getAgeThresholdDate(), configurationFile)) {
                Selectable s = new DefaultSelectable(player);
                PlayerPool pp = database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID()));
                if(pp != null && pp.isValid()) {
                    s.setEnabled(false);
                    s.setSelected(true);
                } else if(selectedPlayers.contains(player.getID())) {
                    s.setSelected(true);
                }
                playerListModel.addElement(s);
            }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        genderComboBox = new javax.swing.JComboBox();
        poolNameTextField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        maxGradeCheckBox = new javax.swing.JCheckBox();
        maxAgeCheckBox = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        maxGradeComboBox = new javax.swing.JComboBox();
        minWeightCheckBox = new javax.swing.JCheckBox();
        maxWeightCheckBox = new javax.swing.JCheckBox();
        maxAgeSpinner = new javax.swing.JSpinner();
        minAgeSpinner = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        minGradeCheckBox = new javax.swing.JCheckBox();
        minWeightSpinner = new javax.swing.JSpinner();
        jLabel16 = new javax.swing.JLabel();
        minGradeComboBox = new javax.swing.JComboBox();
        maxWeightSpinner = new javax.swing.JSpinner();
        minAgeCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        matchTimeSecSpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        breakTimeMinSpinner = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        matchTimeMinSpinner = new javax.swing.JSpinner();
        breakTimeSecSpinner = new javax.swing.JSpinner();
        jLabel14 = new javax.swing.JLabel();
        goldenScoreTimeMinSpinner = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        goldenScoreTimeSecSpinner = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        playerList = new com.jidesoft.swing.CheckBoxListWithSelectable();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Division Details");
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel10.setText("Gender:");

        jLabel1.setText("Division Name:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(poolNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(genderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel10});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(poolNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(genderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel17.setText("kg");

        jLabel18.setText("Age Range:");

        maxGradeCheckBox.setText("At most");

        maxAgeCheckBox.setText("Max");

        jLabel15.setText("Weight Range:");

        jLabel2.setText("Grade Range:");

        jLabel20.setText("yrs");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, maxGradeCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), maxGradeComboBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        minWeightCheckBox.setText("Min");

        maxWeightCheckBox.setText("Max");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, maxAgeCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), maxAgeSpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, minAgeCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), minAgeSpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel19.setText("yrs");

        minGradeCheckBox.setText("At least");

        minWeightSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 300.0d, 0.1d));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, minWeightCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), minWeightSpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel16.setText("kg");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, minGradeCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), minGradeComboBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        maxWeightSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 300.0d, 0.1d));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, maxWeightCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), maxWeightSpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        minAgeCheckBox.setText("Min");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(maxGradeCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxGradeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(minGradeCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minGradeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(maxAgeCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxAgeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minAgeCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minAgeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel20))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(maxWeightCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxWeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minWeightCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minWeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel17)))
                .addContainerGap(64, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {maxAgeSpinner, maxWeightSpinner, minAgeSpinner, minWeightSpinner});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel16, jLabel19});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(maxWeightCheckBox)
                    .addComponent(maxWeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(minWeightCheckBox)
                    .addComponent(minWeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(maxAgeCheckBox)
                    .addComponent(maxAgeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(minAgeCheckBox)
                    .addComponent(minAgeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(minGradeCheckBox)
                    .addComponent(minGradeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxGradeCheckBox)
                    .addComponent(maxGradeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel6.setText("sec");

        jLabel3.setText("min");

        jLabel5.setText("min");

        jLabel11.setText("Match Time:");

        matchTimeSecSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        jLabel4.setText("sec");

        breakTimeMinSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel13.setText("Minimum Break:");

        matchTimeMinSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        breakTimeSecSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        jLabel14.setText("Golden score:");

        goldenScoreTimeMinSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel8.setText("min");

        goldenScoreTimeSecSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        jLabel9.setText("sec");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(breakTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(breakTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(matchTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(goldenScoreTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(goldenScoreTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(matchTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)))
                        .addGap(62, 62, 62)))
                .addContainerGap(135, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {breakTimeMinSpinner, breakTimeSecSpinner, goldenScoreTimeMinSpinner, goldenScoreTimeSecSpinner, matchTimeMinSpinner, matchTimeSecSpinner});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(matchTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(matchTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(goldenScoreTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(goldenScoreTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(breakTimeMinSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(breakTimeSecSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jScrollPane1.setViewportView(playerList);

        jLabel7.setText("Eligible Players");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(okButton)
                            .addComponent(cancelButton)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if(validateInput()) {
            populate(pool);
            
            Set<Integer> visiblePlayers = new HashSet<Integer>();
            for(Object o : playerListModel.toArray())
                visiblePlayers.add(((Player)((DefaultSelectable)o).getObject()).getID());
            selectedPlayers.retainAll(visiblePlayers);

            if(selectedPlayers.isEmpty()){
                String msg = "You have not selected any players \n";
                msg = msg + "Are you sure you wish to continue?";
                if (JOptionPane.showConfirmDialog(null, msg, "Confirm Lock", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            database.perform(new Transaction() {
                @Override
                public void perform() {
                    if(newPool)
                        database.add(pool);
                    else
                        database.update(pool);

                    for(int playerID : selectedPlayers) {
                        PlayerPool pp = new PlayerPool();
                        pp.setPlayerID(playerID);
                        pp.setPoolID(pool.getID());
                        database.add(pp);
                    }
                }
            });

            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner breakTimeMinSpinner;
    private javax.swing.JSpinner breakTimeSecSpinner;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox genderComboBox;
    private javax.swing.JSpinner goldenScoreTimeMinSpinner;
    private javax.swing.JSpinner goldenScoreTimeSecSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
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
    private javax.swing.JSpinner matchTimeMinSpinner;
    private javax.swing.JSpinner matchTimeSecSpinner;
    private javax.swing.JCheckBox maxAgeCheckBox;
    private javax.swing.JSpinner maxAgeSpinner;
    private javax.swing.JCheckBox maxGradeCheckBox;
    private javax.swing.JComboBox maxGradeComboBox;
    private javax.swing.JCheckBox maxWeightCheckBox;
    private javax.swing.JSpinner maxWeightSpinner;
    private javax.swing.JCheckBox minAgeCheckBox;
    private javax.swing.JSpinner minAgeSpinner;
    private javax.swing.JCheckBox minGradeCheckBox;
    private javax.swing.JComboBox minGradeComboBox;
    private javax.swing.JCheckBox minWeightCheckBox;
    private javax.swing.JSpinner minWeightSpinner;
    private javax.swing.JButton okButton;
    private com.jidesoft.swing.CheckBoxListWithSelectable playerList;
    private javax.swing.JTextField poolNameTextField;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    
}
