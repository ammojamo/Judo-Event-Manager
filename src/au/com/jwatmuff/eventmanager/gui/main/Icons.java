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

package au.com.jwatmuff.eventmanager.gui.main;

import javax.swing.ImageIcon;

/**
 *
 * @author James
 */
public class Icons {
    private final static String SILK = "/com/famfamfam/icons/silk";
    
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
    public static ImageIcon TEAM = getIcon(SILK, "group.png");
    public static ImageIcon YES = getIcon(SILK, "tick.png");
    public static ImageIcon NO = getIcon(SILK, "cross.png");

    public static ImageIcon LOCK = getIcon(SILK, "lock.png");
    public static ImageIcon UNLOCK = getIcon(SILK, "lock_open.png");

    public static ImageIcon REMOTE = getIcon(SILK, "transmit.png");
    public static ImageIcon LOCAL = getIcon(SILK, "drive.png");

    public static ImageIcon EDIT = getIcon(SILK, "pencil.png");
    public static ImageIcon LOG = getIcon(SILK, "information.png");
    
    public static ImageIcon getIcon(String dir, String file) {
        return new ImageIcon(Icons.class.getResource(dir + "/" + file));
    }
}
