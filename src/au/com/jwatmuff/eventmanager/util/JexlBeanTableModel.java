/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

/**
 *
 * @author James
 */
public class JexlBeanTableModel<T> extends BeanMapperTableModel<T> {
    private JexlBeanMapper<T> mapper = new JexlBeanMapper<T>();
    
    public JexlBeanTableModel() {
        super();
        super.setBeanMapper(mapper);
    }
    
    @Override
    public void addColumn(String name, String expression) {
        mapper.addMapping(name, expression);
        super.addColumn(name, name);
    }
}
