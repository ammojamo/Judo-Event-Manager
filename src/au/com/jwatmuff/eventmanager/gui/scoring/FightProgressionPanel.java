/*
 * FightProgressionPanel.java
 *
 * Created on 28 August 2008, 14:54
 */

package au.com.jwatmuff.eventmanager.gui.scoring;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors.Area;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.UpcomingFightFinder;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableLabel;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author  James
 */
public class FightProgressionPanel extends javax.swing.JPanel implements TransactionListener {
    private static final Logger log = Logger.getLogger(FightProgressionPanel.class);

    private Database database;
    private TransactionNotifier notifier;
    private Frame parentWindow;
    private Session matSession;
    
    ScalableLabel f1highlight, now, matTitle;

    ScalableLabel[] numbers;
    ScalableLabel[] player1s;
    ScalableLabel[] player2s;


    /** Creates new form FightProgressionPanel */
    public FightProgressionPanel(Session matSession, boolean displayMatName, int layoutType, int noVertCells, int noHorizCells, double screenHight, double screenWidth) {
        initComponents();
        this.matSession = matSession;
        String matName = matSession.getMat();

        JLayeredPane layeredPane = new JLayeredPane();

        JPanel front = new JPanel();
        Font impact = new Font("Impact", Font.PLAIN, 12);
        ScalableAbsoluteLayout layout = new ScalableAbsoluteLayout(front, screenWidth, screenHight);

        matTitle = new ScalableLabel(matName, impact);
        numbers = new ScalableLabel[noVertCells*noHorizCells];
        player1s = new ScalableLabel[noVertCells*noHorizCells];
        player2s = new ScalableLabel[noVertCells*noHorizCells];
        for(int i = 0; i < noVertCells*noHorizCells; i++){
            numbers[i] = new ScalableLabel(" ");
            player1s[i] = new ScalableLabel(" ");
            player2s[i] = new ScalableLabel(" ");
        }

        double borderRatio = 0.1;
        double numberRatio = 1.5;

        double cellStart;
        double frameHight;
        double frameWidth;
        double cellHight;
        double cellWidth;
        double textHight;
        double borderWidth;
        double numberWidth;
        double textWidth;

        if (layoutType == 0) {
            if (displayMatName) {
                cellStart = screenHight/(noVertCells+1);
            } else {
                cellStart = 0.0;
            }
            frameHight = screenHight - cellStart;
            frameWidth = screenWidth;
            cellHight = frameHight/(noVertCells);
            cellWidth = frameWidth/noHorizCells;
            textHight = cellHight/(2.0*(1+borderRatio));
            borderWidth = borderRatio*textHight;
            numberWidth = textHight*numberRatio;
            textWidth = cellWidth-(2.0*borderWidth+numberWidth);

            if (displayMatName) {
                layout.addComponent(matTitle, new Rectangle.Double(borderWidth, borderWidth, screenWidth-2.0*borderWidth, 2.0*textHight));
                matTitle.setBorder(new EmptyBorder(0,0,0,0));
            }

            now = new ScalableLabel("Now", impact);

            layout.addComponent(now, new Rectangle.Double(borderWidth, cellStart+borderWidth, numberWidth, textHight));
            now.setBorder(new EmptyBorder(0,0,0,0));
            numbers[0].setBorder(new EmptyBorder(0,0,0,0));
            layout.addComponent(player1s[0], new Rectangle.Double(borderWidth+numberWidth, cellStart+borderWidth, textWidth, textHight));
            layout.addComponent(player2s[0], new Rectangle.Double(borderWidth+numberWidth, cellStart+borderWidth+textHight, textWidth, textHight));
            layout.addComponent(numbers[0], new Rectangle.Double(borderWidth, cellStart+borderWidth+textHight, numberWidth, textHight));

            for(int a = 0; a < noVertCells; a++){
                for(int b = 0; b < noHorizCells; b++){
                    if( a > 0 || b > 0){
                        layout.addComponent(player1s[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHight+borderWidth, textWidth, textHight));
                        layout.addComponent(player2s[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHight+borderWidth+textHight, textWidth, textHight));
                        layout.addComponent(numbers[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth, cellStart+a*cellHight+borderWidth, numberWidth, 2.0*textHight));
                    }
                }
            }

        } else {
            if (displayMatName) {
                cellStart = 2.0*(screenHight/(noVertCells+2));
            } else {
                cellStart = 0.0;
            }
            frameHight = screenHight - cellStart;
            frameWidth = screenWidth;
            cellHight = frameHight/(noVertCells);
            cellWidth = frameWidth/noHorizCells;
            textHight = cellHight/(1+2.0*borderRatio);
            borderWidth = borderRatio*textHight;
            numberWidth = textHight*numberRatio;
            textWidth = (cellWidth-(2.0*borderWidth+numberWidth))/2.0;

            if (displayMatName) {
                layout.addComponent(matTitle, new Rectangle.Double(borderWidth, borderWidth, screenWidth-2.0*borderWidth, 2.0*textHight));
                matTitle.setBorder(new EmptyBorder(0,0,0,0));
            }

            now = new ScalableLabel("Now", impact);

            layout.addComponent(now, new Rectangle.Double(borderWidth, cellStart+borderWidth, numberWidth, textHight));
            now.setBorder(new EmptyBorder(0,0,0,0));
            numbers[0].setBorder(new EmptyBorder(0,0,0,0));
            layout.addComponent(player1s[0], new Rectangle.Double(borderWidth+numberWidth, cellStart+borderWidth, textWidth, textHight));
            layout.addComponent(player2s[0], new Rectangle.Double(borderWidth+numberWidth+textWidth, cellStart+borderWidth, textWidth, textHight));

            for(int a = 0; a < noVertCells; a++){
                for(int b = 0; b < noHorizCells; b++){
                    if( a > 0 || b > 0){
                        layout.addComponent(player1s[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHight+borderWidth, textWidth, textHight));
                        layout.addComponent(player2s[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth+numberWidth+textWidth, cellStart+a*cellHight+borderWidth, textWidth, textHight));
                        layout.addComponent(numbers[a+noVertCells*b], new Rectangle.Double(b*cellWidth+borderWidth, cellStart+a*cellHight+borderWidth, numberWidth, textHight));
                    }
                }
            }
        }

        front.setLayout(layout);
        JPanel back = new JPanel();

        layout = new ScalableAbsoluteLayout(back, screenWidth, screenHight);
        f1highlight = new ScalableLabel(" ");
        f1highlight.setBorder(new EmptyBorder(0,0,0,0));
        layout.addComponent(f1highlight, new Rectangle.Double(0.0, cellStart, cellWidth, cellHight));
        back.setLayout(layout);

        layeredPane.setLayout(new OverlayLayout(layeredPane));
        front.setOpaque(false);
        front.setBackground(new Color(0,0,0,0));
        back.setOpaque(false);
        back.setBackground(new Color(0,0,0,0));

        layeredPane.add(front, new Integer(2));
        layeredPane.add(back, new Integer(1));

        setColors(new ScoringColors());
        setLayout(new GridLayout(1,1));
        add(layeredPane);
    }
    
    public void setColors(ScoringColors colors) {
        setForeground(colors.getColor(Area.FIGHTING_FOREGROUND));
        setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
        for(ScalableLabel f : new ScalableLabel[] { now, f1highlight }) {
            f.setForeground(colors.getColor(Area.IDLE_FOREGROUND));
            f.setBackground(colors.getColor(Area.IDLE_BACKGROUND));
        }
        for(int a = 0; a < numbers.length; a++){
            if(a == 0) {
                numbers[a].setForeground(colors.getColor(Area.IDLE_FOREGROUND));
                numbers[a].setBackground(colors.getColor(Area.IDLE_BACKGROUND));
            } else {
                numbers[a].setForeground(colors.getColor(Area.FIGHTING_FOREGROUND));
                numbers[a].setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
            }
            player1s[a].setForeground(colors.getColor(Area.PLAYER1_FOREGROUND));
            player2s[a].setForeground(colors.getColor(Area.PLAYER2_FOREGROUND));
            player1s[a].setBackground(colors.getColor(Area.PLAYER1_BACKGROUND));
            player2s[a].setBackground(colors.getColor(Area.PLAYER2_BACKGROUND));
        }
        matTitle.setForeground(colors.getColor(Area.FIGHTING_FOREGROUND));
        matTitle.setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
    }
    
    @Required
    public void setDatabase(Database database) {
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
        notifier.addListener(this, Result.class, Fight.class, SessionFight.class, Session.class);
        updateFightsFromDatabase();
    }    
    
    private void updateFightsFromDatabase() {

        for(int a = 0; a < numbers.length; a++){
            numbers[a].setText("");
            player1s[a].setText("");
            player2s[a].setText("");
        }

        try {

            Collection<Fight> fights = UpcomingFightFinder.findUpcomingFights(database, matSession.getID(), numbers.length);

            if(fights.size() > 0) {
                Iterator<Fight> iter = fights.iterator();
                int number = 0;
                for(int a = 0; a < numbers.length; a++){
                    if(iter.hasNext()) {
                        Fight f = iter.next();
                        SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, f.getID());
                        number = SessionFightSequencer.getFightMatInfo(database, sf).fightNumber;
                        numbers[a].setText("" + number++);
                        player1s[a].setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString());
                        player2s[a].setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString());
                    } else {
                        return;
                    }
                }
                return;
            }
        } catch(DatabaseStateException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateFightsFromDatabase();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    
}
