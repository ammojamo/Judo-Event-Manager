/*
 * IDGenerator.java
 *
 * Created on 24 April 2008, 14:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.net.InetAddress;
import java.util.Random;

/**
 *
 * @author James
 */
public class IDGenerator {
    private static boolean valid = false;
    private static int compID;
    private static Random generator = new Random();
    
    /** Creates a new instance of IDGenerator */
    protected IDGenerator() {
    }
    
    private static void generateCompID() {
        try {
            compID = InetAddress.getLocalHost().getHostName().hashCode() % (2^8);
        } catch(Exception e) {
            compID = generator.nextInt() % (2^8);
        }
        valid = true;
    }
    
    public static int generate() {
        if(!valid)
            generateCompID();
        int random = generator.nextInt() * (2^23);
        return Math.abs(compID + (random << 8));
    }
}
