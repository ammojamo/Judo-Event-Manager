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

package au.com.jwatmuff.eventmanager.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class ObjectCopier {
    
    /** Creates a new instance of ObjectCopier */
    private ObjectCopier() {
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T copy(T object) {
        try {
            return bytesToObject(objectToBytes(object));
        }catch(IOException | ClassNotFoundException e) {
            throw(new RuntimeException("Exception while copying object", e));
        }
    }
    
    public static byte[] objectToBytes(Serializable object) throws IOException {
        try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
        ) {
            oos.writeObject(object);
            oos.flush();
            return bos.toByteArray();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T bytesToObject(byte[] bytes)
            throws ClassNotFoundException, IOException {
        try (
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bin);
        ) {
            T copy = (T)ois.readObject();
            return copy;
        }
    }
}
