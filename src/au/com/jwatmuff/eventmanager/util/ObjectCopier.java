/*
 * ObjectCopier.java
 *
 * Created on 13 August 2008, 17:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
