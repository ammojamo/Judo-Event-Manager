/*
 * ScoringPanel.java
 *
 * Created on 28 August 2008, 14:54
 */

package au.com.jwatmuff.eventmanager.gui.results;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.export.CSVExporter;
import au.com.jwatmuff.eventmanager.gui.main.Icons;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache.DivisionResult;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.print.DivisionResultHTMLGenerator;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.LimitedFrequencyRunner;
import au.com.jwatmuff.eventmanager.util.gui.CheckboxListDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class DivisionResultsPanel extends javax.swing.JPanel implements TransactionListener {
    public static final Logger log = Logger.getLogger(ResultsPanel.class);
    
    private Database database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private DivisionResultCache cache;

    private ResultTableModel resultTableModel = new ResultTableModel();

    private LimitedFrequencyRunner updater = new LimitedFrequencyRunner(new Runnable() {
        public void run() {
            updateFromDatabase();
        }
    }, 2000);
    
    /** Creates new form FightProgressionPanel */
    public DivisionResultsPanel() {
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
    
    public void setDivisionResultCache(DivisionResultCache cache) {
        this.cache = cache;
    }
    
    public void setParentWindow(Frame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    public void afterPropertiesSet() {
        this.notifier.addListener(this, Result.class);
        updater.run(true);
        SortKey key = new SortKey(0, SortOrder.ASCENDING);
        resultTable.getRowSorter().setSortKeys(Arrays.asList(key));
    }
    
    private void updateFromDatabase() {
        log.info("updating result table");

        List<DivisionResult> drs = new ArrayList<DivisionResult>();
        for(Pool p : database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED)) {
            drs.addAll(cache.getDivisionResults(p.getID()));
        }
        resultTableModel.setBeans(drs);
    }

    private class ResultTableModel extends BeanMapperTableModel<DivisionResult> {
        NumberFormat format = new DecimalFormat();
        //DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        public ResultTableModel() {
            super();
            format.setMinimumIntegerDigits(2);
            this.setBeanMapper(new BeanMapper<DivisionResult>() {
                @Override
                public Map<String, Object> mapBean(DivisionResult bean) {                    
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("division", bean.pool.getDescription());
                    map.put("player", bean.player.getFirstName() + " " + bean.player.getLastName());
                    map.put("team", bean.player.getTeam());
                    map.put("place", bean.place);
                    return map;
                } 
            });

            addColumn("Division", "division");
            addColumn("Place", "place");
            addColumn("Player", "player");
            addColumn("Team", "team");
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
    List<Pool> divisions = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED);
    cache.filterDivisionsWithoutResults(divisions);
    CheckboxListDialog<Pool> dialog = new CheckboxListDialog<Pool>(null, true, divisions, "Select divisions to print", "Print Division Results");
    dialog.setRenderer(new StringRenderer<Pool>() {
            public String asString(Pool p) { return p.getDescription(); }
        }, Icons.POOL);
    dialog.setVisible(true);
    if(dialog.getSuccess()) {
        new DivisionResultHTMLGenerator(database, cache, dialog.getSelectedItems()).openInBrowser();
    }
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
