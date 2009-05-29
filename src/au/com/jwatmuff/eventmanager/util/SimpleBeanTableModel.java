/*
 * BeanTableModel.java
 *
 * Created on 1 May 2008, 18:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;


/**
 *
 * @author James
 */
public class SimpleBeanTableModel<T> extends AbstractTableModel implements BeanTableModel<T> {
    private static Logger log = Logger.getLogger(SimpleBeanTableModel.class);
    
    private Class<T> clazz;

    private List<T> beans = new ArrayList<T>();
    private List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    
    /** Creates a new instance of BeanTableModel */
    public SimpleBeanTableModel(Class<T> clazz) {
        assert clazz != null;

        this.clazz = clazz;
    }

    @Override
    public int getRowCount() {
        return beans.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        T bean = beans.get(row);
        ColumnInfo cinfo = columns.get(column);
        
        try {
            return cinfo.readPropertyMethod.invoke(bean);
        } catch(Exception e) {
            log.error("Couldn't read property '" + cinfo.property + "'", e);
            return null;
        }
    }
    
    @Override
    public String getColumnName(int column) {
        return columns.get(column).name;
    }
    
    @Override
    public Class getColumnClass(int column) {
        if(beans.size() > 0) {
            return getValueAt(0, column).getClass();
        } else {
            return Object.class;
        }
    }
    
    public void addColumn(String columnName, String property) {
        try {
            columns.add(new ColumnInfo(columnName, property));
        } catch(Exception e) {
            log.error("Could not add column for property '" + property + "' in class " + clazz.getName(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void setBeans(Collection<T> beans) {
        this.beans = new ArrayList<T>(beans);                
        fireTableDataChanged();
    }
    
    @Override
    public T getAtRow(int row) {
        return beans.get(row);
    }

    @Override
    public List<T> getBeans() {
        return new ArrayList<T>(beans);
    }

    @Override
    public void updateBean(T oldBean, T bean) {
        int index = beans.indexOf(oldBean);
        if(index >= 0) {
            beans.set(index, bean);
            fireTableRowsUpdated(index, index);
        }
    }

    @Override
    public void removeBean(T bean) {
        int index = beans.indexOf(bean);
        if(index >= 0) {
            beans.remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    @Override
    public void addBean(T bean) {
        beans.add(bean);
        int index = beans.size() - 1;
        fireTableRowsInserted(index,index);
    }
    
    private class ColumnInfo {
        String name;
        String property;
        Method readPropertyMethod;
        
        public ColumnInfo(String name, String property) {
            this.name = name;
            this.property = property;
            readPropertyMethod = BeanUtils.getPropertyDescriptor(clazz, property).getReadMethod();
        }        
    }
}
