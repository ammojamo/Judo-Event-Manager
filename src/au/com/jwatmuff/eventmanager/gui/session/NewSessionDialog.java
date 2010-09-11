/*
 * EditSessionDialog.java
 *
 * Created on 19 August 2008, 01:00
 */

package au.com.jwatmuff.eventmanager.gui.session;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.misc.SessionLinker;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.Database;
import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class NewSessionDialog extends javax.swing.JDialog {
    private static Logger log = Logger.getLogger(NewSessionDialog.class);
    
    private Frame parentWindow;
    
    private Database database;
    
    private DefaultComboBoxModel poolComboBoxModel;
    private DefaultListModel poolListModel;
    private DefaultComboBoxModel sessionComboBoxModel;
    private DefaultListModel sessionListModel;
    
    private DefaultComboBoxModel matComboBoxModel;

    /** Creates new form EditSessionDialog */
    public NewSessionDialog(java.awt.Frame parent, boolean modal, Database database) {
        super(parent, modal);
        initComponents();
        this.database = database;
        this.parentWindow = parent;
        this.setLocationRelativeTo(parent);
        this.getRootPane().setDefaultButton(okButton);

        // populate mats
        matComboBoxModel = new DefaultComboBoxModel();
        ArrayList<Session> mats = new ArrayList<Session>(database.findAll(Session.class, SessionDAO.ALL_MATS));
        Collections.sort(mats, new Comparator<Session>() {
            public int compare(Session mat1, Session mat2) {
                return mat1.getMat().compareTo(mat2.getMat());
            }
        });
        for(Session mat : mats)
            matComboBoxModel.addElement(mat);
        matComboBox.setModel(matComboBoxModel);
        
        // set up mat name display
        matComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int i, boolean b, boolean b0) {
                if(object instanceof Session) {
                    Session si = (Session)object;
                    object = si.getMat();
                }
                return super.getListCellRendererComponent(jList, object, i, b, b0);
            }
        });
        
        ListCellRenderer sessionRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int i, boolean b, boolean b0) {
                if(object instanceof Session) {
                    Session session = (Session)object;
                    return super.getListCellRendererComponent(jList, session.getName(), i, b, b0);
                } else {
                    return super.getListCellRendererComponent(jList, object, i, b, b0);
                }
            }
        };
        sessionComboBox.setRenderer(sessionRenderer);
        sessionList.setCellRenderer(sessionRenderer);
        
        sessionComboBoxModel = new DefaultComboBoxModel();
        for(Session session : database.findAll(Session.class, SessionDAO.ALL_NORMAL))
            sessionComboBoxModel.addElement(session);
        sessionComboBox.setModel(sessionComboBoxModel);
        sessionListModel = new DefaultListModel();
        sessionList.setModel(sessionListModel);
        
        // set up pool stuff
        poolComboBoxModel = new DefaultComboBoxModel();
        for(Pool pool : database.findAll(Pool.class, PoolDAO.WITHOUT_SESSION))
            poolComboBoxModel.addElement(pool);
        poolComboBox.setModel(poolComboBoxModel);
        poolListModel = new DefaultListModel();
        poolList.setModel(poolListModel);
        
        // set up pool name display
        ListCellRenderer poolRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int i, boolean b, boolean b0) {
                if(object instanceof Pool) {
                    Pool pool = (Pool)object;
                    return super.getListCellRendererComponent(jList, pool.getDescription(), i, b, b0);
                } else {
                    return super.getListCellRendererComponent(jList, object, i, b, b0);
                }
            }
        };
        poolComboBox.setRenderer(poolRenderer);
        poolList.setCellRenderer(poolRenderer);         
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        buttonGroup1 = new javax.swing.ButtonGroup();
        poolPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        poolList = new javax.swing.JList();
        poolComboBox = new javax.swing.JComboBox();
        removePoolButton = new javax.swing.JButton();
        addPoolButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        sessionNameTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        matComboBox = new javax.swing.JComboBox();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        poolPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        sessionList = new javax.swing.JList();
        sessionComboBox = new javax.swing.JComboBox();
        removeSessionButton = new javax.swing.JButton();
        addSessionButton = new javax.swing.JButton();

        jLabel2.setText("jLabel2");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Session");
        setLocationByPlatform(true);

        poolPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Divisions"));

        jScrollPane1.setViewportView(poolList);

        removePoolButton.setText("Remove");
        removePoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePoolButtonActionPerformed(evt);
            }
        });

        addPoolButton.setText("Add");
        addPoolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPoolButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout poolPanelLayout = new javax.swing.GroupLayout(poolPanel);
        poolPanel.setLayout(poolPanelLayout);
        poolPanelLayout.setHorizontalGroup(
            poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(poolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(poolComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(addPoolButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(removePoolButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, Short.MAX_VALUE))
                .addContainerGap())
        );
        poolPanelLayout.setVerticalGroup(
            poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(poolPanelLayout.createSequentialGroup()
                .addGroup(poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(poolPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(poolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(poolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addPoolButton)))
                    .addComponent(removePoolButton))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Session"));

        jLabel1.setText("Name");

        jLabel3.setText("Contest Area");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sessionNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(matComboBox, 0, 219, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(sessionNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(matComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        poolPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Preceding Sessions"));

        jScrollPane2.setViewportView(sessionList);

        removeSessionButton.setText("Remove");
        removeSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSessionButtonActionPerformed(evt);
            }
        });

        addSessionButton.setText("Add");
        addSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSessionButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout poolPanel1Layout = new javax.swing.GroupLayout(poolPanel1);
        poolPanel1.setLayout(poolPanel1Layout);
        poolPanel1Layout.setHorizontalGroup(
            poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(poolPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(sessionComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(addSessionButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(removeSessionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, Short.MAX_VALUE))
                .addContainerGap())
        );
        poolPanel1Layout.setVerticalGroup(
            poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(poolPanel1Layout.createSequentialGroup()
                .addGroup(poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(poolPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(poolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sessionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addSessionButton)))
                    .addComponent(removeSessionButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(poolPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(poolPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(poolPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(poolPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSessionButtonActionPerformed
        Session session = (Session)sessionComboBox.getSelectedItem();
        if(session != null) {
            sessionComboBoxModel.removeElement(session);
            sessionListModel.addElement(session);
        }
    }//GEN-LAST:event_addSessionButtonActionPerformed

    private void removeSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSessionButtonActionPerformed
        Session session = (Session)sessionList.getSelectedValue();
        if(session != null) {
            sessionListModel.removeElement(session);
            sessionComboBoxModel.addElement(session);
        }
    }//GEN-LAST:event_removeSessionButtonActionPerformed

    private void removePoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePoolButtonActionPerformed
        Pool pool = (Pool)poolList.getSelectedValue();
        if(pool != null) {
            poolListModel.removeElement(pool);
            poolComboBoxModel.addElement(pool);
        }
    }//GEN-LAST:event_removePoolButtonActionPerformed

    private void addPoolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPoolButtonActionPerformed
        Pool pool = (Pool)poolComboBox.getSelectedItem();
        if(pool != null) {
            poolComboBoxModel.removeElement(pool);
            poolListModel.addElement(pool);
        }
    }//GEN-LAST:event_addPoolButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        String sessionName = sessionNameTextField.getText().trim();
        if(sessionName.length() <= 0) {
            GUIUtils.displayError(parentWindow, "Session name must be given");
            return;
        }

        Session matSession = (Session)matComboBox.getSelectedItem();
        if(matSession == null) {
            GUIUtils.displayError(parentWindow, "Must specify a mat");
            return;
        }
        
        if(sessionListModel.getSize() == 0 && poolListModel.getSize() == 0) {
            GUIUtils.displayError(parentWindow, "Cannot create a session with no divisions and no preceding sessions.");
            return;
        }
        
        try {
            Session session = new Session();        
            session.setName(sessionName);
            session.setType(Session.SessionType.NORMAL);
            session.setMat(matSession.getMat());

            Session matSessionLast = SessionInfo.getLastOnMat(database, matSession);

            Collection<Session> preceding = new ArrayList<Session>();
            for(Object o : sessionListModel.toArray()) {
                Session s = (Session)o;
                if(new SessionInfo(database, (Session)o).getFollowingSessions().size() > 0) {
                    GUIUtils.displayError(parentWindow, "Cannot link to session " + s.getName() + ": already has a following session");
                    return;
                }
                preceding.add(s);
            }
            
            Collection<Pool> pools = new ArrayList<Pool>();
            for(Object o : poolListModel.toArray())
                pools.add((Pool)o);
            
            SessionLinker.insertSession(database, session, matSessionLast, preceding, pools);
        } catch(Exception e) {
            log.error("Exception while adding new session", e);
            GUIUtils.displayError(parentWindow, "Adding new session failed");
        }
        
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPoolButton;
    private javax.swing.JButton addSessionButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JComboBox matComboBox;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox poolComboBox;
    private javax.swing.JList poolList;
    private javax.swing.JPanel poolPanel;
    private javax.swing.JPanel poolPanel1;
    private javax.swing.JButton removePoolButton;
    private javax.swing.JButton removeSessionButton;
    private javax.swing.JComboBox sessionComboBox;
    private javax.swing.JList sessionList;
    private javax.swing.JTextField sessionNameTextField;
    // End of variables declaration//GEN-END:variables
    
}
