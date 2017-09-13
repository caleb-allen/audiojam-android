package io.caleballen.audiojam.data.effects;

import io.caleballen.audiojam.data.peripherals.IBinaryPeripheral;

import java.io.Serializable;

/**
 * Created by caleb on 4/17/17.
 */

public abstract class Effect implements Serializable{
    protected int duration;

    public abstract void execute(IBinaryPeripheral peripheral);
}
