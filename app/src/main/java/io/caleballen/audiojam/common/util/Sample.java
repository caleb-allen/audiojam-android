package io.caleballen.audiojam.common.util;


/**
 * Created by caleb on 3/8/17.
 */

public class Sample{
    private char c;
    private long time;

    public Sample(char c, long time) {
        this.c = c;
        this.time = time;
    }

    public char getChar() {
        return c;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
