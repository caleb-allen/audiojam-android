package io.caleballen.audiojam;


import com.google.android.things.pio.PeripheralManagerService;
import com.torchlighttech.data.peripherals.IBinaryPeripheral;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.caleballen.audiojam.peripherals.Pin;
import timber.log.Timber;

public class BoardManager implements IBinaryPeripheral{

    private PeripheralManagerService service;
    private List<Pin> pins;

    public BoardManager() {
        this.service = new PeripheralManagerService();
        pins = new ArrayList<>();
        for (String pinName : getPinNames()) {
            try {
                pins.add(new Pin(pinName, service));
            } catch (IOException e) {
                Timber.e("Error connecting to pin:");
                Timber.e(e);
            }
        }
    }

    public void enablePin(int position) {
        pins.get(position).setEnabled(true);
    }

    public void disablePin(int position) {
        pins.get(position).setEnabled(false);
    }

    public void enableAllPins(boolean enabled) {
        for (Pin pin : pins) {
            pin.setEnabled(!enabled);
        }
    }

    public void disconnectAll(){
        for (Pin pin : pins) {
            pin.disconnect();
        }
        pins = null;
    }

    private static String[] getPinNames() {
        String[] pins = {
                "BCM2",
                "BCM3",
                "BCM4",
                "BCM17",
                "BCM27",
                "BCM22",
                "BCM10",
                "BCM9",
        };
        return pins;
    }

    @Override
    public void setEnabled(boolean enabled) {
        pins.get(0).setEnabled(!enabled);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}