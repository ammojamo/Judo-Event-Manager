/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class AuthenticationUtils {
    private static final Logger log = Logger.getLogger(AuthenticationUtils.class);
    private static SecureRandom generator = new SecureRandom();
    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            log.error("Fatal error: ", e);
        }
    }

    private static int PREFIX_LENGTH = 16;

    private AuthenticationUtils() {}

    public static AuthenticationPair getAuthenticationPair(int password) {
        byte[] prefix = generatePrefix();
        return getAuthenticationPair(prefix, password);
    }
    
    public static AuthenticationPair getAuthenticationPair(byte[] prefix, int password) {
        byte[] data = concat(prefix, intToByte(password));
        return new AuthenticationPair(prefix, digest.digest(data));
    }

    public static boolean checkAuthenticationPair(AuthenticationPair pair, int password) {
        if(pair == null) return false;

        byte[] data = concat(pair.getPrefix(), intToByte(password));        
        byte[] hash = digest.digest(data);

        return Arrays.equals(hash, pair.getHash());
    }
    
    public static byte[] generatePrefix() {
        byte[] prefix = new byte[PREFIX_LENGTH];
        generator.nextBytes(prefix);
        return prefix;
    }

    private static byte[] intToByte(int i) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(i);
            dos.flush();
            bos.flush();
            return bos.toByteArray();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        for(int i=0; i<a.length; i++)
            c[i] = a[i];
        for(int i=0; i<b.length; i++)
            c[i + a.length] = b[i];
        
        return c;
    }
    
    public static class AuthenticationPair {
        private byte[] prefix;
        private byte[] hash;

        public AuthenticationPair(byte[] prefix, byte[] hash) {
            this.prefix = prefix;
            this.hash = hash;
        }
        
        public byte[] getPrefix() {
            return prefix;
        }
        
        public byte[] getHash() {
            return hash;
        }
    }
}
