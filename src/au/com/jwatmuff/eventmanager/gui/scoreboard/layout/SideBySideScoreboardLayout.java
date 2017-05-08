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
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Point;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Rect;

/**
 *
 * @author james
 */
public class SideBySideScoreboardLayout implements ScoreboardLayout {
    private Point getScorePosition(int player) {
        return (player == 0) ?
                new Point(0.25, 4) :
                new Point(8.25, 4);
    }

    @Override
    public Rect getScoreRect(int player, Score score) {
        Point pos = getScorePosition(player);

        switch(score) {
            case IPPON: return new Rect(1.5, 1, 2, 4).offsetBy(pos);
            case WAZARI: return new Rect(4.5, 1, 2, 4).offsetBy(pos);
            default: return null;
        }
    }

    @Override
    public Rect getScoreLabelRect(int player, Score score) {
        Point pos = getScorePosition(player);
        
        switch(score) {
            case IPPON: return new Rect(1.5, 0, 2, 1).offsetBy(pos);
            case WAZARI: return new Rect(4.5, 0, 2, 1).offsetBy(pos);
            default: return null;
        }
    }

    @Override
    public Rect getPlayerLabelRect(int player) {
        return player == 0 ?
                new Rect(0.25, 2.5, 7.5, 1.5) :
                new Rect(8.25, 2.5, 7.5, 1.5);
    }

    @Override
    public Rect getTeamLabelRect(int player) {
        return player == 0 ?
                new Rect(0.25, 1.5, 4, 1) :
                new Rect(11.75, 1.5, 4, 1);
    }

    @Override
    public Rect getHolddownScoreRect(int player, int index) {
        Point position = (player == 0) ?
                new Point(0.5, 8) :
                new Point(10.5, 8);

        Rect rect = new Rect(index * 1.5, 0, 2, 2);
        return rect.offsetBy(position);
    }

    @Override
    public Rect getShidoRect(int player, int shido, int globalIndex, Score shidoType, ScoreboardModel model) {
        double x = 5 - 1.5 * globalIndex;
        if(player == 1) {
            x = 15 - x;
        }
        return new Rect(x, 9.5, 1, 1.5);
    }

    @Override
    public Rect getTimerRect() {
        return new Rect(5, 0, 6, 2.5);
    }

    @Override
    public Rect getHolddownRect() {
        return new Rect(6, 8, 4, 4);
    }

    @Override
    public Rect getDivisionRect() {
        return new Rect(12, 0, 4, 1.5);
    }

    @Override
    public Rect getPlayerBackgroundRect(int player) {
        return null;
    }

    @Override
    public Rect getTimerBackgroundRect() {
        return null;
    }

    @Override
    public Rect getGoldenScoreRect() {
        return new Rect(5, 0, 6, 0.5);
    }

    @Override
    public Rect getGoldenScoreApproveRect() {
        return new Rect(4, 0, 8, 2.5);
    }

    @Override
    public Rect getResultRect() {
        return new Rect(6, 8, 4, 4);
    }
}
