/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.permissions;

import au.com.jwatmuff.eventmanager.gui.admin.EnterPasswordDialog;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.StringUtil;
import au.com.jwatmuff.genericdb.Database;
import java.util.Arrays;
import java.util.Collection;
import static au.com.jwatmuff.eventmanager.permissions.PasswordType.*;
import static au.com.jwatmuff.eventmanager.permissions.LicenseType.*;

/**
 *
 * @author James
 */
public class PermissionChecker {
    private static final LicenseType ourLicense = FULL;

    public static boolean isAllowed(Action action, Database database) {
        if(action.requiredPassword != null) {
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
                epd.setVisible(true);
                if(epd.getSuccess()) {
                    if(epd.getPassword().hashCode() != hash) {
                        GUIUtils.displayError(null, "Incorrect password");
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        if(action.requiredLicenses.isEmpty()) {
            return true;
        } else {
            for(LicenseType license : action.requiredLicenses)
                if(ourLicense.covers(license)) return true;
        }

        GUIUtils.displayMessage(null, "This feature requires a license", "License Required");
        return false;
    }
}
