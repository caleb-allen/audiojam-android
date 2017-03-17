
package com.torchlighttech.data;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class Effect implements Serializable
{
    public boolean flash;
    public Strobe strobe;
    public boolean fade;
}
