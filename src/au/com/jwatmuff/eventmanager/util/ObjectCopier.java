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
      ObjectOutputStream oos = null;
      ObjectInputStream ois = null;
      try
      {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         oos = new ObjectOutputStream(bos);
         oos.writeObject(object);
         oos.flush();
         ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
         ois = new ObjectInputStream(bin);
         T copy = (T)ois.readObject();
         oos.close();
         ois.close();
         return copy;
      }
      catch(Exception e)
      {
         throw(new RuntimeException("Exception while copying object", e));
      }
    }
}
