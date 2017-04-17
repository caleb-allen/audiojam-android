package com.torchlighttech.data.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.torchlighttech.data.effects.Effect;
import com.torchlighttech.data.effects.Flash;
import com.torchlighttech.data.effects.Strobe;

/**
 * Created by caleb on 4/17/17.
 */

public class SGson {
    private static Gson gson;
    public static Gson getInstance(){

        if (gson == null) {
            RuntimeTypeAdapterFactory<Effect> effectAdapter = RuntimeTypeAdapterFactory.of(Effect.class);
            effectAdapter
                    .registerSubtype(Flash.class, "flash")
                    .registerSubtype(Strobe.class, "strobe");

            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(effectAdapter)
                    .create();
        }
        return gson;
    }
}
