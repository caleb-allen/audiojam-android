
package com.torchlighttech.data;

import java.io.Serializable;
import java.util.List;

public class Show implements Serializable
{

    public String name;
    public String id;
    public List<Event> events = null;
}
