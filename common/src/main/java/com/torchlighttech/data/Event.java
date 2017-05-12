
package com.torchlighttech.data;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import com.torchlighttech.data.effects.Effect;
import com.torchlighttech.data.peripherals.Peripheral;

public class Event implements Serializable, Comparable<Event> {


    @SerializedName("start_time")
    public int startTime;
    public Effect effect;
    public Peripheral peripheral;

    /**
     * Compare start time
     * @param event
     * @return
     */
    @Override
    public int compareTo(Event event) {
        return this.startTime - event.startTime;
    }
}
