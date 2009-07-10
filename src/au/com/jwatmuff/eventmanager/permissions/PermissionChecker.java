/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            if(hash != 0) {
                EnterPasswordDialog epd = new EnterPasswordDialog((javax.swing.JFrame)null, true);
                epd.setActionText(action.description);
                epd.setPromptText(action.requiredPassword.description + " password required:");
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
