package au.com.jwatmuff.eventmanager.permissions;

import au.com.jwatmuff.eventmanager.util.StringUtil;

public enum LicenseType {
    /*
     * This is where license types are defined.
     *
     * If a parent license is specified, the child license automatically has
     * all the privileges of the parent license.
     *
     * See Action.java for defining what actions are allowed for a given license
     */
    FREE(null),
    FULL(FREE);

    public final String description;
    private final LicenseType parent;

    LicenseType(LicenseType parent) {
        description = StringUtil.humanize(name());
        this.parent = parent;
    }

    public boolean covers(LicenseType license) {
        LicenseType lt = this;
        while (lt != null) {
            if (lt.equals(license)) {
                return true;
            }
            lt = lt.parent;
        }
        return false;
    }
}
