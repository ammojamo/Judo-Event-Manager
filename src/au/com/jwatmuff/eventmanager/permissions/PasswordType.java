package au.com.jwatmuff.eventmanager.permissions;

import au.com.jwatmuff.eventmanager.util.StringUtil;

public enum PasswordType {
    MASTER, WEIGH_IN, PERSONAL_DETAILS, SCOREBOARD;

    public final String description;

    PasswordType() {
        description = StringUtil.humanize(name());
    }
}
