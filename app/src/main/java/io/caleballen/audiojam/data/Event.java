
package io.caleballen.audiojam.data;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class Event implements Serializable
{

    @SerializedName("start_time")
    public int startTime;
    public int duration;
    public Effect effect;
    public Peripheral peripheral;
}
