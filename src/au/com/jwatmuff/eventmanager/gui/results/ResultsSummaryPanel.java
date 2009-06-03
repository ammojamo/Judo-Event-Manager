/*
 * ScoringPanel.java
 *
 * Created on 28 August 2008, 14:54
 */

package au.com.jwatmuff.eventmanager.gui.results;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.print.ResultListHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.awt.Frame;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class ResultsSummaryPanel extends javax.swing.JPanel implements TransactionListener {
    public static final Logger log = Logger.getLogger(ResultsPanel.class);
    
    private Database database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private ResultInfoCache cache;

    private boolean changeMode = false;

    private NumberFormat format = new DecimalFormat();
    private DateFormat dformat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    private BeanMapper<ResultInfo> mapper = new BeanMapper<ResultInfo>() {
        @Override
        public Map<String, Object> mapBean(ResultInfo bean) {

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("matfight", bean.getMatName() + " " + format.format(bean.getMatFightNumber()));
            map.put("division", database.get(Pool.class, bean.getFight().getPoolID()).getDescription());
            map.put("player1", bean.getPlayerName()[0]);
            map.put("player2", bean.getPlayerName()[1]);
            int[] scores = bean.getResult().getPlayerScores();
            map.put("score", scores[0] + " : " + scores[1]);
            if(scores[0] > scores[1])
                map.put("winner", bean.getPlayerName()[0]);
            else if(scores[0] < scores[1])
                map.put("winner", bean.getPlayerName()[1]);
            else
                map.put("winner", "Draw");

            map.put("time", dformat.format(bean.getResult().getTimestamp()));
            map.put("timerec", bean.getResult().getTimestamp()); // for printing

            return map;
        }
    };

    private ResultTableModel resultTableModel = new ResultTableModel();
    
    /** Creates new form FightProgressionPanel */
    public ResultsSummaryPanel() {
        this(false);
    }

    public ResultsSummaryPanel(boolean changeMode) {
        this.changeMode = changeMode;
        initComponents();
        resultTable.setModel(resultTableModel);
        printButton.setVisible(!changeMode);
    }
    
    @Required
    public void setDatabase(Database database) {
        this.database = database;
    }
    
    @Required
    public void setNotifier(TransactionNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void setResultInfoCache(ResultInfoCache cache) {
        this.cache = cache;
    }
    
    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    public void afterPropertiesSet() {
        notifier.addListener(this, Result.class);
        updateFromDatabase();
    }

    public void shutdown() {
        notifier.removeListener(this);
    }
    
    private void updateFromDatabase() {
        log.info("updating result table");
        List<ResultInfo> ri = new ArrayList<ResultInfo>();
        for(Result r : database.findAll(Result.class, ResultDAO.ALL)) {
            try {
                ri.add(cache.getResultInfo(r.getID()));
            } catch(Exception e) {
                log.info(e,e);
            }
        }
        resultTableModel.setBeans(ri);

        /* order by fight and time recorded */
        resultTable.getRowSorter().setSortKeys(Arrays.asList(
                new SortKey(0, SortOrder.ASCENDING),
                new SortKey(6, SortOrder.ASCENDING)));
    }

    private class ResultTableModel extends BeanMapperTableModel<ResultInfo> {
        
        public ResultTableModel() {
            super();
            format.setMinimumIntegerDigits(3);
            this.setBeanMapper(mapper);

            addColumn("Fight", "matfight");
            addColumn("Division", "division");
            addColumn("Player 1", "player1");
            addColumn("Player 2", "player2");
            addColumn("Score", "score");
            addColumn("Winner", "winner");
            addColumn("Time Recorded", "time");
        }
    }

    private ResultInfo getSelectedResult() {
        int row = resultTable.getSelectedRow();
        if(row < 0) return null;
        row = resultTable.getRowSorter().convertRowIndexToModel(row);
        return resultTableModel.getAtRow(row);
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateFromDatabase();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        printButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        resultTable.setAutoCreateRowSorter(true);
        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Title 1"
            }
        ));
        resultTable.setGridColor(new java.awt.Color(204, 204, 204));
        resultTable.setRowHeight(19);
        resultTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultTableMouseClicked(evt);
            }
        });
        scrollPane1.setViewportView(resultTable);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/printer.png"))); // NOI18N
        printButton.setFocusable(false);
        printButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        printButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(printButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
    List<Map> results = new ArrayList<Map>();

    int rows = resultTableModel.getRowCount();
    for(int i = 0; i < rows; i++) {
        ResultInfo ri = resultTableModel.getAtRow(resultTable.getRowSorter().convertRowIndexToModel(i));
        results.add(mapper.mapBean(ri));
    }

    new ResultListHTMLGenerator(database, results).openInBrowser();
}//GEN-LAST:event_printButtonActionPerformed

private void resultTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseClicked
    if(this.changeMode == false) return;

    if(evt.getClickCount() == 2) {
        ResultInfo ri = getSelectedResult();
        if(ri == null) return;

        /* determine whether any dependant fights have been fought */
        Fight f1 = ri.getFight();
        int poolID = f1.getPoolID();
        for(Fight f2 : database.findAll(Fight.class, FightDAO.FOR_POOL, poolID)) {
            if(f2.getPosition() <= f1.getPosition()) continue;

            for(int i = 0; i < 2; i++) {
                String code = f2.getPlayerCodes()[i];
                if(!PlayerCodeParser.getPrefix(code).equals("P")) {
                    int n = PlayerCodeParser.getNumber(code);
                    if(n == f1.getPosition()) {
                        List<Result> r = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f2.getID());
                        if(!r.isEmpty()) {
                            GUIUtils.displayMessage(null, "Result cannot be changed because a fight depending on this result has been fought.", "Cannot Change Result");
                            return;
                        }
                    }
                }
            }
        }

        /* if we get here, show change result dialog */
        ChangeResultDialog crd = new ChangeResultDialog(null, true, ri);
        crd.setVisible(true);
        if(crd.getSuccess()) {
            Result r = new Result();
            r.setFightID(f1.getID());
            r.setPlayerIDs(ri.getResult().getPlayerIDs());
            r.setPlayerScores(crd.getPlayerScores());
            database.add(r);
        }
    }
}//GEN-LAST:event_resultTableMouseClicked

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton printButton;
    private javax.swing.JTable resultTable;
    private javax.swing.JScrollPane scrollPane1;
    // End of variables declaration//GEN-END:variables

    
}
