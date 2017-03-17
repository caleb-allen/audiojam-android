
package com.torchlighttech.data;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Screen implements Serializable
{

    @SerializedName("on_color")
    public String onColor;
    @SerializedName("off_color")
    public String offColor;
}
