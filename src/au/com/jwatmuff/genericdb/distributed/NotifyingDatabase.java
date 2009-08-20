/*
 * NotifyingDatabase.java
 *
 * Created on 12 August 2008, 00:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import au.com.jwatmuff.genericdb.Database;
import java.util.List;

/**
 *
 * @author James
 */
public class NotifyingDatabase implements Database {
    private Database database;
    private DataEventListener listener;
    
    private boolean readOnly;
    private boolean updateTimestamps;
    
    /** Creates a new instance of NotifyingDatabase */
    public NotifyingDatabase(Database database) {
        this.database = database;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setUpdateTimestamps(boolean updateTimestamps) {
        this.updateTimestamps = updateTimestamps;
    }
    
    public void setListener(DataEventListener listener) {
        this.listener = listener;
    }
    
    private void fireEvent(DataEvent event) {
        if(listener != null)
            listener.handleDataEvent(event);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> void add(T item) {
        if(item instanceof Distributable) {
            Distributable tsItem = (Distributable)item;
            if(updateTimestamps) tsItem.setTimestamp(Clock.getTime());
            if(!readOnly) database.add(item);
            fireEvent(new DataEvent(tsItem, DataEvent.Type.CREATE));
        }
        else
            if(!readOnly) database.add(item);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void update(T item) {
        if(item instanceof Distributable) {
            Distributable tsItem = (Distributable)item;
            if(updateTimestamps) tsItem.setTimestamp(Clock.getTime());
            if(!readOnly) database.update(item);
            fireEvent(new DataEvent(tsItem, DataEvent.Type.UPDATE));
        }
        else
            if(!readOnly) database.update(item);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void delete(T item) {
        if(item instanceof Distributable) {
            Distributable tsItem = (Distributable)item;
            if(updateTimestamps) tsItem.setTimestamp(Clock.getTime());
            if(!readOnly) database.delete(item);
            fireEvent(new DataEvent(tsItem, DataEvent.Type.DELETE));
        }
        else
            if(!readOnly) database.delete(item);
    }

    @Override
    public <T> T get(Class<T> aClass, Object id) {
        return database.get(aClass, id);
    }

    @Override
    public <T> List<T> findAll(Class<T> aClass, String query, Object... args) {
        return database.findAll(aClass, query, args);
    }

    @Override
    public <T> T find(Class<T> aClass, String query, Object... args) {
        return database.find(aClass, query, args);
    }
}
