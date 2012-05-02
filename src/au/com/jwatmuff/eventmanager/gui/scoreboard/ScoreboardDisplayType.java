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
    STYLE_2("Display Style 2");
    
    public String description;  
    
    ScoreboardDisplayType(String description) {
        this.description = description;
    }
    
    public static ScoreboardDisplayPanel getPanel(ScoreboardDisplayType type) {
        switch(type) {
            case DEFAULT: return new DefaultScoreboardDisplayPanel();
            case STYLE_2: return new DefaultScoreboardDisplayPanel();
            default: return new DefaultScoreboardDisplayPanel();
        }
    }
}
