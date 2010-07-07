/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class LimitedFrequencyRunner {
    private static final Logger log = Logger.getLogger(LimitedFrequencyRunner.class);

    private static enum State {
        IDLE, RUNNING, REQUESTED
    }
    
    private State state = State.IDLE;

    private final Runnable runnable;
    private final int period_ms;
    private final List<CountDownLatch> startLatches = new ArrayList<CountDownLatch>();
    private final List<CountDownLatch> doneLatches = new ArrayList<CountDownLatch>();

    public LimitedFrequencyRunner(Runnable runnable, int period_ms) {
        this.runnable = runnable;
        this.period_ms = period_ms;
    }

    private CountDownLatch newStartLatch() {
        synchronized(startLatches) {
            CountDownLatch latch = new CountDownLatch(1);
            startLatches.add(latch);
            return latch;
        }
    }

    private CountDownLatch newDoneLatch() {
        synchronized(doneLatches) {
            CountDownLatch latch = new CountDownLatch(1);
            doneLatches.add(latch);
            return latch;
        }
    }

    private void openStartLatches() {
        synchronized(startLatches) {
            for(CountDownLatch latch : startLatches)
                latch.countDown();
            startLatches.clear();
        }
    }

    private void openDoneLatches() {
        synchronized(doneLatches) {
            for(CountDownLatch latch : doneLatches)
                latch.countDown();
            doneLatches.clear();
        }
    }

    public void run(boolean block) {
        if(block) {
            CountDownLatch startLatch = newStartLatch();
            CountDownLatch doneLatch = newDoneLatch();
            run();
            try {
                startLatch.await();
                doneLatch.await();
            } catch(InterruptedException e) {
                log.warn("Interrupted while waiting on latch", e);
            }
        } else {
            run();
        }
    }

    public synchronized void run() {
        switch(state) {
            case IDLE:
                state = State.RUNNING;
                runThread();
                break;
            case RUNNING:
            case REQUESTED:
                state = State.REQUESTED;
        }
    }

    private synchronized void runIfRequested() {
        switch(state) {
            case IDLE:
                log.error("This should never happen");
                break;
            case RUNNING:
                state = State.IDLE;
                break;
            case REQUESTED:
                state = State.RUNNING;
                runThread();
        }
    }

    private void runThread() {
        new Thread() {
            @Override
            public void run() {
                openStartLatches();
                runnable.run();
                openDoneLatches();
                try {
                    Thread.sleep(period_ms);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while sleeping", e);
                }
                runIfRequested();
            }
        }.start();
    }
}
