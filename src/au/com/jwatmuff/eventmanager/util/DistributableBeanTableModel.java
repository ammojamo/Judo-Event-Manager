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

import au.com.jwatmuff.genericdb.distributed.Distributable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.event.TableModelListener;

/**
 *
 * @author James
 */
public class DistributableBeanTableModel<T extends Distributable> implements BeanTableModel<T> {
    BeanTableModel<T> tableModel;

    /** Creates a new instance of DistributableBeanTableModel */
    public DistributableBeanTableModel(BeanTableModel<T> tableModel) {
        this.tableModel = tableModel;
    }

    @Override
    public void setBeans(Collection<T> beans) {
        ArrayList<T> oldBeans = new ArrayList<T>(getBeans());
        ArrayList<T> newBeans = new ArrayList<T>(beans);
        
        for(T oldBean : oldBeans) {
            T newBean = findBeanWithID(newBeans, oldBean.getID());
            if(newBean == null)
                tableModel.removeBean(oldBean);
            else {
                tableModel.updateBean(oldBean, newBean);
                newBeans.remove(newBean);
            }
        }
        
        for(T newBean : newBeans) {
            tableModel.addBean(newBean);
        }
    }
    
    private T findBeanWithID(ArrayList<T> beans, Object id) {
        for(T bean : beans) {
            if(bean.getID().equals(id))
                return bean;
        }
        return null;
    }

    @Override
    public List<T> getBeans() {
        return tableModel.getBeans();
    }

    @Override
    public void updateBean(T oldBean, T bean) {
        tableModel.updateBean(oldBean, bean);
    }

    @Override
    public void removeBean(T bean) {
        tableModel.removeBean(bean);
    }

    @Override
    public void addBean(T bean) {
        tableModel.addBean(bean);
    }

    @Override
    public T getAtRow(int row) {
        return tableModel.getAtRow(row);
    }

    @Override
    public int getRowCount() {
        return tableModel.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return tableModel.getColumnCount();
    }

    @Override
    public String getColumnName(int i) {
        return tableModel.getColumnName(i);
    }

    @Override
    public Class getColumnClass(int i) {
        return tableModel.getColumnClass(i);
    }

    @Override
    public boolean isCellEditable(int i, int i0) {
        return tableModel.isCellEditable(i, i0);
    }

    @Override
    public Object getValueAt(int i, int i0) {
        return tableModel.getValueAt(i, i0);
    }

    @Override
    public void setValueAt(Object object, int i, int i0) {
        tableModel.setValueAt(object, i, i0);
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {
        tableModel.addTableModelListener(tableModelListener);
    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {
        tableModel.removeTableModelListener(tableModelListener);
    }
}
