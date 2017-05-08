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

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author James
 */
public class UpdateSyncInfo implements Serializable {
    public enum Status {
        OK,
        DATABASE_ID_MISMATCH,
        AUTH_FAILED,
        BAD_INFO}

    public Status status = Status.OK;
    public UUID senderID;
    public UUID databaseID;
    public byte[] authPrefix, authHash;
    public UpdatePosition position;
    public Update update;
    public Date senderTime;
}
