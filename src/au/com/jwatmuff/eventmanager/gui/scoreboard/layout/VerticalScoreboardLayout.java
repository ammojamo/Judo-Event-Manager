/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.scoreboard.layout;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Point;
import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout.Rect;

/**
 *
 * @author james
 */
public class VerticalScoreboardLayout implements ScoreboardLayout {
    private Point getScorePosition(int player) {
        return (player == 0) ? new Point(1.5, 1.5) : new Point(1.5, 4.5);
    }
    
    @Override
    public Rect getScoreRect(int player, Score score) {
        Point pos = getScorePosition(player);

        switch(score) {
            case IPPON: return new Rect(4, 0, 2, 2.75).offsetBy(pos);
            case WAZARI: return new Rect(7, 0, 2, 2.75).offsetBy(pos);
            default: return null;
        }
    }

    @Override
    public Rect getScoreLabelRect(int player, Score score) {
        // Place label relative to score
        Rect r = getScoreRect(player, score);
        return new Rect(r.x - 1, r.y, 1, 1);
    }

    @Override
    public Rect getPlayerLabelRect(int player) {
        return player == 0 ?
                new Rect(0.25, 0.25, 15.5, 1) :
                new Rect(0.25, 7.5, 15.5, 1);
    }

    @Override
    public Rect getTeamLabelRect(int player) {
        return player == 0 ?
                new Rect(0.25, 1.25, 4, 1) :
                new Rect(0.25, 6.5, 4, 1);
    }
    
    @Override
    public Rect getHolddownScoreRect(int player, int index) {
        Point position = (player == 0) ?
                new Point(11, 1.5) :
                new Point(11, 4.5);

        Rect rect = new Rect(index, index * 0.5, 2, 2);
        return rect.offsetBy(position);
    }

    @Override
    public Rect getShidoRect(int player, int shido, int globalIndex, Score shidoType, ScoreboardModel model) {
        Point pos = (player == 0) ?
                new Point(13, 1.5) :
                new Point(13, 4.5);
        double offset = 1.5 - 0.5 * globalIndex;
        return new Rect(offset, offset, 1, 1).offsetBy(pos);
    }

    @Override
    public Rect getTimerRect() {
        return new Rect(5, 8.75, 6, 3);
    }

    @Override
    public Rect getHolddownRect() {
        return new Rect(11.5, 8.75, 4, 3);
    }

    @Override
    public Rect getDivisionRect() {
        return new Rect(0, 10.5, 4, 1.5);
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
        return new Rect(0, 9, 4, 1.5);
    }

    @Override
    public Rect getGoldenScoreApproveRect() {
        return getGoldenScoreRect();
    }

    @Override
    public Rect getResultRect() {
        return getHolddownRect();
    }
}
