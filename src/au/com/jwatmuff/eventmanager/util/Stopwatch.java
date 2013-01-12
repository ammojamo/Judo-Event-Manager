/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author James
 */
public class Stopwatch {
    private Timer timer;
    private long time, period;
    private boolean direction;
    
    private Runnable runnable;
    private int runperiod = 1;
    
    private class StopwatchTask extends TimerTask {
        boolean firstRun = true;

        @Override
        public void run() {
            if(firstRun) {
                firstRun = false;
                if(time % runperiod == 0 && runnable != null)
                    runnable.run();
            }

            if(direction)
                time += period;
            else
                time -= period;

            if(time % runperiod == 0 && runnable != null)
                runnable.run();
        }
    };
    
    public Stopwatch(long period, boolean direction) {
        this.direction = direction;
        this.period = period;
    }
    
    public Stopwatch(long period, boolean direction, Runnable runnable, int runperiod) {
        this(period, direction);
        this.runperiod = runperiod;
        this.runnable = runnable;
    }

    public boolean isRunning() {
        return timer != null;
    }
    
    public void reset(long time) {
        stop();
        this.time = (time / runperiod) * runperiod;
    }
    
    public void direction(boolean direction) {
        stop();
        this.direction = direction;
    }

    public synchronized void start() {
        if(timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new StopwatchTask(), 0, period);            
        }
    }

    public synchronized void stop() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public boolean getDirection() {
        return direction;
    }

    public long getTime() {
        return time;
    }
}
