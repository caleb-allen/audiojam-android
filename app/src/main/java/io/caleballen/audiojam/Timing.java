package io.caleballen.audiojam;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import io.caleballen.audiojam.BR;

/**
 * Created by caleb on 3/13/17.
 */

public class Timing extends BaseObservable{
    private long avgFft = 0;
    private long avgAvg = 0;
    private long avgDraw = 0;

    @Bindable
    public String getAvgFft() {
        return "FFT: " + avgFft;
    }

    @Bindable
    public String getAvgAvg() {
        return "Buffer: " + avgAvg;
    }

    @Bindable
    public String getAvgDraw() {
        return "Graph: " + avgDraw;
    }

    public void setAvgFft(long avgFft) {
        this.avgFft = avgFft;
        notifyPropertyChanged(BR.avgFft);
    }

    public void setAvgAvg(long avgAvg) {
        this.avgAvg = avgAvg;
        notifyPropertyChanged(BR.avgAvg);
    }

    public void setAvgDraw(long avgDraw) {
        this.avgDraw = avgDraw;
        notifyPropertyChanged(BR.avgDraw);
    }
}
