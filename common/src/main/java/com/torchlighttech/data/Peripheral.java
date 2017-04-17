
package com.torchlighttech.data;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Peripheral implements Serializable
{

    @SerializedName("screen")
    @Expose
    public Screen screen;
    @SerializedName("torch")
    @Expose
    public boolean torch;

}
