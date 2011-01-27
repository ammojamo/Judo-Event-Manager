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
    ADD_CONTEST_AREA(MASTER),
    ADD_DIVISION(MASTER),
    ADD_MORE_THAN_TWO_MATS(null, D),
    ADD_MORE_THAN_SIX_MATS(null, Z),
    ADD_PLAYER(PERSONAL_DETAILS),
    ADD_SESSION(MASTER),
    APPROVE_DIVISION(MASTER),
    AUTO_ASSIGN_DIVISIONS(MASTER),
    CHANGE_PERSONAL_DETAILS_PASSWORD(MASTER),
    CHANGE_MASTER_PASSWORD(MASTER),
    CHANGE_RESULTS(MASTER),
    CHANGE_SCOREBOARD_PASSWORD(MASTER),
    CHANGE_WEIGH_IN(MASTER),
    CHANGE_WEIGH_IN_PASSWORD(MASTER),
    CREATE_COMPETITION(null, C),
    EDIT_DIVISION(MASTER),
    EDIT_PLAYER(MASTER),
    ENTER_WEIGH_IN(WEIGH_IN),
    EXPORT_PLAYERS(MASTER),
    IMPORT_DIVISIONS(MASTER),
    IMPORT_PLAYERS(MASTER),
    LOCK_DIVISION(MASTER),
    LOCK_DRAW(MASTER),
    LOCK_SESSION(MASTER),
    LOCK_SESSION_FIGHT_ORDER(MASTER),
    MANUAL_SCOREBOARD(null, B),
    MANUAL_FIGHT_PROGRESSION(null, B),
    OPEN_PLAYER(PERSONAL_DETAILS),
    UNLOCK_MASTER(MASTER),
    UPDATE_COMPETITION_DETAILS(MASTER),
    UPDATE_COMPETITION_LICENSE(MASTER),
    REMOVE_CONTEST_AREA(MASTER),
    REMOVE_DIVISION(MASTER),
    REMOVE_PLAYER(PERSONAL_DETAILS),
    REMOVE_SESSION(MASTER),
    SCOREBOARD_ENTRY(SCOREBOARD),
    WITHDRAW_PLAYER(MASTER);

    public final String description;
    public final PasswordType requiredPassword;
    public final Collection<LicenseType> requiredLicenses;

    Action(PasswordType requiredPassword, LicenseType... requiredLicenses) {
        description = StringUtil.humanize(name());
        this.requiredPassword = requiredPassword;
        this.requiredLicenses = Arrays.asList(requiredLicenses);
    }
}
