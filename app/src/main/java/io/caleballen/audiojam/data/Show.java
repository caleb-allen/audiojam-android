
package io.caleballen.audiojam.data;

import java.io.Serializable;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Show implements Serializable
{

    public String name;
    public String id;
    public List<Event> events = null;
}
