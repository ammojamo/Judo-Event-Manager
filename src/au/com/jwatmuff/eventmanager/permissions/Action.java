package au.com.jwatmuff.eventmanager.permissions;

import au.com.jwatmuff.eventmanager.util.StringUtil;
import java.util.Arrays;
import java.util.Collection;
import static au.com.jwatmuff.eventmanager.permissions.PasswordType.*;
import static au.com.jwatmuff.eventmanager.permissions.LicenseType.*;

public enum Action {
    /*
     * This is where user actions are linked to license and password restrictions
     */
    CHANGE_MASTER_PASSWORD(MASTER),
    CHANGE_WEIGH_IN_PASSWORD(MASTER),
    CHANGE_PERSONAL_DETAILS_PASSWORD(MASTER),
    CHANGE_SCOREBOARD_PASSWORD(MASTER),
    CHANGE_WEIGH_IN(MASTER),
    ENTER_WEIGH_IN(WEIGH_IN),
    UPDATE_COMPETITION_DETAILS(MASTER),
    UPDATE_COMPETITION_LICENSE(MASTER);

    public final String description;
    public final PasswordType requiredPassword;
    public final Collection<LicenseType> requiredLicenses;

    Action(PasswordType requiredPassword, LicenseType... requiredLicenses) {
        description = StringUtil.humanize(name());
        this.requiredPassword = requiredPassword;
        this.requiredLicenses = Arrays.asList(requiredLicenses);
    }
}
