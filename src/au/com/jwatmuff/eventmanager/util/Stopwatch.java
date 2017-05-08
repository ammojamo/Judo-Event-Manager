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
