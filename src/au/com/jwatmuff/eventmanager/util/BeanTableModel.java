/*
 * BeanTableModel.java
 *
 * Created on 17 August 2008, 00:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.util.Collection;
import java.util.List;
import javax.swing.table.TableModel;

/**
 *
 * @author James
 */
public interface BeanTableModel<T> extends TableModel {
    void setBeans(Collection<T> beans);
    List<T> getBeans();
    void updateBean(T oldBean, T bean);
    void removeBean(T bean);
    void addBean(T bean);
    T getAtRow(int row);
}
