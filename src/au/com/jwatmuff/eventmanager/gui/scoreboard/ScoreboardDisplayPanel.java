/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.scoreboard;

import javax.swing.JPanel;

/**
 *
 * @author james
 */
public abstract class ScoreboardDisplayPanel extends JPanel {   
    abstract ScoreboardModel getModel();
    abstract void setModel(ScoreboardModel m);
    abstract void setImagesEnabled(boolean enabled);
    abstract void swapPlayers();
}
