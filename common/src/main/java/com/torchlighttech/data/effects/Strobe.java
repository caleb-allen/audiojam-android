
package com.torchlighttech.data.effects;

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.annotations.SerializedName;
import com.torchlighttech.data.peripherals.IBinaryPeripheral;

public class Strobe extends Effect implements Serializable {
    @SerializedName("time_on")
    public int timeOn;
    @SerializedName("time_off")
    public int timeOff;

    private transient IBinaryPeripheral peripheral;
    private transient Timer timer;
    private long startTime;

    @Override
    public void execute(IBinaryPeripheral p) {
        peripheral = p;
        startTime = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new Task(), 0);
    }

    private class Task extends TimerTask{

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > duration) {
                peripheral.setEnabled(false);
                return;
            }
            /*if (!peripheral.isEnabled()) {
                peripheral.setEnabled(true);
                timer.schedule(new Task(), timeOn);
            }else{
                peripheral.setEnabled(false);
                timer.schedule(new Task(), timeOff);
            }*/
            if (!peripheral.isEnabled()) {
                peripheral.setEnabled(true);
                timer.schedule(new Task(), getDelayNextStrobe(timeOff));
            }else{
                peripheral.setEnabled(false);
                timer.schedule(new Task(), getDelayNextStrobe(0));
            }
        }
    }

    public long getDelayNextStrobe(int time){
        long currentTime = System.currentTimeMillis();
        //time between turning light on and off
        long increment = timeOn + timeOff;
        int period = (int) (((currentTime - startTime) / increment) + 1);
        return (increment * (period)) - (currentTime - startTime) - time;
    }
}
