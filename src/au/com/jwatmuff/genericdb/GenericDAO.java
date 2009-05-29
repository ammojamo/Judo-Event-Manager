/*
 * GenericDAO.java
 *
 * Created on 11 August 2008, 22:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb;

/**
 *
 * @author James
 */
public interface GenericDAO<T> {
    public void add(T item);
    public T get(Object id);
    public void update(T item);
    public void delete(T item);
    
    public Class<T> getDataClass();
}
