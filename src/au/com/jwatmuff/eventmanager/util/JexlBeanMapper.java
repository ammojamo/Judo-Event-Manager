/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
