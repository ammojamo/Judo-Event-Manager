/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.scoreboard;

/**
 *
 * @author james
 */
public enum ScoreboardDisplayType {
    DEFAULT("Default Display"),
    VERTICAL("Vertical Display");
    
    public String description;  
    
    ScoreboardDisplayType(String description) {
        this.description = description;
    }
    
    public static ScoreboardPanel getPanel(ScoreboardDisplayType type) {
        switch(type) {
            case DEFAULT: return new DefaultScoreboardDisplayPanel();
            case VERTICAL: return new VerticalScoreboardDisplayPanel();
            default: return new DefaultScoreboardDisplayPanel();
        }
    }
}
