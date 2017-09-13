package io.caleballen.audiojam;

import io.caleballen.audiojam.data.Event;
import io.caleballen.audiojam.data.Show;
import io.caleballen.audiojam.data.peripherals.IBinaryPeripheral;

import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

/**
 * Created by caleb on 9/12/2017.
 */

public class ShowRunner {

    private Show show;
    private BoardManager boardManager;

    public ShowRunner(Show show, BoardManager boardManager, long startTime) {
        this.show = show;
        this.boardManager = boardManager;
        scheduleAllEvents(startTime);
    }

    private void scheduleAllEvents(long startTime){
        Timer t = new Timer();
        for (final Event nextEvent : show.events) {

            long currentTime = System.currentTimeMillis();
            long delay = (nextEvent.startTime - (currentTime - startTime));
            if (delay < 0) {
                Timber.i(delay + " in the past, removing event");
                continue;
            }
            IBinaryPeripheral p = null;

            if (nextEvent.track < boardManager.pinCount()) {
                p = boardManager.getPin(nextEvent.track);
            }
            //get peripheral
            final IBinaryPeripheral peripheral = p;

            if (p == null) {
                Timber.e("No peripheral assigned: Track " + nextEvent.track);
                continue;
            }
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    nextEvent.effect.execute(peripheral);
                }
            };
            t.schedule(task, delay);
        }
    }
}
