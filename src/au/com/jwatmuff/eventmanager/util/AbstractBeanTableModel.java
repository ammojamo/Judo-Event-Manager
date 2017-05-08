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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public abstract class AbstractBeanTableModel<T> extends AbstractTableModel implements BeanTableModel<T> {
    private static final Logger log = Logger.getLogger(AbstractBeanTableModel.class);

    private List<T> beans = new ArrayList<T>();
    
    @Override
    public int getRowCount() {
        return beans.size();
    }
    
    @Override
    public Class getColumnClass(int column) {
        /* try finding class by looking at any non-null value in the column */
        for(int i = 0; i < beans.size(); i++) {
            Object o = getValueAt(i, column);
            if(o != null) return o.getClass();
        }

        /* if all values are null, just return Object class */
        return Object.class;
    }
    
    @Override
    public void setBeans(Collection<T> beans) {
        this.beans = new ArrayList<T>(beans);
        this.fireTableDataChanged();            
    }
    
    @Override
    public List<T> getBeans() {
        return new ArrayList<T>(beans);
    }
    
    @Override
    public void updateBean(T oldBean, T bean) {
        int row = beans.indexOf(oldBean);
        if(row >= 0) {
            beans.set(row, bean);
            this.fireTableRowsUpdated(row, row);
        }
    }
    
    @Override
    public void removeBean(T bean) {
        int row = beans.indexOf(bean);
        if(row >= 0) {
            beans.remove(row);
            this.fireTableRowsDeleted(row, row);
        }
    }
    
    @Override
    public void addBean(T bean) {
        beans.add(bean);
        int row = beans.size()-1;
        this.fireTableRowsInserted(row, row);
    }

    @Override
    public T getAtRow(int row) {
        return beans.get(row);
    }
}
