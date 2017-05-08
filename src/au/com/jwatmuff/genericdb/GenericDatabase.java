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

package au.com.jwatmuff.genericdb;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.util.ReflectionUtils;

/**
 *
 * @author James
 */
public class GenericDatabase implements Database {
    private static final Logger log = Logger.getLogger(GenericDatabase.class);
    
    private Map<Class, GenericDAO> daos = new HashMap<Class, GenericDAO>();
    
    /** Creates a new instance of BasicDatabase */
    public GenericDatabase() {
    }
    
    public <T> void addDAO(GenericDAO<T> dao) {
        daos.put(dao.getDataClass(), dao);
    }
    
    @SuppressWarnings("unchecked")
    private <T> GenericDAO<T> getDAOForItem(T item) {
        return (GenericDAO<T>)getDAOForClass(item.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T> GenericDAO<T> getDAOForClass(Class<? extends T> aClass) {
        GenericDAO dao = daos.get(aClass);
        if(dao == null)
            throw new RuntimeException("Could not find DAO for class " + aClass);
        return dao;
    }

    @Override
    public <T> void add(T item) {
        GenericDAO<T> dao = getDAOForItem(item);
        dao.add(item);
    }

    @Override
    public <T> void update(T item) {
        GenericDAO<T> dao = getDAOForItem(item);
        dao.update(item);
    }

    @Override
    public <T> void delete(T item) {
        GenericDAO<T> dao = getDAOForItem(item);
        dao.delete(item);
    }

    @Override
    public <T> T get(Class<T> aClass, Object id) {
        //logCounts();
        GenericDAO<T> dao = (GenericDAO<T>)getDAOForClass(aClass);
        return dao.get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> findAll(Class<T> aClass, String query, Object... args) {
        assert query != null : "Query may not be null";
        assert query.length() > 0 : "Query may not be empty string";

        //log.debug("findAll: " + aClass.getName() + ": " + query);
        
        GenericDAO<T> dao = (GenericDAO<T>)getDAOForClass(aClass);
        try {
            query = "find" + query.substring(0, 1).toUpperCase() + query.substring(1);
            Method[] methods = dao.getClass().getMethods();
            for(Method method : methods)
                if(method.getName().equals(query))
                    //try { return (Collection<T>)method.invoke(dao, args); } catch(Exception e) { log.error("doh", e); return null; }
                    return (List<T>)ReflectionUtils.invokeMethod(method, dao, args);
            throw new NoSuchMethodException();
        } catch(NoSuchMethodException e) {
            throw new RuntimeException("Could not find method for query '" + query + "' on type " + aClass , e);
        } catch(ClassCastException e) {
            throw new RuntimeException("Method did not return expected type", e);            
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T find(Class<T> aClass, String query, Object... args) {
        //log.debug("find: " + aClass.getName() + ": " + query);
        
        GenericDAO<T> dao = (GenericDAO<T>)daos.get(aClass);
        try {
            query = "find" + query.substring(0, 1).toUpperCase() + query.substring(1);
            Method[] methods = dao.getClass().getMethods();
            for(Method method : methods)
                if(method.getName().equals(query))
                    return (T)ReflectionUtils.invokeMethod(method, dao, args);
            throw new NoSuchMethodException();
        } catch(NoSuchMethodException e) {
            throw new RuntimeException("Could not find method for query '" + query + "'", e);
        } catch(ClassCastException e) {
            throw new RuntimeException("Method did not return expected type", e);
        }
    }
}
