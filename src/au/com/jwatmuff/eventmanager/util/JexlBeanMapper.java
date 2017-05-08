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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.context.HashMapContext;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class JexlBeanMapper<T> implements BeanMapper<T> {
    private static final Logger log = Logger.getLogger(JexlBeanMapper.class);
    
    private Map<String, String> mappings = new HashMap<String, String>();

    @Override
    public Map<String, Object> mapBean(T bean) {
        Map<String, Object> map = new HashMap<String, Object>();
        JexlContext jc = new HashMapContext();
        try {
            jc.setVars(PropertyUtils.describe(bean));
        } catch(Exception e) {
            log.error("Exception while reading bean properties", e);
            return map;
        }
        
        for(String prop : mappings.keySet()) {
            try {
                Expression e = ExpressionFactory.createExpression(mappings.get(prop));
                map.put(prop, e.evaluate(jc));
            } catch(Exception e) {
                log.error("Exception while mapping bean", e);
            }
        }
        
        return map;
    }
    
    public void addMapping(String property, String expression) {
        mappings.put(property, expression);
    }
}
