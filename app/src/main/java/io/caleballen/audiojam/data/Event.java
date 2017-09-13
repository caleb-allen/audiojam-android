
package io.caleballen.audiojam.data;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import io.caleballen.audiojam.data.effects.Effect;

public class Event implements Serializable, Comparable<Event> {


    @SerializedName("start_time")
    public int startTime;
    public Effect effect;
    public int track;

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
