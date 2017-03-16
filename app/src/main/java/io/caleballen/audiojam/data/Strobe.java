
package io.caleballen.audiojam.data;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Strobe implements Serializable
{

    @SerializedName("time_on")
    public int timeOn;
    @SerializedName("time_off")
    public int timeOff;

}
