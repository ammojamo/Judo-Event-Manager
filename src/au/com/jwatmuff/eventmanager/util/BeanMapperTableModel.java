/*
 * BeanMapperTableModel.java
 *
 * Created on 2 May 2008, 02:25
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class BeanMapperTableModel<T> extends AbstractBeanTableModel<T> {
    private static final Logger log = Logger.getLogger(BeanMapperTableModel.class);

    private List<ColumnInfo> columns = new ArrayList<ColumnInfo>();    
    private BeanMapper<T> mapper;
    private Map<T, Map<String, Object>> mapCache = new HashMap<T, Map<String, Object>>();
    
    /** Creates a new instance of BeanMapperTableModel */
    public BeanMapperTableModel(BeanMapper<T> mapper) {
        setBeanMapper(mapper);
    }
    
    public BeanMapperTableModel() { }
    
    public void setBeanMapper(BeanMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object getValueAt(int row, int column) {
        try {
            T bean = getAtRow(row);
            Map<String, Object> rowData;
            if(mapCache.containsKey(bean)) {
                rowData = mapCache.get(bean);
            }
            else  {
                rowData = mapper.mapBean(bean);
                mapCache.put(bean, rowData);
            }

            ColumnInfo cinfo = columns.get(column);
            return rowData.get(cinfo.property);
        } catch(Exception e) {
            log.error("Exception in table model", e);
            return null;
        }
    }
    
    @Override
    public int getColumnCount() {
        return columns.size();
    }
    
    @Override
    public String getColumnName(int column) {
        return columns.get(column).name;
    }
    
    @Override
    public void setBeans(Collection<T> beans) {
        mapCache.clear();
        super.setBeans(beans);
    }
    
    @Override
    public void updateBean(T oldBean, T bean) {
        mapCache.remove(oldBean);
        super.updateBean(oldBean, bean);
    }
    
    @Override
    public void removeBean(T bean) {
        mapCache.remove(bean);
        super.removeBean(bean);
    }
    
    public void addColumn(String columnName, String property) {
        columns.add(new ColumnInfo(columnName, property));
    }
    
    private class ColumnInfo {
        String name;
        String property;
        
        public ColumnInfo(String name, String property) {
            this.name = name;
            this.property = property;
        }
    }    
}
