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
    UPDATE_COMPETITION_LICENSE(MASTER),
    UNLOCK_MASTER(MASTER),
    CHANGE_RESULTS(MASTER),
    EDIT_PLAYER(PERSONAL_DETAILS),
    ADD_PLAYER(PERSONAL_DETAILS),
    REMOVE_PLAYER(PERSONAL_DETAILS),
    IMPORT_PLAYERS(MASTER),
    EXPORT_PLAYERS(MASTER),
    AUTO_ASSIGN_DIVISIONS(MASTER),
    ADD_DIVISION(MASTER),
    EDIT_DIVISION(MASTER),
    LOCK_DIVISION(MASTER),
    REMOVE_DIVISION(MASTER),
    APPROVE_DIVISION(MASTER),
    IMPORT_DIVISIONS(MASTER),
    LOCK_DRAW(MASTER),
    ADD_CONTEST_AREA(MASTER),
    REMOVE_CONTEST_AREA(MASTER),
    ADD_SESSION(MASTER),
    REMOVE_SESSION(MASTER),
    LOCK_SESSION(MASTER),
    LOCK_SESSION_FIGHT_ORDER(MASTER),
    SCOREBOARD_ENTRY(SCOREBOARD);

    public final String description;
    public final PasswordType requiredPassword;
    public final Collection<LicenseType> requiredLicenses;

    Action(PasswordType requiredPassword, LicenseType... requiredLicenses) {
        description = StringUtil.humanize(name());
        this.requiredPassword = requiredPassword;
        this.requiredLicenses = Arrays.asList(requiredLicenses);
    }
}
