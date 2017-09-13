package io.caleballen.audiojam.peripherals;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import io.caleballen.audiojam.data.peripherals.IBinaryPeripheral;

import java.io.IOException;

import timber.log.Timber;

/**
 * Created by caleb on 9/12/2017.
 */

public class Pin implements IBinaryPeripheral{
    private boolean enabled = false;

    private Gpio gpio;

    public Pin(String pinName, PeripheralManagerService managerService) throws IOException {
        gpio = managerService.openGpio(pinName);
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // pins are inverse
        enabled = !enabled;
        try {
            gpio.setValue(enabled);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disconnect(){
        try {
            gpio.close();
        } catch (IOException e) {
            Timber.e(e);
        }
    }
}
