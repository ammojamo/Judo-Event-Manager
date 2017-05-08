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

package au.com.jwatmuff.genericdb.distributed;

import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class Clock {
    private static final Logger log = Logger.getLogger(Clock.class);

    private static long startNano;
    private static long startTime;

    static {
        setTime(new Date().getTime());
    }

    private static void setTime(long time) {
        // log.debug("Setting time to: " + new Date(time));
        startNano = System.nanoTime();
        startTime = time;
        // log.debug("Verifying time is: " + getTime());
    }

    /* returns clock time now in milliseconds */
    private static long now() {
        long endNano = System.nanoTime();
        long elapsedTime = (endNano - startNano) / 1000000;
        return startTime + elapsedTime;
    }

    public static Timestamp getTime() {
        return new Timestamp(now());
    }

    public static void setEarliestTime(Date time) {
        if(time == null) {
            log.error("Time must not be null");
            return;
        }
        if(time.getTime() > now()) {
            setTime(time.getTime());
        }
    }
}
