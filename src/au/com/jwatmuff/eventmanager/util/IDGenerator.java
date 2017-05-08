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
