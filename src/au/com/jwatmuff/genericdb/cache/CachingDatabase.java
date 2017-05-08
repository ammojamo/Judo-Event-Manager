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

package au.com.jwatmuff.genericdb.cache;

import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.Distributable;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class CachingDatabase implements TransactionalDatabase {
    private static final Logger log = Logger.getLogger(CachingDatabase.class);

    private Map<Class,Map> caches = Collections.synchronizedMap(new HashMap<Class,Map>());
    //Collections.synchronizedMap(new LRUMap(1000));
    private Map<QueryInfo,List> findAllCache;
    private Map<QueryInfo,Object> findCache;

    private TransactionalDatabase database;
    
    private int hits, misses;
    
    public CachingDatabase(TransactionalDatabase database) {
        this.database = database;
        clearQueryCaches();
    }
    
    private void clearQueryCaches() {
        findAllCache = Collections.synchronizedMap(new HashMap<QueryInfo,List>());
        findCache = Collections.synchronizedMap(new HashMap<QueryInfo,Object>());        
    }
    
    @SuppressWarnings("unchecked")
    private Map getCache(Class c) {
        if(!caches.containsKey(c))
            caches.put(c, Collections.synchronizedMap(new LRUMap(1000)));
        return caches.get(c);
    }
            
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> void add(T item) {
        clearQueryCaches();
        database.add(item);
        if(item instanceof Distributable && item instanceof Serializable) {
            item = (T)ObjectCopier.copy((Serializable)item);
            Distributable d = (Distributable)item;
            Map<Object,T> cache = (Map<Object,T>) getCache(item.getClass());
            cache.put(d.getID(), item);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void update(T item) {
        clearQueryCaches();
        database.update(item);
        if(item instanceof Distributable && item instanceof Serializable) {
            item = (T)ObjectCopier.copy((Serializable)item);
            Distributable d = (Distributable)item;
            Map<Object,T> cache = (Map<Object,T>) getCache(item.getClass());
            cache.put(d.getID(), item);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void delete(T item) {
        clearQueryCaches();
        database.update(item);
        if(item instanceof Distributable) {
            Distributable d = (Distributable)item;
            Map<Object,T> cache = (Map<Object,T>) getCache(item.getClass());
            cache.remove(d.getID());
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> aClass, Object id) {
        if(id != null) {
            Map<Object,T> cache = (Map<Object,T>) getCache(aClass);
            T item = cache.get(id);
            if(item == null) {
                item = database.get(aClass, id);
                cache.put(id, item);
            }
            if(item instanceof Serializable)
                return (T)ObjectCopier.copy((Serializable)item);
            else
                return item;
        }
        
        return database.get(aClass, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> aClass, String query, Object... args) {
        QueryInfo qi = new QueryInfo(aClass, query, this,args);
        if(findAllCache.containsKey(qi)) {
            List<T> results = (List<T>)findAllCache.get(qi);
            return (List<T>)ObjectCopier.copy((Serializable)results);
        }
        else {
            List<T> results = database.findAll(aClass, query, args);
            findAllCache.put(qi, results);
            return (List<T>)ObjectCopier.copy((Serializable)results);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> aClass, String query, Object... args) {
        QueryInfo qi = new QueryInfo(aClass, query, this,args);
        if(findCache.containsKey(qi)) {
            T result = (T)findCache.get(qi);
            return (T)ObjectCopier.copy((Serializable)result);
        }
        else {
            T result = database.find(aClass, query, args);
            findCache.put(qi, result);
            return (T)ObjectCopier.copy((Serializable)result);
        }
    }

    @Override
    public void perform(Transaction t) {
        database.perform(t);
    }

}
