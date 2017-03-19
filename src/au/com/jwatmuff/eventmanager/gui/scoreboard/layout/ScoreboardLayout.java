/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
