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

package au.com.jwatmuff.eventmanager.gui.scoring;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.gui.scoring.ScoringColors.Area;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.UpcomingFightFinder;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
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
    ScalableLabel[] divisions;

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
        divisions = new ScalableLabel[noVertCells*noHorizCells];
        for(int i = 0; i < noVertCells*noHorizCells; i++){
            numbers[i] = new ScalableLabel(" ");
            player1s[i] = new ScalableLabel(" ");
            player2s[i] = new ScalableLabel(" ");
            divisions[i] = new ScalableLabel(" ");
        }

        double borderRatio = 0.1;
        double numberRatio = 1.5;

        double cellStart;
        double frameHight;
        double frameWidth;
        double cellHeight;
        double cellWidth;
        double textHeight;
        double borderWidth;
        double numberWidth;
        double textWidth;
        double divisionWidth;

        if (layoutType == 0) {
            if (displayMatName) {
                cellStart = screenHight/(noVertCells+1);
            } else {
                cellStart = 0.0;
            }
            frameHight = screenHight - cellStart;
            frameWidth = screenWidth;
            cellHeight = frameHight/(noVertCells);
            cellWidth = frameWidth/noHorizCells;
            textHeight = cellHeight/(2.0*(1+borderRatio));
            borderWidth = borderRatio*textHeight;
            numberWidth = textHeight*numberRatio;
            textWidth = cellWidth-(2.0*borderWidth+numberWidth);

            if (displayMatName) {
                layout.addComponent(matTitle, borderWidth, borderWidth, screenWidth-2.0*borderWidth, 2.0*textHeight);
                matTitle.setBorder(new EmptyBorder(0,0,0,0));
            }

            now = new ScalableLabel("Now", impact);

            layout.addComponent(now, borderWidth, cellStart+borderWidth, numberWidth, textHeight * 0.75);
            now.setBorder(new EmptyBorder(0,0,0,0));
            numbers[0].setBorder(new EmptyBorder(0,0,0,0));
            divisions[0].setBorder(new EmptyBorder(0,0,0,0));
            layout.addComponent(player1s[0], borderWidth+numberWidth, cellStart+borderWidth, textWidth, textHeight);
            layout.addComponent(player2s[0], borderWidth+numberWidth, cellStart+borderWidth+textHeight, textWidth, textHeight);
            layout.addComponent(numbers[0], borderWidth, cellStart+borderWidth+textHeight * 0.75, numberWidth, textHeight * 0.75);
            layout.addComponent(divisions[0], borderWidth, cellStart+borderWidth+textHeight * 1.5, numberWidth, textHeight * 0.5);

            for(int a = 0; a < noVertCells; a++){
                for(int b = 0; b < noHorizCells; b++){
                    if( a > 0 || b > 0){
                        layout.addComponent(player1s[a+noVertCells*b], b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHeight+borderWidth, textWidth, textHeight);
                        layout.addComponent(player2s[a+noVertCells*b], b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHeight+borderWidth+textHeight, textWidth, textHeight);
                        layout.addComponent(numbers[a+noVertCells*b], b*cellWidth+borderWidth, cellStart+a*cellHeight+borderWidth, numberWidth, 1.5*textHeight);
                        layout.addComponent(divisions[a+noVertCells*b], b*cellWidth+borderWidth, cellStart+a*cellHeight+borderWidth + 1.5*textHeight, numberWidth, 0.5*textHeight);
                    }
                }
            }

        } else {
            if (displayMatName) {
                cellStart = screenHight*((2.0+2*borderRatio)/(1+(noVertCells+1)*(1.0+2*borderRatio)));
            } else {
                cellStart = 0.0;
            }
            frameHight = screenHight - cellStart;
            frameWidth = screenWidth;
            cellHeight = frameHight/(noVertCells);
            cellWidth = frameWidth/noHorizCells;
            textHeight = cellHeight/(1+2.0*borderRatio);
            borderWidth = borderRatio*textHeight;
            numberWidth = textHeight*numberRatio;
            divisionWidth = numberWidth;
            textWidth = (cellWidth-(2.0*borderWidth+numberWidth+divisionWidth))/2.0;

            if (displayMatName) {
                layout.addComponent(matTitle, borderWidth, borderWidth, screenWidth-2.0*borderWidth, 2.0*textHeight);
                matTitle.setBorder(new EmptyBorder(0,0,0,0));
            }

            now = new ScalableLabel("Now", impact);

            layout.addComponent(now, borderWidth, cellStart+borderWidth, numberWidth, textHeight);
            now.setBorder(new EmptyBorder(0,0,0,0));
            numbers[0].setBorder(new EmptyBorder(0,0,0,0));
            layout.addComponent(player1s[0], borderWidth+numberWidth+divisionWidth, cellStart+borderWidth, textWidth, textHeight);
            layout.addComponent(player2s[0], borderWidth+numberWidth+divisionWidth+textWidth, cellStart+borderWidth, textWidth, textHeight);
            layout.addComponent(divisions[0], borderWidth+numberWidth, cellStart+borderWidth, divisionWidth, textHeight);

            for(int a = 0; a < noVertCells; a++){
                for(int b = 0; b < noHorizCells; b++){
                    if( a > 0 || b > 0){
                        layout.addComponent(player1s[a+noVertCells*b], b*cellWidth+borderWidth+numberWidth+divisionWidth, cellStart+a*cellHeight+borderWidth, textWidth, textHeight);
                        layout.addComponent(player2s[a+noVertCells*b], b*cellWidth+borderWidth+numberWidth+divisionWidth+textWidth, cellStart+a*cellHeight+borderWidth, textWidth, textHeight);
                        layout.addComponent(numbers[a+noVertCells*b], b*cellWidth+borderWidth, cellStart+a*cellHeight+borderWidth, numberWidth, textHeight);
                        layout.addComponent(divisions[a+noVertCells*b], b*cellWidth+borderWidth+numberWidth, cellStart+a*cellHeight+borderWidth, numberWidth, textHeight);
                    }
                }
            }
        }

        front.setLayout(layout);
        JPanel back = new JPanel();

        layout = new ScalableAbsoluteLayout(back, screenWidth, screenHight);
        f1highlight = new ScalableLabel(" ");
        f1highlight.setBorder(new EmptyBorder(0,0,0,0));
        layout.addComponent(f1highlight, 0.0, cellStart, cellWidth, cellHeight);
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
                divisions[a].setBackground(colors.getColor(Area.IDLE_BACKGROUND));
            } else {
                numbers[a].setForeground(colors.getColor(Area.FIGHTING_FOREGROUND));
                numbers[a].setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
                divisions[a].setBackground(colors.getColor(Area.FIGHTING_BACKGROUND));
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

        Collection<Fight> fights;
        Iterator<Fight> iter;
        int number;

        try {

            fights = UpcomingFightFinder.findUpcomingFights(database, matSession.getID(), numbers.length);

            if(fights.size() > 0) {
                iter = fights.iterator();
                for(int a = 0; a < numbers.length; a++){
                    if(iter.hasNext()) {
                        Fight f = iter.next();
                        SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, f.getID());
                        number = SessionFightSequencer.getFightMatInfo(database, sf).fightNumber;
                        if(!numbers[a].getText().equals("" + number))
                            numbers[a].setText("" + number);
                        if(!player1s[a].getText().equals(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString()))
                            player1s[a].setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[0], f.getPoolID()).toString());
                        if(!player2s[a].getText().equals(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString()))
                            player2s[a].setText(PlayerCodeParser.parseCode(database, f.getPlayerCodes()[1], f.getPoolID()).toString());
                        
                        Pool p = database.get(Pool.class, f.getPoolID());
                        if(!divisions[a].getText().equals(p.getShortName()));
                            divisions[a].setText(p.getShortName());
                    } else {
                        if(!numbers[a].getText().equals(""))
                            numbers[a].setText("");
                        if(!player1s[a].getText().equals(""))
                            player1s[a].setText("");
                        if(!player2s[a].getText().equals(""))
                            player2s[a].setText("");
                        if(!divisions[a].getText().equals(""))
                            divisions[a].setText("");
                    }
                }
            } else {
                for(int a = 0; a < numbers.length; a++){
                    if(!numbers[a].getText().equals(""))
                        numbers[a].setText("");
                    if(!player1s[a].getText().equals(""))
                        player1s[a].setText("");
                    if(!player2s[a].getText().equals(""))
                        player2s[a].setText("");
                    if(!divisions[a].getText().equals(""))
                        divisions[a].setText("");
                }
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
