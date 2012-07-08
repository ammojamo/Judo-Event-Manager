/*
 * ManageSessionsPanel.java
 *
 * Created on 18 August 2008, 20:44
 */
package au.com.jwatmuff.eventmanager.gui.session;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.SessionLinker;
import au.com.jwatmuff.eventmanager.model.misc.SessionLocker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.Session.SessionType;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.gui.ComboBoxDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Color;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class ManageSessionsPanel extends javax.swing.JPanel {

    private static final Logger log = Logger.getLogger(ManageSessionsPanel.class);
    private TransactionalDatabase database;
    private TransactionNotifier notifier;
    private SessionTableModel sessionTableModel;
    private Frame parentWindow;

    /** Creates new form ManageSessionsPanel */
    public ManageSessionsPanel() {
        initComponents();
        sessionTableModel = new SessionTableModel();
        sessionTable.setModel(sessionTableModel);
        sessionTable.setRowSelectionAllowed(false);
        sessionTable.setGridColor(Color.WHITE);
    }

    @Required
    public void setDatabase(TransactionalDatabase database) {
        this.database = database;
    }

    @Required
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }

    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }

    public void afterPropertiesSet() {
        notifier.addListener(sessionTableModel, Session.class, SessionPool.class, SessionLink.class);
        sessionTableModel.updateFromDatabase();
    }

    private class SessionTableModel extends DefaultTableModel implements TransactionListener {

        private int width = 0,  height = 0;
        private ArrayList<ArrayList<SessionInfo>> sessionTableInfo = new ArrayList<ArrayList<SessionInfo>>();
        private String[] columnNames = new String[0];

        public void updateFromDatabase() {
            boolean matsPresent = (database.findAll(Session.class, SessionDAO.ALL_MATS).size() > 0);
            boolean sessionsPresent = (database.findAll(Session.class, SessionDAO.ALL_NORMAL).size() > 0);

            addSessionButton.setEnabled(matsPresent);
            removeMatButton.setEnabled(matsPresent);
            removeSessionButton.setEnabled(sessionsPresent);
            lockSessionButton.setEnabled(sessionsPresent);

            sessionTableInfo = new ArrayList<ArrayList<SessionInfo>>();

            Collection<SessionInfo> sis = SessionInfo.getAll(database);

            for (SessionInfo si : sis) {
                int row = si.getLevel() - 1;
                if (row == -1) {
                    row = 0; // for sessions with no mat
                }
                assert row >= 0;

                // ensure enough rows in table
                while (sessionTableInfo.size() < row + 1) {
                    sessionTableInfo.add(null);                // add session to row
                }
                ArrayList<SessionInfo> sessionRowInfo = null;
                try {
                    sessionRowInfo = sessionTableInfo.get(row);
                } catch (IndexOutOfBoundsException e) {
                }
                if (sessionRowInfo == null) {
                    sessionRowInfo = new ArrayList<SessionInfo>();
                }
                sessionRowInfo.add(si);
                sessionTableInfo.set(row, sessionRowInfo);
            }

            clear();
            populateCells();
        }

        private void populateCells() {
            int unknownMats = 0;
            HashMap<String, Integer> columnIndex = new HashMap<String, Integer>();

                if (!sessionTableInfo.isEmpty()) {

                /** Gets and orders the Mat row so that mats appear in alphabetical order */
                ArrayList<SessionInfo> matRowInfo = sessionTableInfo.get(0);
// TODO: Leonard: Why is there a null mat just after a session is added and just before the mat link is added.
//System.out.println("No mats is " + matRowInfo.size());
                Collections.sort(matRowInfo, new Comparator<SessionInfo>() {
                    public int compare(SessionInfo session1, SessionInfo session2) {
//                        System.out.println(" Mat Names are : " + session1.getMat() + " and " + session2.getMat());
                        if(session1.getMat() == null && session2.getMat() == null) {
                            return 0;
                        } else if(session1.getMat() == null) {
                            return -1;
                        } else if(session2.getMat() == null) {
                            return 1;
                        }
                        return session1.getMat().compareTo(session2.getMat());
                    }
                });

                for (SessionInfo session : matRowInfo) {
                    // only add a column for mat sessions
                    if (session.getSession().getType() == Session.SessionType.MAT) {
                        String mat = session.getMat();
                        if (mat == null) {
                            log.debug("Found a mat session with null mat string!");
                        } else if (!columnIndex.containsKey(mat)) {
                            columnIndex.put(mat, columnIndex.size());
                            setValueAt(null, 0, columnIndex.get(mat));
                        }
                    }
                }

                /** Ignores the mat row and adds the sessions */
                int baseRow = 0;
                for (ArrayList<SessionInfo> sessionRowInfo : sessionTableInfo) {
                    int maxRow = baseRow;
                    int col = 0;

                    for (SessionInfo session : sessionRowInfo) {
                        // add rows under mats
                        if (session.getSession().getType() == Session.SessionType.MAT) {
                            continue;
                        }

                        int row = baseRow;

                        // find correct column
                        String mat = session.getMat();
                        if (mat == null) {
                            mat = "Unknown Mat " + (++unknownMats);
                        }
                        if (!columnIndex.containsKey(mat)) {
                            columnIndex.put(mat, columnIndex.size());
                        }
                        col = columnIndex.get(mat);

                        // populate cells with information
                        setValueAt("[" + session.getSession().getName() + "]", row++, col);
                        if (session.getSession().getLockedStatus() != Session.LockedStatus.UNLOCKED) {
                            setValueAt("LOCKED", row++, col);
                        }
                        for (Pool pool : session.getPools()) {
                            int nfights = 0;
                            try {
                                nfights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID()).size();
                            } catch(Exception e) {}
                            setValueAt(pool.getDescription() + " (" + nfights + ")", row++, col);
                        }
                        Collection<Session> following = session.getFollowingDependentSessions();
                        for (Session fs : following) {
                            setValueAt("--> " + fs.getMat() + " : " + fs.getName(), row++, col);
                        //if(following.size() == 0)
                        //    setValueAt("--> *", row++, col);
                        // calculate the last row index of this column
                        }
                        maxRow = Math.max(maxRow, row);
                    }
                    baseRow = maxRow + 2;
                }
            }

            this.columnNames = new String[columnIndex.size()];
            for (String mat : columnIndex.keySet()) {
                columnNames[columnIndex.get(mat)] = mat;
            }
            this.fireTableStructureChanged();
        }

        private void clear() {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    setValueAt(null, row, col);
                }
            }
            width = height = 0;
            setColumnCount(0);
            setRowCount(0);
        }

        @Override
        public void setValueAt(Object object, int row, int col) {
            if (col >= width) {
                width = col + 1;
                setColumnCount(width);
            }
            if (row >= height) {
                height = row + 1;
                setRowCount(height);
            }

            super.setValueAt(object, row, col);
        }

        @Override
        public boolean isCellEditable(int i, int i0) {
            return false;
        }

        @Override
        public String getColumnName(int i) {
            if (i < columnNames.length) {
                return columnNames[i];
            } else {
                return "###";
            }
        }

        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            updateFromDatabase();
        }
    }

    private boolean confirmLock(Session session) {
        Collection<Session> sessions = SessionInfo.findAllPrecedingUnlockedSessions(database, session);

        /* remove mats from the list */
        Iterator<Session> iter = sessions.iterator();
        while(iter.hasNext()) if(iter.next().getType() == SessionType.MAT) iter.remove();

        if (sessions.size() > 0) {
            String msg = "The following sessions will also be locked: \n";
            for (Session s : sessions) {
                if (s.getType() == Session.SessionType.NORMAL) {
                    msg = msg + "\n " + s.getMat() + " : " + s.getName();
                } else {
                    msg = msg + "\n " + s.getMat();
                }
            }
            msg = msg + "\n\nAre you sure you wish to continue?";
            if (JOptionPane.showConfirmDialog(parentWindow, msg, "Confirm Lock", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return false;
            }
        } else {
            return GUIUtils.confirmLock(parentWindow, session.getMat() + " : " + session.getName());
        }
        return true;
    }

    private boolean confirmDelete(Session session) {
        if (session.getLockedStatus() != Session.LockedStatus.UNLOCKED) {
            String msg = "";
            if (session.getType() == Session.SessionType.NORMAL) {
                msg = "The session '" + session.getMat() + ":" + session.getName() + "' is locked.\n"
                        + "Are you sure you wish to delete it?";
            } else {
                msg = "The contest area '" + session.getMat() + "' is locked.\n"
                        + "Are you sure you wish to delete it?";
            }
            if (JOptionPane.showConfirmDialog(parentWindow, msg, "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        Collection<Session> sessions = SessionInfo.findAllFollowingSessions(database, session);
        if (sessions.size() > 0) {
            String msg = "The following sessions will also be deleted: \n";
            for (Session s : sessions) {
                msg = msg + "\n " + s.getMat() + " : " + s.getName();
            }
            msg = msg + "\n\nAre you sure you wish to continue?";
            if (JOptionPane.showConfirmDialog(parentWindow, msg, "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return true;
    }
    
    private List<Session> getUnlockedSessions(boolean normal) {
        List<Session> sessions = database.findAll(Session.class, normal?SessionDAO.ALL_NORMAL:SessionDAO.ALL_MATS);
        Iterator<Session> iter = sessions.iterator();
        while(iter.hasNext())
            if(iter.next().getLockedStatus() != Session.LockedStatus.UNLOCKED)
                iter.remove();
        return sessions;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addSessionButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        sessionTable = new javax.swing.JTable();
        addMatButton = new javax.swing.JButton();
        removeSessionButton = new javax.swing.JButton();
        removeMatButton = new javax.swing.JButton();
        lockSessionButton = new javax.swing.JButton();

        addSessionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/time_add.png"))); // NOI18N
        addSessionButton.setText("Add Session..");
        addSessionButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addSessionButton.setIconTextGap(8);
        addSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSessionButtonActionPerformed(evt);
            }
        });

        sessionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Sessions"
            }
        ));
        jScrollPane1.setViewportView(sessionTable);

        addMatButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/shape_square_add.png"))); // NOI18N
        addMatButton.setText("Add Contest Area..");
        addMatButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addMatButton.setIconTextGap(8);
        addMatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMatButtonActionPerformed(evt);
            }
        });

        removeSessionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/time_delete.png"))); // NOI18N
        removeSessionButton.setText("Remove Session..");
        removeSessionButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        removeSessionButton.setIconTextGap(8);
        removeSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSessionButtonActionPerformed(evt);
            }
        });

        removeMatButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/shape_square_delete.png"))); // NOI18N
        removeMatButton.setText("Remove Contest Area..");
        removeMatButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        removeMatButton.setIconTextGap(8);
        removeMatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeMatButtonActionPerformed(evt);
            }
        });

        lockSessionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/lock.png"))); // NOI18N
        lockSessionButton.setText("Lock Session..");
        lockSessionButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        lockSessionButton.setIconTextGap(8);
        lockSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockSessionButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addMatButton)
                        .addGap(6, 6, 6)
                        .addComponent(removeMatButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addSessionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeSessionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lockSessionButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addMatButton, addSessionButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {removeMatButton, removeSessionButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addMatButton)
                    .addComponent(removeMatButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addSessionButton)
                    .addComponent(removeSessionButton)
                    .addComponent(lockSessionButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void lockSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockSessionButtonActionPerformed
        List<Session> sessions = getUnlockedSessions(true);
        
        if(sessions.size() == 0) {
            GUIUtils.displayMessage(parentWindow, "No unlocked sessions to lock.", "Lock Session");
            return;
        }
        if(!PermissionChecker.isAllowed(Action.LOCK_SESSION, database)) return;

        Collections.sort(sessions, new Comparator<Session>() {
            public int compare(Session session1, Session session2) {
                if(session1.getMat().equals(session2.getMat())){
                    return session1.getName().compareTo(session2.getName());
                } else {
                    return session1.getMat().compareTo(session2.getMat());
                }
            }
        });

        ComboBoxDialog<Session> cbd = new ComboBoxDialog<Session>(parentWindow, true, sessions, "Choose session to lock", "Lock Session");
        cbd.setRenderer(new StringRenderer<Session>() {
            @Override
            public String asString(Session o) {
                return o.getMat() + " : " + o.getName();
            }
        }, Icons.SESSION);

        cbd.setVisible(true);
        Session session = cbd.getSelectedItem();
        if (cbd.getSuccess() && confirmLock(session)) {
            try {
                SessionLocker.lockPosition(database, session);
            } catch(DatabaseStateException e) {
                GUIUtils.displayError(parentWindow, e.getMessage());
            }
        }
    }//GEN-LAST:event_lockSessionButtonActionPerformed

    private void removeMatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeMatButtonActionPerformed
//        List<Session> unlockedMats = getUnlockedSessions(false);
        List<Session> unlockedMats = database.findAll(Session.class, SessionDAO.ALL_MATS);
        
        if(unlockedMats.size() == 0) {
            GUIUtils.displayMessage(parentWindow, "No unlocked contest areas to delete.", "Delete Contest Area");
            return;
        }
        if(!PermissionChecker.isAllowed(Action.REMOVE_CONTEST_AREA, database)) return;

        Collections.sort(unlockedMats, new Comparator<Session>() {
            public int compare(Session session1, Session session2) {
                return session1.getMat().compareTo(session2.getMat());
            }
        });

        ComboBoxDialog<Session> cbd = new ComboBoxDialog<Session>(parentWindow, true, unlockedMats, "Choose contest area to delete", "Delete Contest Area");
        cbd.setRenderer(new StringRenderer<Session>() {
            @Override
            public String asString(Session o) {
                return o.getMat();
            }
        }, Icons.CONTEST_AREA);

        cbd.setVisible(true);
        Session session = cbd.getSelectedItem();
        if (cbd.getSuccess() && confirmDelete(session)) {
            try {
                SessionLinker.deleteSession(database, session);
            } catch(DatabaseStateException e) {
                GUIUtils.displayError(parentWindow, e.getMessage());
            }
        }
    }//GEN-LAST:event_removeMatButtonActionPerformed

    private void removeSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSessionButtonActionPerformed
        List<Session> sessions = database.findAll(Session.class, SessionDAO.ALL_NORMAL);
        
        if(sessions.size() == 0) {
            GUIUtils.displayMessage(parentWindow, "No sessions to delete.", "Delete Session");
            return;
        }
        if(!PermissionChecker.isAllowed(Action.REMOVE_SESSION, database)) return;

        Collections.sort(sessions, new Comparator<Session>() {
            public int compare(Session session1, Session session2) {
                if(session1.getMat().equals(session2.getMat())){
                    return session1.getName().compareTo(session2.getName());
                } else {
                    return session1.getMat().compareTo(session2.getMat());
                }
            }
        });

        ComboBoxDialog<Session> cbd = new ComboBoxDialog<Session>(parentWindow, true, sessions, "Choose session to delete", "Delete Session");
        cbd.setRenderer(new StringRenderer<Session>() {
            @Override
            public String asString(Session o) {
                return o.getMat() + " : " + o.getName();
            }
        }, Icons.SESSION);

        cbd.setVisible(true);
        Session session = cbd.getSelectedItem();
        if (cbd.getSuccess() && confirmDelete(session)) {
            try {
                SessionLinker.deleteSession(database, session);
            } catch(DatabaseStateException e) {
                GUIUtils.displayError(parentWindow, e.getMessage());
            }
        }
    }//GEN-LAST:event_removeSessionButtonActionPerformed

    private void addMatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMatButtonActionPerformed
        if(database.get(CompetitionInfo.class, null).getPasswordHash() == 0) {
            GUIUtils.displayMessage(this, "A Master Password must be set before contest areas can be added", "Add Contest Area");
            return;
        }
        if(!PermissionChecker.isAllowed(Action.ADD_CONTEST_AREA, database)) return;

        /* special case to handle licenses allowing limited number of mats */
        List<Session> mats = database.findAll(Session.class, SessionDAO.ALL_MATS);
        int numMats = mats.size();
        if(numMats >= 10 && !PermissionChecker.isAllowed(Action.ADD_MORE_THAN_TEN_MATS, database)) return;
        else if(numMats >= 2 && !PermissionChecker.isAllowed(Action.ADD_MORE_THAN_TWO_MATS, database)) return;

        NewMatDialog nmd = new NewMatDialog(parentWindow, true);
        nmd.setVisible(true);
        if (nmd.getSuccess()) {
            for (Session mat : mats) {
                if (mat.getMat().equalsIgnoreCase(nmd.getMatName())) {
                    GUIUtils.displayMessage(this, "A contest area of this name already exists", "Name Already Exists");
                    return;
                }
            }
            Session session = new Session();
            session.setType(Session.SessionType.MAT);
            session.setMat(nmd.getMatName());
            database.add(session);
        }
    }//GEN-LAST:event_addMatButtonActionPerformed

    private void addSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSessionButtonActionPerformed
        if(database.get(CompetitionInfo.class, null).getPasswordHash() == 0) {
            GUIUtils.displayMessage(this, "A Master Password must be set before sessions can be added", "Add Session");
            return;
        }
        if(!PermissionChecker.isAllowed(Action.ADD_SESSION, database)) return;
        new NewSessionDialog(parentWindow, true, database).setVisible(true);
    }//GEN-LAST:event_addSessionButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addMatButton;
    private javax.swing.JButton addSessionButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton lockSessionButton;
    private javax.swing.JButton removeMatButton;
    private javax.swing.JButton removeSessionButton;
    private javax.swing.JTable sessionTable;
    // End of variables declaration//GEN-END:variables
}
