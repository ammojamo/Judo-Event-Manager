/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.ScoringSystem;
import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardWindow;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.UIManager;

/**
 *
 * @author James
 */
public class ManualScoreboard {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /*
         * Set look and feel to 'system' style
         */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
           System.out.println("Failed to set system look and feel");
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
//                ScoreboardWindow window = new ScoreboardWindow("Manual Scoreboard", ScoringSystem.OLD);
                ScoreboardWindow window = new ScoreboardWindow("Manual Scoreboard", ScoringSystem.NEW);

                window.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent evt) {
                        System.exit(0);
                    }
                });
                window.setVisible(true);
            }
        });
    }
}
