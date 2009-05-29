/*
 * EnumConvertingWrapDynaBean.java
 *
 * Created on 31 July 2008, 16:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
