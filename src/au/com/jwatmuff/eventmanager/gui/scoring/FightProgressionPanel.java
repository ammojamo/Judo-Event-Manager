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
    
    ScalableLabel f1p1, f1p2, f2p1, f2p2, f3p1, f3p2, f1num, f2num, f3num, f1highlight, now;
    
    /** Creates new form FightProgressionPanel */
    public FightProgressionPanel(Session matSession) {
        initComponents();
        this.matSession = matSession;
        
        JLayeredPane layeredPane = new JLayeredPane();
        
        JPanel front = new JPanel();
        
        Font impact = new Font("Impact", Font.PLAIN, 12);
        
        ScalableAbsoluteLayout layout = new ScalableAbsoluteLayout(front, 1.5, 1);
                
        f1p1 = new ScalableLabel(" ");
        layout.addComponent(f1p1, new Rectangle.Double(0.3, 0, 1.15, 7.0/48));
        
        f1p2 = new ScalableLabel(" ");
        layout.addComponent(f1p2, new Rectangle.Double(0.3, 7.0/48, 1.15, 7.0/48));
                
        f1num = new ScalableLabel(" ", impact);
        layout.addComponent(f1num, new Rectangle.Double(0, 7.0/48, 0.3, 9.0/48));
        
        now = new ScalableLabel("Now", impact);
        layout.addComponent(now, new Rectangle.Double(0, -2.0/48, 0.3, 11.0/48));
        
        f2p1 = new ScalableLabel(" ");
        layout.addComponent(f2p1, new Rectangle.Double(0.3, 16.0/48, 1.15, 7.0/48));
        
        f2p2 = new ScalableLabel(" ");
        layout.addComponent(f2p2, new Rectangle.Double(0.3, 23.0/48, 1.15, 7.0/48));
        
        f2num = new ScalableLabel(" ", impact);
        layout.addComponent(f2num, new Rectangle.Double(0, 16.0/48, 0.3, 14.0/48));
        
        f3p1 = new ScalableLabel(" ");
        layout.addComponent(f3p1, new Rectangle.Double(0.3, 32.0/48, 1.15, 7.0/48));
        
        f3p2 = new ScalableLabel(" ");
        layout.addComponent(f3p2, new Rectangle.Double(0.3, 39.0/48, 1.15, 7.0/48));
        
        f3num = new ScalableLabel(" ", impact);
        layout.addComponent(f3num, new Rectangle.Double(0, 32.0/48, 0.3, 14.0/48));
                
        f1num.setBorder(new EmptyBorder(0,0,0,0));
        f2num.setBorder(new EmptyBorder(0,0,0,0));
        f3num.setBorder(new EmptyBorder(0,0,0,0));
        now.setBorder(new EmptyBorder(0,0,0,0));

        front.setLayout(layout);
        
        JPanel back = new JPanel();

        layout = new ScalableAbsoluteLayout(back, 1.5, 1);
        f1highlight = new ScalableLabel(" ");
        f1highlight.setBorder(new EmptyBorder(0,0,0,0));
        layout.addComponent(f1highlight, new Rectangle.Double(0, -2.0/48, 1.5, 18.0/48));
        
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
        for(ScalableLabel f : new ScalableLabel[] { f1num, now, f1highlight }) {
            f.setForeground(colors.getColor(Area.IDLE_FOREGROUND));
            f.setBackground(colors.getColor(Area.IDLE_BACKGROUND));
        }
        for(ScalableLabel f : new ScalableLabel[] { f2num, f3num }) {
            f.setForeground(colors.getColor(Area.FIGHTING_FOREGROUND));
            f.setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
        }
        for(ScalableLabel f : new ScalableLabel[] { f1p1, f2p1, f3p1}) {
            f.setForeground(colors.getColor(Area.PLAYER1_FOREGROUND));
            f.setBackground(colors.getColor(Area.PLAYER1_BACKGROUND));
        }
        for(ScalableLabel f : new ScalableLabel[] { f1p2, f2p2, f3p2}) {
            f.setForeground(colors.getColor(Area.PLAYER2_FOREGROUND));
            f.setBackground(colors.getColor(Area.PLAYER2_BACKGROUND));
        }
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
        f1p1.setText("");
        f1p2.setText("");
        f1num.setText("");
        f2p1.setText("");
        f2p2.setText("");
        f2num.setText("");
        f3p1.setText("");
        f3p2.setText("");
        f3num.setText("");

        try {

            Collection<Fight> fights = UpcomingFightFinder.findUpcomingFights(database, matSession.getID(), 3);

            if(fights.size() > 0) {
                Iterator<Fight> iter = fights.iterator();
                int number = 0;
                if(iter.hasNext()) {
                    Fight f = iter.next();                    
                    SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, f.getID());
                    number = SessionFightSequencer.getFightMatInfo(database, sf).fightNumber;
                    f1p1.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString());
                    f1p2.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString());
                    f1num.setText("F#" + number++);
                }
                if(iter.hasNext()) {
                    Fight f = iter.next();
                    f2p1.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString());
                    f2p2.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString());
                    f2num.setText("F#" + number++);
                }
                if(iter.hasNext()) {
                    Fight f = iter.next();
                    f3p1.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString());
                    f3p2.setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString());
                    f3num.setText("F#" + number++);
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
