package io.caleballen.audiojam.data.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.caleballen.audiojam.data.effects.Effect;
import io.caleballen.audiojam.data.effects.Flash;
import io.caleballen.audiojam.data.effects.Strobe;
import io.caleballen.audiojam.data.peripherals.Peripheral;
import io.caleballen.audiojam.data.peripherals.Screen;
import io.caleballen.audiojam.data.peripherals.Torch;

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
