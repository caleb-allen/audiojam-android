
package com.torchlighttech.data;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class Event implements Serializable, Comparable<Event> {


    @SerializedName("start_time")
    public int startTime;
    public int duration;
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
