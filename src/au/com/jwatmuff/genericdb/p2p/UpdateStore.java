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

package au.com.jwatmuff.genericdb.p2p;

import java.io.IOException;

/**
 * An UpdateStore manages how data events are persisted on disk. It also keeps
 * track of a "committed position", so that in the event of a crash, events
 * that have not been committed to the database can be replayed.
 * 
 * Example implementations include writing events out to a file on disk,
 * or writing them to a table in the database.
 * 
 * @author james
 */
public interface UpdateStore {
    Update loadUpdate() throws IOException;
    void writePartialUpdate(Update update) throws IOException;
    void writeCommitedPosition(UpdatePosition position) throws IOException;
    UpdatePosition getCommittedPosition();
}
