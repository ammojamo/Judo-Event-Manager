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
                SCORE_GROUP_X = new double[]{1.5, 1.5}; // Player 1, Player 2
                SCORE_GROUP_Y = new double[]{1.5, 4.5}; // P1, P2
                SCORE_X = new double[]{1, 4, 7}; // I, W, Y
                SCORE_Y = new double[] {0, 0, 0}; // I, W, Y
                SCORE_WIDTHS = new double[]{2, 2, 2}; // I,W,Y
                SCORE_HEIGHTS = new double[]{2.75, 2.75, 2.75}; // I,W,Y
                
                SCORE_LABEL_X = new double[]{0, 3, 6}; // I, W, Y
                SCORE_LABEL_Y = new double[] {0, 0, 0}; // I, W, Y
                SCORE_LABEL_WIDTHS = new double[]{1, 1, 1}; // I,W,Y
                SCORE_LABEL_HEIGHTS = new double[]{1, 1, 1}; // I,W,Y
                
                SCORE_SHOW_IPON = true; 

                PLAYER_LABEL_X = new double[]{0.25, 0.25}; // P1, P2
                PLAYER_LABEL_Y = new double[]{0.25, 7.5};  // P1, P2
                PLAYER_LABEL_WIDTH = 15.5;
                PLAYER_LABEL_HEIGHT = 1;

                HD_SCORE_GROUP_X = new double[]{11, 11}; // P1, P2
                HD_SCORE_GROUP_Y = new double[]{1.5, 4.5}; // P1, P2
                HD_SCORE_X = new double[]{0, 1, 2}; // S,S,S,H
                HD_SCORE_Y = new double[]{0, 0.5, 1}; // S,S,S,H
                HD_SCORE_WIDTH = 2;
                HD_SCORE_HEIGHT = 2;

                SHIDO_GROUP_X = new double[]{13, 13}; // P1, P2
                SHIDO_GROUP_Y = new double[]{1.5, 4.5}; // P1, P2
                SHIDO_X = new double[]{1.5, 1, 0.5, 0}; // S,S,S,H
                SHIDO_Y = new double[]{1.5, 1, 0.5, 0}; // S,S,S,H
                SHIDO_WIDTH = 1;
                SHIDO_HEIGHT = 1;

                TIMER_X = 5;
                TIMER_Y = 8.75;
                TIMER_WIDTH = 6;
                TIMER_HEIGHT = 3;

                HOLD_DOWN_X = 11.5;
                HOLD_DOWN_Y = 8.75;
                HOLD_DOWN_WIDTH = 4;
                HOLD_DOWN_HEIGHT = 3;
            }
        });
    }
}
