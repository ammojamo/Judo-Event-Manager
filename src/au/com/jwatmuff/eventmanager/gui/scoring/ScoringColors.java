/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.gui.scoring;

import java.awt.Color;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author James
 */
public class ScoringColors implements Serializable {
    public static enum Area {
        IDLE_BACKGROUND(Color.YELLOW), 
        IDLE_FOREGROUND(Color.BLACK),
        FIGHTING_BACKGROUND(new Color(194, 191, 221)), 
        FIGHTING_FOREGROUND(Color.BLACK),
        HOLDDOWN_BACKGROUND(Color.GREEN), 
        HOLDDOWN_FOREGROUND(Color.BLACK),
        PLAYER1_BACKGROUND(Color.WHITE), 
        PLAYER1_FOREGROUND(Color.BLACK),
        PLAYER2_BACKGROUND(Color.BLUE), 
        PLAYER2_FOREGROUND(Color.WHITE);
        
        private Color defaultColor;

        Area(Color defaultColor) {
            this.defaultColor = defaultColor;
        }
        
        public Color getDefault() {
            return defaultColor;
        }
    }
    
    private Map<Area, Color> colors = new EnumMap<Area, Color>(Area.class);
    
    public Color getColor(Area a) {        
        Color c = colors.get(a);
        return (c == null) ? a.getDefault() : c;
    }
    
    public void setColor(Area a, Color c) {
        colors.put(a, c);
    }
    
    public ScoringColors combine(ScoringColors sc) {
        ScoringColors result = new ScoringColors();
        for(Area a : Area.values()) {
            Color c = getColor(a);
            if(c == null) c = sc.getColor(a);
            if(c != null) result.setColor(a, c);
        }
        
        return result;
    }
}