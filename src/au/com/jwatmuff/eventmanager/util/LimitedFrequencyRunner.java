/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author James
 */
public class LimitedFrequencyRunner {
    private static enum State {
        IDLE, ACTIVE, PENDING    
    }
    
    private State state = State.IDLE;

    private final Runnable runnable;
    private final Runnable internal_runnable = new Runnable() {
        @Override
        public void run() {
            do_run();
        }
    };

    private int period_ms;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    public LimitedFrequencyRunner(Runnable runnable, int period_ms) {
        this.runnable = runnable;
        this.period_ms = period_ms;
    }

    public synchronized void run() {
        switch(state) {
            case IDLE:
                do_run();
                break;
            case ACTIVE:
            case PENDING:
                state = State.PENDING;
                break;
        }
    }
    
    private synchronized void do_run() {        
        switch(state) {
            case PENDING:
            case IDLE:
                state = state.ACTIVE;
                runnable.run();
                executor.schedule(internal_runnable, period_ms, TimeUnit.MILLISECONDS);
                break;
            case ACTIVE:
                state = state.IDLE;
                break;
        }
    }
}