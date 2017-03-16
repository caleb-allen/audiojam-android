package io.caleballen.audiojam.events;

/**
 * Created by caleb on 3/15/17.
 */

public class ScreenColorManager implements IGradualEffect, IBinaryEffect {
    private String colorOff;
    private String colorOn;

    public ScreenColorManager(String colorOff, String colorOn) {
        this.colorOff = colorOff;
        this.colorOn = colorOn;
    }


    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setValue(int value) {

    }

    @Override
    public int getValue() {
        return 0;
    }
}
