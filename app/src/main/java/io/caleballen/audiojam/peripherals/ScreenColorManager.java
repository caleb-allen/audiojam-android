package io.caleballen.audiojam.peripherals;

import android.content.Context;
import android.databinding.ObservableField;

import com.torchlighttech.data.peripherals.IBinaryPeripheral;
import com.torchlighttech.data.peripherals.IGradualPeripheral;
import com.torchlighttech.data.peripherals.Screen;

import timber.log.Timber;

/**
 * Created by caleb on 3/15/17.
 */

public class ScreenColorManager implements IBinaryPeripheral {
    private String colorOff;
    private String colorOn;
    private boolean enabled = false;
    ObservableField<String> screenColor;

    public ScreenColorManager(Screen screen, ObservableField<String> screenColor) {
        colorOff = screen.offColor;
        colorOn = screen.onColor;
        this.screenColor = screenColor;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            this.screenColor.set(colorOn);
        }else{
            this.screenColor.set(colorOff);
        }

        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
