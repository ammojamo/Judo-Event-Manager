/*
 * Database.java
 *
 * Created on 11 August 2008, 21:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb;

import java.util.List;

/**
 *
 * @author James
 */
public interface Database {
    public <T> void add(T item);
    public <T> void update(T item);
    public <T> void delete(T item);
    public <T> T get(Class<T> aClass, Object id);

    public <T> List<T> findAll(Class<T> aClass, String query, Object... args);
    public <T> T find(Class<T> aClass, String query, Object... args);    
}
