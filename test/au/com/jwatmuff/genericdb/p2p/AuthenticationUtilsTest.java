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

import au.com.jwatmuff.genericdb.p2p.AuthenticationUtils.AuthenticationPair;
import junit.framework.TestCase;

/**
 *
 * @author James
 */
public class AuthenticationUtilsTest extends TestCase {
    
    public AuthenticationUtilsTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getAuthenticationPair method, of class AuthenticationUtils.
     */
    public void testGetAuthenticationPair_int() {
        System.out.println("getAuthenticationPair");

        int password = 234132;
        AuthenticationPair pair = AuthenticationUtils.getAuthenticationPair(password);
        
        assertNotNull(pair);
        assertEquals(true, AuthenticationUtils.checkAuthenticationPair(pair, password));
    }

    /**
     * Test of checkAuthenticationPair method, of class AuthenticationUtils.
     */
    public void testCheckAuthenticationPair() {
        System.out.println("checkAuthenticationPair");
        
        int password = 23415;
        int wrongPassword = 6473;
        
        boolean result = AuthenticationUtils.checkAuthenticationPair(null, password);
        assertEquals(false, result);
        
        AuthenticationPair pair = AuthenticationUtils.getAuthenticationPair(password);
        result = AuthenticationUtils.checkAuthenticationPair(pair, wrongPassword);
        assertEquals(false, result);
    }

    /**
     * Test of getAuthenticationPair method, of class AuthenticationUtils.
     */
    public void testGetAuthenticationPair_byteArr_int() {
        System.out.println("getAuthenticationPair");
        
        byte[] prefix = new byte[] { 32, 65, 34, 86, 121, -43 };
        int password = 54564;

        AuthenticationPair pair = AuthenticationUtils.getAuthenticationPair(prefix, password);
        
        assertNotNull(pair);
        assertEquals(true, AuthenticationUtils.checkAuthenticationPair(pair, password));
    }

    /**
     * Test of generatePrefix method, of class AuthenticationUtils.
     */
    public void testGeneratePrefix() {
        System.out.println("generatePrefix");

        byte[] result = AuthenticationUtils.generatePrefix();
        
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
