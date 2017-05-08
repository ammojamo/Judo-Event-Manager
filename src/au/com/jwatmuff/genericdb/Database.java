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

import java.util.List;

/**
 * Basic database interface to perform CRUD operations and execute named
 * queries with parameters.
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
