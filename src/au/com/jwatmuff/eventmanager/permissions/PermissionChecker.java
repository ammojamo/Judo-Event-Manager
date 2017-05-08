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

package au.com.jwatmuff.eventmanager.permissions;

import au.com.jwatmuff.eventmanager.gui.admin.EnterPasswordDialog;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.Database;
import static au.com.jwatmuff.eventmanager.permissions.PasswordType.*;

/**
 *
 * @author James
 */
public class PermissionChecker {
    private static LicenseType ourLicenseType = LicenseType.DEFAULT_LICENSE;
    private static boolean masterUnlocked;

    public static void unlockMaster() {
        masterUnlocked = true;
    }

    public static void lockMaster() {
        masterUnlocked = false;
    }

    public static boolean isMasterUnlocked() {
        return masterUnlocked;
    }

    public static void setLicenseType(LicenseType licenseType) {
        ourLicenseType = licenseType;
    }

    public static boolean isAllowed(Action action, Database database) {
        if(action.requiredPassword != null && !masterUnlocked && database != null) {
            int hash = 0;
            CompetitionInfo ci = database.get(CompetitionInfo.class, null);
            switch(action.requiredPassword) {
                case MASTER:
                    hash = ci.getPasswordHash(); break;
                case PERSONAL_DETAILS:
                    hash = ci.getPersonalDetailsPasswordHash(); break;
                case WEIGH_IN:
                    hash = ci.getWeighInPasswordHash(); break;
                case SCOREBOARD:
                    hash = ci.getScoreboardPasswordHash(); break;
            }
            
            /* Default to master if set and other passwords not set */
            PasswordType requiredPassword = action.requiredPassword;
            if(hash == 0 && requiredPassword != MASTER) {
                hash = ci.getPasswordHash();
                requiredPassword = MASTER;
            }

            if(hash != 0) {
                EnterPasswordDialog epd = new EnterPasswordDialog((javax.swing.JFrame)null, true);
                epd.setActionText(action.description);
                epd.setPromptText(requiredPassword.description + " password required:");
                while(true) {
                    epd.setVisible(true);
                    if(epd.getSuccess()) {
                        if(epd.getPassword().hashCode() != hash) {
                            GUIUtils.displayError(null, "Incorrect password");
                        } else {
                            break;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        if(action.requiredLicenses.isEmpty()) {
            return true;
        } else {
            for(LicenseType license : action.requiredLicenses)
                if(ourLicenseType.covers(license)) return true;
        }

        GUIUtils.displayMessage(null, "This feature requires an upgraded license", "License Required");
        return false;
    }
}
