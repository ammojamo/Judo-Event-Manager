/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.gui.main;

import javax.swing.ImageIcon;

/**
 *
 * @author James
 */
public class Icons {
    private static String SILK = "/com/famfamfam/icons/silk";
    
    public static ImageIcon CONTEST_AREA = getIcon(SILK, "shape_square.png");
    public static ImageIcon SCOREBOARD = getIcon(SILK, "application_view_tile.png");
    public static ImageIcon PLAYER = getIcon(SILK, "user_green.png");
    public static ImageIcon INVALID_PLAYER = getIcon(SILK, "user_red.png");
    public static ImageIcon UNLOCKED_PLAYER = getIcon(SILK, "lock_break.png");
    public static ImageIcon MAIN_WINDOW = getIcon(SILK, "award_star_gold_1.png");
    public static ImageIcon SESSION = getIcon(SILK, "time.png");
    public static ImageIcon MALE = getIcon(SILK, "male.png");
    public static ImageIcon FEMALE = getIcon(SILK, "female.png");
    public static ImageIcon UNKNOWN = getIcon(SILK, "bullet_white.png");
    public static ImageIcon POOL = getIcon(SILK, "group.png");
    public static ImageIcon YES = getIcon(SILK, "tick.png");
    public static ImageIcon NO = getIcon(SILK, "cross.png");

    public static ImageIcon LOCK = getIcon(SILK, "lock.png");
    public static ImageIcon UNLOCK = getIcon(SILK, "lock_open.png");
    
    public static ImageIcon getIcon(String dir, String file) {
        return new ImageIcon(Icons.class.getResource(dir + "/" + file));
    }
}
