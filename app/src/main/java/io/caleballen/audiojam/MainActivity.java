package io.caleballen.audiojam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLERATE = 44100;
    private static final int BUCKETS = 1024;
    private static final int FRAMES_THRESHOLD = 4;
    //    private static final int SAMPLERATE = 8000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private boolean recording = false;
    private Thread recordingThread = null;
    private XYPlot plot;
    private short buffer[] = new short[BUCKETS];
    private Queue<Double[]> bufferFrames;
    private Double[] averages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        plot = (XYPlot) findViewById(R.id.plot);
//        plot.setRangeBoundaries(0, 22000, BoundaryMode.GROW);
        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
        plot.setRangeUpperBoundary(100, BoundaryMode.FIXED);
        plot.setDomainLowerBoundary(0, BoundaryMode.FIXED);
        plot.setDomainUpperBoundary(BUCKETS, BoundaryMode.FIXED);
//        plot.setDomainUpperBoundary(300, BoundaryMode.FIXED);

//        plot.setRenderMode(Plot.RenderMode.USE_BACKGROUND_THREAD);

        bufferFrames = new LinkedBlockingQueue<>();
        averages = new Double[BUCKETS];
        for (int i = 0; i < averages.length; i++) {
            averages[i] = 0D;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }

        final int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELS, AUDIO_ENCODING);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLERATE, CHANNELS, AUDIO_ENCODING, bufferSize);

        Timber.d("Buffer Size: " + bufferSize);
        recorder.startRecording();
        recording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (recording) {
                    recorder.read(buffer, 0, BUCKETS);
                    DoubleFFT_1D fft = new DoubleFFT_1D(BUCKETS);
                    double[] data = new double[buffer.length];
                    for (int i = 0; i < buffer.length; i++) {
                        data[i] = (double) buffer[i];
                    }
                    fft.realForward(data);

                    Double[] dData = new Double[data.length];
                    for (int i = 0; i < data.length; i++) {
                        dData[i] = Math.abs(data[i]);
                    }

                    //high pass filter
//                    Double[] dData = new Double[data.length / 2];
//                    for (int i = 0; i < data.length / 2; i++) {
//                        dData[i] = data[i + (data.length / 2)];
//                    }

                    if (bufferFrames.size() >= FRAMES_THRESHOLD) {
                        Double[] frameToRemove = bufferFrames.poll();
                        for (int i = 0; i < averages.length; i++) {
                            averages[i] -= frameToRemove[i];
                        }
                    }
                    if (bufferFrames.size() < FRAMES_THRESHOLD) {
                        bufferFrames.add(dData);
                        for (int i = 0; i < averages.length; i++) {
                            averages[i] += dData[i];
                        }
                    }

                    Double[] calcAverages = new Double[BUCKETS];
                    for (int i = 0; i < calcAverages.length; i++) {
                        calcAverages[i] = averages[i] / averages.length;
                    }

//                    LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);
                    BarFormatter bf = new BarFormatter(Color.CYAN, Color.CYAN);

                    XYSeries series = new SimpleXYSeries(Arrays.asList(calcAverages), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
//                    XYSeries series = new SimpleXYSeries(Arrays.asList(dData), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
                    plot.clear();
                    plot.addSeries(series, bf);
                    plot.redraw();
                    try {
                        Thread.sleep(33);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        recordingThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recording = false;
        recorder.stop();
    }
}
