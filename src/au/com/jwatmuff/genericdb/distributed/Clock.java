/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        log.debug("Setting time to: " + new Date(time));
        startNano = System.nanoTime();
        startTime = time;
        log.debug("Verifying time is: " + getTime());
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
        if(time.getTime() > now()) {
            setTime(time.getTime());
        }
    }
}
