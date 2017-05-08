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

package au.com.jwatmuff.eventmanager;

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
                ScoreboardWindow window = new ScoreboardWindow("Manual Scoreboard");

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
