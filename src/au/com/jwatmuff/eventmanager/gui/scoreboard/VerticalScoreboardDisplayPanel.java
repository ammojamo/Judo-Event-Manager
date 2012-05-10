/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.scoreboard;

/**
 *
 * @author james
 */
public class VerticalScoreboardDisplayPanel extends DefaultScoreboardDisplayPanel {

    public VerticalScoreboardDisplayPanel() {
        super(new LayoutValues() {

            {
                SCORE_GROUP_X = new double[]{0.25, 0.25}; // Player 1, Player 2
                SCORE_GROUP_Y = new double[]{1.25, 7.25}; // P1, P2
                SCORE_X = new double[]{0, 1.5, 4}; // I, W, Y
                SCORE_WIDTHS = new double[]{1, 2, 2}; // I,W,Y
                SCORE_HEIGHTS = new double[]{1.5, 2, 2}; // I,W,Y

                SCORE_LABEL_HEIGHT = 1;

                PLAYER_LABEL_X = new double[]{0.25, 0.25}; // P1, P2
                PLAYER_LABEL_Y = new double[]{0.25, 6.25};  // P1, P2
                PLAYER_LABEL_WIDTH = 7.5;
                PLAYER_LABEL_HEIGHT = 1;

                SHIDO_GROUP_X = new double[]{1.5, 1.5}; // P1, P2
                SHIDO_GROUP_Y = new double[]{4.75, 10.75}; // P1, P2
                SHIDO_X = new double[]{4, 2, 0, 0}; // S,S,S,H
                SHIDO_WIDTH = 1;
                SHIDO_HEIGHT = 1;

                TIMER_X = 9;
                TIMER_Y = 0.5;
                TIMER_WIDTH = 6;
                TIMER_HEIGHT = 2.5;

                HOLD_DOWN_X = 10;
                HOLD_DOWN_Y = 7;
                HOLD_DOWN_WIDTH = 4;
                HOLD_DOWN_HEIGHT = 4;
            }
        });
    }
}
