/*
 * ScoringPanel.java
 *
 * Created on 28 August 2008, 14:54
 */

package au.com.jwatmuff.eventmanager.gui.results;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.export.CSVExporter;
import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.PoolChecker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.print.GradingPointsHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.LimitedFrequencyRunner;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class ResultsPointsPanel extends javax.swing.JPanel implements TransactionListener {
    public static final Logger log = Logger.getLogger(ResultsPanel.class);
    
    private Database database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private ResultInfoCache cache;
    
    private String competitionName;

    private ResultTableModel resultTableModel = new ResultTableModel();

    private LimitedFrequencyRunner updater = new LimitedFrequencyRunner(new Runnable() {
        public void run() {
            updateFromDatabase();
        }
    }, 2000);
    
    /** Creates new form FightProgressionPanel */
    public ResultsPointsPanel() {
        initComponents();
        resultTable.setModel(resultTableModel);
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
        updater.run(true);
        SortKey key = new SortKey(1, SortOrder.ASCENDING);
        resultTable.getRowSorter().setSortKeys(Arrays.asList(key));
    }

    public void shutdown() {
        notifier.removeListener(this);
    }

    private void updateFromDatabase() {
        log.info("updating result table");
        competitionName = database.get(CompetitionInfo.class, null).getName();

        List<ResultInfo> ri = new ArrayList<ResultInfo>();
        //for(Result r : database.findAll(Result.class, ResultDAO.ALL)) {
        for(Fight f : database.findAll(Fight.class, FightDAO.WITH_RESULT)) {
            Result r = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID()).iterator().next();
            try {
                if(r.getPlayerIDs()[0] > 0 && r.getPlayerIDs()[1] > 0) // ignore bye fights
                    ri.add(cache.getResultInfo(r.getID()));
            } catch(Exception e) {
                log.info(e,e);
            }
        }
        Collections.sort(ri, new Comparator<ResultInfo>() {
            public int compare(ResultInfo ri1, ResultInfo ri2) {
                double[] scores1 = ri1.getResult().getSimpleScores(database);
                int winnerIndex1 = scores1[1] > scores1[0] ? 1 : 0;
                Player winner1 = ri1.getPlayer()[winnerIndex1];

                double[] scores2 = ri2.getResult().getSimpleScores(database);
                int winnerIndex2 = scores2[1] > scores2[0] ? 1 : 0;
                Player winner2 = ri2.getPlayer()[winnerIndex2];
                return winner1.getVisibleID().compareTo(winner2.getVisibleID());
            }
        });
        resultTableModel.setBeans(ri);
    }

    private class ResultTableModel extends BeanMapperTableModel<ResultInfo> {
        NumberFormat format = new DecimalFormat();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        
        public ResultTableModel() {
            super();
            format.setMinimumIntegerDigits(2);
            this.setBeanMapper(new BeanMapper<ResultInfo>() {
                @Override
                public Map<String, Object> mapBean(ResultInfo bean) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("date", dateFormat.format(bean.getResult().getTimestamp()));
                    map.put("compname", competitionName);
                    
                    Pool pool = database.get(Pool.class, bean.getFight().getPoolID());

                    map.put("pool", pool.getDescription());

                    Date censusDate = database.get(CompetitionInfo.class, null).getAgeThresholdDate();
                    ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());

                    double[] scores = bean.getResult().getSimpleScores(database);

                    Grade winnerGrade;
                    Grade loserGrade = null;

                    if(scores[0] != scores[1]) {
                        int winnerIndex = scores[1] > scores[0] ? 1 : 0;
                        int loserIndex = 1 - winnerIndex;
                        map.put("winner", bean.getPlayerName()[winnerIndex]);
                        map.put("loser", bean.getPlayerName()[loserIndex]);
                        Player loser = bean.getPlayer()[loserIndex];
                        Player winner = bean.getPlayer()[winnerIndex];
                        winnerGrade = (winner == null) ? null : PoolChecker.getEffectiveGrade(winner, pool, censusDate, configurationFile);
                        map.put("winnerRank", (winnerGrade==null)?"N/A": winnerGrade.shortGrade);
                        loserGrade = (loser == null) ? null : PoolChecker.getEffectiveGrade(loser, pool, censusDate, configurationFile);
                        map.put("loserRank", (loserGrade==null)?"N/A": loserGrade.shortGrade);
                        map.put("loserId", (loser == null) ? "N/A" : loser.getVisibleID());
                        map.put("winnerId", (winner == null) ? "N/A" : winner.getVisibleID());
                    }
                    else {
                        map.put("winner", "Draw");
                        map.put("loser", "Draw");
                        map.put("winnerRank", "N/A");
                        map.put("loserRank", "N/A");
                        map.put("winnerId", "N/A");
                        map.put("loserId", "N/A");
                    }

                    
                    map.put("points", calculatePoints(bean, loserGrade));
                    map.put("signature", " ");

                    return map;
                } 
            });

            addColumn("Date", "date");
            addColumn("Player", "winner");
            addColumn("Player ID", "winnerId");
            addColumn("Player Rank", "winnerRank");
            addColumn("Competition", "compname");
            addColumn("Division", "pool");
            addColumn("Opponent", "loser");
            addColumn("Opponent ID", "loserId");
            addColumn("Opponent Rank", "loserRank");
            addColumn("Points", "points");
            addColumn("Signature", "signature");
        }

        private final int[] POINTS = new int[] { 1, 3, 5, 7, 10, 15, 20 };

        private int calculatePoints(ResultInfo info, Grade loserGrade) {
            if(loserGrade == null) return 0;

            double[] score = info.getResult().getSimpleScores(database);
            if(score[0] == score[1]) return 0;
            int w = (score[0] > score[1]) ? 0 : 1;
            int l = 1-w;

            if(info.getPlayer()[w] == null) {
                log.warn("This shouldn't happen");
                return 0;
            }

            Grade winnerGrade = info.getPlayer()[w].getGrade();

            int rankDifference = loserGrade.ordinal() - winnerGrade.ordinal();
            
            if(rankDifference < -2) return 0;
            rankDifference = Math.min(rankDifference, 2);

            if(score[w] == 10)
                return POINTS[4 + rankDifference];
            else if(score[w] == 7)
                return POINTS[3 + rankDifference];
            else
                return POINTS[2 + rankDifference];
        }
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updater.run();
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
        exportButton = new javax.swing.JButton();

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

        exportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/table_go.png"))); // NOI18N
        exportButton.setFocusable(false);
        exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(exportButton);

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
    new GradingPointsHTMLGenerator(database, cache).openInBrowser();
}//GEN-LAST:event_printButtonActionPerformed

private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
        try {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if(!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            OutputStream os = new FileOutputStream(file);
            CSVExporter.generateFromTable(resultTable, os);
            os.close();
        }
    } catch(Exception e) {
        log.error("Exception while writing to text file", e);
        GUIUtils.displayError(parentWindow, "Unable to export to CSV file");
    }
}//GEN-LAST:event_exportButtonActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton exportButton;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton printButton;
    private javax.swing.JTable resultTable;
    private javax.swing.JScrollPane scrollPane1;
    // End of variables declaration//GEN-END:variables

    
}
