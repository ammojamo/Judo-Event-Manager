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
    A(null),
    B(A),
    C(B),
    D(C),
    Z(D);

    public static final LicenseType DEFAULT_LICENSE = A;

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
