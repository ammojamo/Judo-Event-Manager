/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.scoreboard;

import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.SideBySideScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.IJFScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.ScoreboardLayout;
import au.com.jwatmuff.eventmanager.gui.scoreboard.layout.VerticalScoreboardLayout;

/**
 *
 * @author james
 */
public enum ScoreboardDisplayType {
    SIDE_BY_SIDE("Side by Side Display"),
//    VERTICAL("Vertical Display"),
    IJF("IJF Display");
    
    public String description;  
    
    ScoreboardDisplayType(String description) {
        this.description = description;
    }
    
    private static ScoreboardLayout getLayout(ScoreboardDisplayType type) {
        switch(type) {
//            case VERTICAL:
//                return new VerticalScoreboardLayout();            
            case IJF:
            default:
                return new IJFScoreboardLayout();
            case SIDE_BY_SIDE:
                return new SideBySideScoreboardLayout();
        }
    }
    
    public static ScoreboardPanel getPanel(ScoreboardDisplayType type) {
        return ScoreboardDisplayPanel.getInstance(getLayout(type));
    }
}
