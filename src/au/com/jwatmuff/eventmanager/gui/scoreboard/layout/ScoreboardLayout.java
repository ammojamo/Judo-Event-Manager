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

package au.com.jwatmuff.eventmanager.gui.scoreboard.layout;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardDisplayPanel;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardPanel;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Rect;

/**
 *
 * @author james
 */
public interface ScoreboardLayout {
    // The scoreboard is laid out using a 16 x 12 grid
    // 16 x 12 was chosen due to 4:3 ratio and being easily divisible numbers.

    // This methods of this class return Rect objects describing the position
    // and size of various elements on the scoreboard.
    
    Rect getDivisionRect();
    Rect getHolddownRect();
    Rect getHolddownScoreRect(int player, int index);
    Rect getPlayerLabelRect(int player);
    Rect getTeamLabelRect(int player);
    Rect getScoreLabelRect(int player, ScoreboardModel.Score score);
    Rect getScoreRect(int player, ScoreboardModel.Score score);
    Rect getShidoRect(int player, int shido, int globalIndex, ScoreboardModel.Score shidoType, ScoreboardModel model);
    Rect getTimerRect();
    Rect getPlayerBackgroundRect(int player);
    Rect getTimerBackgroundRect();
    Rect getGoldenScoreRect();
    Rect getGoldenScoreApproveRect();
    Rect getResultRect();
}
