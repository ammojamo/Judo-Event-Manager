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

    private static int PREFIX_LENGTH = 16;

    private AuthenticationUtils() {}
    
    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static AuthenticationPair getAuthenticationPair(int password) {
        byte[] prefix = generatePrefix();
        return getAuthenticationPair(prefix, password);
    }
    
    public static AuthenticationPair getAuthenticationPair(byte[] prefix, int password) {
        byte[] data = concat(prefix, intToByte(password));
        return new AuthenticationPair(prefix, sha256(data));
    }

    public static boolean checkAuthenticationPair(AuthenticationPair pair, int password) {
        if(pair == null) return false;

        byte[] data = concat(pair.getPrefix(), intToByte(password));        
        byte[] hash = sha256(data);

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
