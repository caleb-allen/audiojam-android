
package io.caleballen.audiojam.data;

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
    private final static long serialVersionUID = 6751055597618192392L;

}
