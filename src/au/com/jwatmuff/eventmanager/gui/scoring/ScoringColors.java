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