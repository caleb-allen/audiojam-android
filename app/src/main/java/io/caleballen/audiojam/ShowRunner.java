package io.caleballen.audiojam;

import com.torchlighttech.data.Event;
import com.torchlighttech.data.Show;
import com.torchlighttech.data.peripherals.IBinaryPeripheral;
import com.torchlighttech.data.peripherals.Screen;
import com.torchlighttech.data.peripherals.Torch;

import java.util.Timer;
import java.util.TimerTask;

import io.caleballen.audiojam.peripherals.ScreenColorManager;
import io.caleballen.audiojam.peripherals.TorchManager;
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

            if (nextEvent.peripheral instanceof Torch) {
                p = boardManager;
            }
            //get peripheral
            final IBinaryPeripheral peripheral = p;

            if (p == null) {
                Timber.e("No peripheral assigned");
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
