package com.torchlighttech.data.effects;

import com.torchlighttech.data.peripherals.IBinaryPeripheral;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by caleb on 4/17/17.
 */

public class Flash extends Effect {
    private transient IBinaryPeripheral peripheral;
    private transient Timer timer;
    private transient TimerTask task;

    @Override
    public void execute(IBinaryPeripheral p) {
        peripheral = p;
        timer = new Timer();
        peripheral.setEnabled(true);
        task = new TimerTask() {
            @Override
            public void run() {
                peripheral.setEnabled(false);
            }
        };
        timer.schedule(task, duration);
    }
}
