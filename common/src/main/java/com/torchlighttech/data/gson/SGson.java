package com.torchlighttech.data.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.torchlighttech.data.effects.Effect;
import com.torchlighttech.data.effects.Flash;
import com.torchlighttech.data.effects.Strobe;
import com.torchlighttech.data.peripherals.Peripheral;
import com.torchlighttech.data.peripherals.Screen;
import com.torchlighttech.data.peripherals.Torch;

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

            RuntimeTypeAdapterFactory<Peripheral> peripheralAdapter = RuntimeTypeAdapterFactory.of(Peripheral.class);

            peripheralAdapter
                    .registerSubtype(Screen.class, "screen")
                    .registerSubtype(Torch.class, "torch");

            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(effectAdapter)
                    .registerTypeAdapterFactory(peripheralAdapter)
                    .create();
        }
        return gson;
    }
}
