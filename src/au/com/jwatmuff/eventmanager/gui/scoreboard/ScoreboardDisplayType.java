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
