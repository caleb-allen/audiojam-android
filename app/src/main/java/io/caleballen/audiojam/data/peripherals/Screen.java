
package io.caleballen.audiojam.data.peripherals;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class Screen extends Peripheral implements Serializable {
    @SerializedName("on_color")
    public String onColor;
    @SerializedName("off_color")
    public String offColor;
}
