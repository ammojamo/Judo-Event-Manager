/*
 * FightOrderPanel.java
 *
 * Created on 2 August 2008, 03:24
 */

package au.com.jwatmuff.eventmanager.gui.session;

import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.model.info.SessionFightInfo;
import au.com.jwatmuff.eventmanager.model.info.SessionInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionLocker;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.Session.SessionType;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.eventmanager.permissions.Action;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.print.FightOrderHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.DistributableBeanTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.JexlBeanTableModel;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class SessionFightsPanel extends javax.swing.JPanel {
    private static final Logger log = Logger.getLogger(SessionFightsPanel.class);

    private TransactionalDatabase database;
    private TransactionNotifier notifier;

    private JFrame parentWindow;
    
    private SessionTableModel sessionTableModel;
    private FightTableModel fightTableModel;
    
    private Session selectedSession;
    private List<SessionFightInfo> fights;
    private boolean fightsDirty = false;

    /** Creates new form FightOrderPanel */
    public SessionFightsPanel() {
        initComponents();
        sessionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fightTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        enableButtons(false);
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
        sessionTableModel = new SessionTableModel(notifier);
        sessionTableModel.updateFromDatabase();
        sessionTable.setModel(sessionTableModel);
        
        sessionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selectedSessionChanged(selectedSession, getSelectedSession());
                selectedSession = getSelectedSession();
            }
        });
        
        fightTableModel = new FightTableModel(notifier);
        fightTable.setModel(fightTableModel);
        setFightsDirty(false);

        GUIUtils.leftAlignTable(fightTable);
    }

    private void setFightsDirty(boolean fightsDirty) {
        this.fightsDirty = fightsDirty;
        this.dirtyIconLabel.setVisible(fightsDirty);
    }
    
    private void selectedSessionChanged(Session old, Session current) {
        if(old == current) return;
        
        if(old != null && fightsDirty) {
            setFightsDirty(false);

            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            try {
                SessionFightSequencer.saveFightSequence(database, fights, true);
            } catch(DatabaseStateException e) {
                this.setCursor(Cursor.getDefaultCursor());
                GUIUtils.displayError(parentWindow, e.getMessage());
            }
            
            this.setCursor(Cursor.getDefaultCursor());
        }
        
        setFightsDirty(false);

        if(current != null) {
            fights = SessionFightSequencer.getFightSequence(database, current.getID());
            enableButtons(current.getLockedStatus() != Session.LockedStatus.FIGHTS_LOCKED);
        }
        else
            fights = new ArrayList<SessionFightInfo>();
        
        fightTableModel.setBeans(fights);
    }
    
    private Session getSelectedSession() {
        int row = sessionTable.getSelectedRow();
        if(row < 0) return null;
        row = sessionTable.getRowSorter().convertRowIndexToModel(row);
        return sessionTableModel.getAtRow(row);
    }
    
    private SessionFight getSelectedFight() {
        int row = fightTable.getSelectedRow();
        if(row < 0) return null;
        //row = fightTable.getRowSorter().convertRowIndexToModel(row);
        return fightTableModel.getAtRow(row).getSessionFight();
    }
    
    private void enableButtons(boolean enabled) {
        deferFightButton.setEnabled(enabled);
        undeferFightButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
        autoOrderButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        spacingSpinner.setEnabled(enabled);
        spacingLabel.setEnabled(enabled);
        lockSessionButton.setEnabled(enabled);
    }
    
    private class SessionTableModel extends JexlBeanTableModel<Session> implements TransactionListener {
        DistributableBeanTableModel<Session> dbtm;
        
        public SessionTableModel(TransactionNotifier notifier) {
            super();
            
            notifier.addListener(this, Session.class);
            
            this.addColumn("Session", "name");
            this.addColumn("Locked", "lockedStatus == 'FIGHTS_LOCKED'");
            dbtm = new DistributableBeanTableModel<Session>(this);
        }

        public void updateFromDatabase() {
            Collection<Session> sessions = database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.POSITION_LOCKED);
            sessions.addAll(database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.FIGHTS_LOCKED));
            Iterator<Session> iter = sessions.iterator();
            while(iter.hasNext())
                if(iter.next().getType() == Session.SessionType.MAT)
                    iter.remove();
            dbtm.setBeans(sessions);
            printButton.setEnabled(getRowCount() > 0);
        }

        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            updateFromDatabase();
        }
    }
    
    private class FightTableModel extends BeanMapperTableModel<SessionFightInfo> implements TransactionListener {
        private int fightNumberOffset;
        public FightTableModel(TransactionNotifier notifier) {
            super();
            
            notifier.addListener(this, SessionFight.class);
            setBeanMapper(new BeanMapper<SessionFightInfo> () {
                @Override
                public Map<String, Object> mapBean(SessionFightInfo bean) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("fight_pos", (fightNumberOffset >= 0) ? bean.getSessionFight().getPosition() + fightNumberOffset : -1);
                    Pool pool = database.get(Pool.class, bean.getFight().getPoolID());
                    String poolDesc = (pool == null)?"(null)":pool.getDescription();
                    map.put("pool_pos", poolDesc + " : " + bean.getFight().getPosition());
                    
                    try {
                        map.put("player1", PlayerCodeParser.parseCode(database, bean.getFight().getPlayerCodes()[0], pool.getID()).toString());
                    } catch(DatabaseStateException e) {
                        map.put("player1", bean.getFight().getPlayerCodes()[0]);
                    }

                    try {
                        map.put("player2", PlayerCodeParser.parseCode(database, bean.getFight().getPlayerCodes()[1], pool.getID()).toString());
                    } catch(DatabaseStateException e) {
                        map.put("player2", bean.getFight().getPlayerCodes()[1]);
                    }

                    return map;
                }
            });
            addColumn("Fight Number", "fight_pos");
            addColumn("Division : Fight", "pool_pos");
            addColumn("Player 1", "player1");
            addColumn("Player 2", "player2");
        }

        @Override
        public void setBeans(Collection<SessionFightInfo> fights) {
            try {
                fightNumberOffset = SessionFightSequencer.getFightMatInfo(database, fights.iterator().next().getSessionFight()).fightNumber - 1;
            } catch(Exception e) {
                fightNumberOffset = -1;
            }
            super.setBeans(fights);
        }
        
        public void updateFromDatabase() {
            setFightsDirty(false);
            if(selectedSession != null) {
                fights = SessionFightSequencer.getFightSequence(database, selectedSession.getID());
            } else {
                fights = Collections.emptyList();
            }
            setBeans(fights);
        }
        
        @Override
        public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
            /**
             * If the current fight order has been modified but not saved, we
             * prompt the user to decide what to do.
             */
            if(fightsDirty) {
                for(DataEvent event : events) {
                    if(event.getDataClass().equals(SessionFight.class) &&
                       ((SessionFight)event.getData()).getSessionID() == selectedSession.getID()) {
                        int result = JOptionPane.showConfirmDialog(
                                parentWindow,
                                "The currently displayed fight order has been modified on another computer.\nDo you wish to load these changes?",
                                "Fight Order Modified",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if(result == JOptionPane.YES_OPTION)
                            updateFromDatabase();
                        return;
                    }
                }
            }
            updateFromDatabase();
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
        sessionTable = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        printButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane3 = new javax.swing.JScrollPane();
        fightTable = new javax.swing.JTable();
        lockSessionButton = new javax.swing.JButton();
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        deferFightButton = new javax.swing.JButton();
        undeferFightButton = new javax.swing.JButton();
        autoOrderButton = new javax.swing.JButton();
        spacingSpinner = new javax.swing.JSpinner();
        spacingLabel = new javax.swing.JLabel();
        resetButton = new javax.swing.JButton();
        dirtyIconLabel = new javax.swing.JLabel();

        jScrollPane1.setDoubleBuffered(true);

        sessionTable.setAutoCreateRowSorter(true);
        sessionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Session"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        sessionTable.setGridColor(new java.awt.Color(204, 204, 204));
        sessionTable.setRowHeight(19);
        jScrollPane1.setViewportView(sessionTable);

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

        lockSessionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/lock.png"))); // NOI18N
        lockSessionButton.setText("Lock Session");
        lockSessionButton.setHideActionText(true);
        lockSessionButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        lockSessionButton.setIconTextGap(8);
        lockSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockSessionButtonActionPerformed(evt);
            }
        });

        upButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_up.png"))); // NOI18N
        upButton.setText("Fight Up");
        upButton.setHideActionText(true);
        upButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        upButton.setIconTextGap(8);
        upButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });

        downButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_down.png"))); // NOI18N
        downButton.setText("Fight Down");
        downButton.setHideActionText(true);
        downButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        downButton.setIconTextGap(8);
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });

        deferFightButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_undo.png"))); // NOI18N
        deferFightButton.setText("Defer Fight");
        deferFightButton.setHideActionText(true);
        deferFightButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        deferFightButton.setIconTextGap(8);
        deferFightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deferFightButtonActionPerformed(evt);
            }
        });

        undeferFightButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/arrow_redo.png"))); // NOI18N
        undeferFightButton.setText("Un-defer Fight");
        undeferFightButton.setHideActionText(true);
        undeferFightButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        undeferFightButton.setIconTextGap(8);
        undeferFightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undeferFightButtonActionPerformed(evt);
            }
        });

        autoOrderButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/wand.png"))); // NOI18N
        autoOrderButton.setText("Auto Order");
        autoOrderButton.setHideActionText(true);
        autoOrderButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        autoOrderButton.setIconTextGap(8);
        autoOrderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoOrderButtonActionPerformed(evt);
            }
        });

        spacingSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(3), Integer.valueOf(1), null, Integer.valueOf(1)));

        spacingLabel.setText("Spacing:");

        resetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/exclamation.png"))); // NOI18N
        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        dirtyIconLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/pencil.png"))); // NOI18N
        dirtyIconLabel.setText(" ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lockSessionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(undeferFightButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(deferFightButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(upButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 54, Short.MAX_VALUE)
                        .addComponent(dirtyIconLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(spacingLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(spacingSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(autoOrderButton, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deferFightButton, undeferFightButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {downButton, upButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lockSessionButton)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deferFightButton)
                    .addComponent(upButton)
                    .addComponent(spacingSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spacingLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(undeferFightButton)
                    .addComponent(downButton)
                    .addComponent(resetButton)
                    .addComponent(autoOrderButton)
                    .addComponent(dirtyIconLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void lockSessionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockSessionButtonActionPerformed
        if(!PermissionChecker.isAllowed(Action.LOCK_SESSION_FIGHT_ORDER, database)) return;
        Session session = getSelectedSession();
        if(session == null) return;
        
        Collection<Session> sessions = SessionInfo.findAllPrecedingPositionLockedSessions(database, session);
        
        /* remove mat sessions */
        Iterator<Session> iter = sessions.iterator();
        while(iter.hasNext()) if(iter.next().getType() == SessionType.MAT) iter.remove();

        if(sessions.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("The fights in the following sessions will also be locked:\n");
            for(Session s : sessions) sb.append("\n" + s.getName());
            sb.append("\n\nDo you wish to continue?");
            int result = JOptionPane.showConfirmDialog(this, sb.toString(), "Lock Session Fights", JOptionPane.YES_NO_OPTION);
            if(result != JOptionPane.YES_OPTION) return;
        } else {
            if(!GUIUtils.confirmLock(parentWindow, "session")) return;
        }
        
        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if(fightsDirty) {
                setFightsDirty(false);
                SessionFightSequencer.saveFightSequence(database, fights, true);
            }
            SessionLocker.lockFights(database, session);
        } catch(Exception e) {
            log.error("Error while locking fights in session " + session.getName(), e);
            GUIUtils.displayError(parentWindow, "Unable to lock session");
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }//GEN-LAST:event_lockSessionButtonActionPerformed

    private void undeferFightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undeferFightButtonActionPerformed
        Session session = getSelectedSession();
        if(session == null) return;
        SessionFight sf = getSelectedFight();
        if(sf == null) return;

        SessionFightInfo sfi = new SessionFightInfo(database, sf);
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if(fightsDirty) {
                setFightsDirty(false);
                SessionFightSequencer.saveFightSequence(database, fights, true);
            }
            SessionFightSequencer.undeferFight(database, sfi);
        } catch(DatabaseStateException e) {
            GUIUtils.displayError(this, "Unable to undefer: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }//GEN-LAST:event_undeferFightButtonActionPerformed

    private void deferFightButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        Session session = getSelectedSession();
        if(session == null) return;
        SessionFight sf = getSelectedFight();
        if(sf == null) return;

        SessionFightInfo sfi = new SessionFightInfo(database, sf);
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if(fightsDirty) {
                setFightsDirty(false);
                SessionFightSequencer.saveFightSequence(database, fights, true);
            }
            SessionFightSequencer.deferFight(database, sfi);
        } catch(DatabaseStateException e) {
            GUIUtils.displayError(this, "Unable to undefer: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }                                                                                            

    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        ChooseMatSessionsDialog cmsd = new ChooseMatSessionsDialog(this.parentWindow, true, database, "Choose sessions to print", "Print sessions");
        cmsd.setVisible(true);

        if(cmsd.getSuccess() && cmsd.getSelectedMat() != null && cmsd.getSelectedSessions().size() > 0)
            new FightOrderHTMLGenerator(database, cmsd.getSelectedMat(), cmsd.getSelectedSessions()).openInBrowser();

        //ChooseMatDialog cmd = new ChooseMatDialog(this.parentWindow, true, database);
        //cmd.setVisible(true);
        //if(cmd.getSuccess() && cmd.getSession() != null)
        //    new FightOrderHTMLGenerator(database, cmd.getSession().getID()).openInBrowser();
    }//GEN-LAST:event_printButtonActionPerformed

    private void autoOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoOrderButtonActionPerformed
        if(selectedSession == null) return;
        setFightsDirty(false);
        SessionFightSequencer.nPassAutoOrder(database, fights, (Integer)spacingSpinner.getValue());
        //SessionFightSequencer.autoOrder(database, fights, (Integer)spacingSpinner.getValue(), true);    
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            SessionFightSequencer.saveFightSequence(database, fights, true);
        } catch(DatabaseStateException e) {
            this.setCursor(Cursor.getDefaultCursor());
            GUIUtils.displayError(parentWindow, e.getMessage());
        }
        this.setCursor(Cursor.getDefaultCursor());
    }//GEN-LAST:event_autoOrderButtonActionPerformed

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upButtonActionPerformed
        if(selectedSession == null) return;
        
        int row = fightTable.getSelectedRow();
        if((row > 0) && (row < fights.size())) {
            int n = SessionFightSequencer.moveFightUp(fights, row);
            if(n > 0) {
                setFightsDirty(true);
                fightTableModel.setBeans(fights);
                fightTable.setRowSelectionInterval(row-1, row-1);
            }
        }
    }//GEN-LAST:event_upButtonActionPerformed

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
        if(selectedSession == null) return;
        
        int row = fightTable.getSelectedRow();
        if((row >= 0) && (row < fights.size()-1)) {
            int n = SessionFightSequencer.moveFightDown(fights, row);
            if(n > 0) {
                setFightsDirty(true);
                fightTableModel.setBeans(fights);
                fightTable.setRowSelectionInterval(row+1, row+1);
            }
        }
    }//GEN-LAST:event_downButtonActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        if(selectedSession == null) return;
        setFightsDirty(false);
        SessionFightSequencer.resetOrder(fights);
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            SessionFightSequencer.saveFightSequence(database, fights, true);
        } catch(DatabaseStateException e) {
            this.setCursor(Cursor.getDefaultCursor());
            GUIUtils.displayError(parentWindow, e.getMessage());
        }
        this.setCursor(Cursor.getDefaultCursor());                                         
    }//GEN-LAST:event_resetButtonActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton autoOrderButton;
    private javax.swing.JButton deferFightButton;
    private javax.swing.JLabel dirtyIconLabel;
    private javax.swing.JButton downButton;
    private javax.swing.JTable fightTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton lockSessionButton;
    private javax.swing.JButton printButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JTable sessionTable;
    private javax.swing.JLabel spacingLabel;
    private javax.swing.JSpinner spacingSpinner;
    private javax.swing.JButton undeferFightButton;
    private javax.swing.JButton upButton;
    // End of variables declaration//GEN-END:variables
    
}
