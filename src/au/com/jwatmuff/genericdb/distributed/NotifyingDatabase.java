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
            if(updateTimestamps) tsItem.setTimestamp(new Timestamp());
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
            if(updateTimestamps) tsItem.setTimestamp(new Timestamp());
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
            if(updateTimestamps) tsItem.setTimestamp(new Timestamp());
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
