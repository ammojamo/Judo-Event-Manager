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

package au.com.jwatmuff.eventmanager.util;

import org.apache.commons.beanutils.ConvertingWrapDynaBean;

/**
 * Extends ConvertingWrapDynaBean from beanutils package to support Enum fields.
 *
 * Does not support indexed or mapped Enum fields at this stage.
 *
 * @author James
 */
public class EnumConvertingWrapDynaBean extends ConvertingWrapDynaBean {
    
    /** Creates a new instance of EnumConvertingWrapDynaBean */
    public EnumConvertingWrapDynaBean(Object instance) {
        super(instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void set(String name, Object value) {
        Class clazz = getDynaProperty(name).getType();
        if((clazz != null) && clazz.isEnum() && (value instanceof String)) {
            try {
                value = Enum.valueOf(clazz, (String)value);
            } catch(Exception e) {
                value = null;
            }
        }
        super.set(name, value);
    }
}
