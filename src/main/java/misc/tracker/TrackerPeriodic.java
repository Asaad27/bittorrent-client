package misc.tracker;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class TrackerPeriodic implements Runnable {

    public static final int SCHEDULER = 15;
    private final AtomicBoolean connectToTracker;

    public TrackerPeriodic(AtomicBoolean connectToTracker) {
        this.connectToTracker = connectToTracker;
    }

    @Override
    public void run() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                connectToTracker.set(true);
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 1000 * SCHEDULER, 1000 * SCHEDULER);
    }
}
