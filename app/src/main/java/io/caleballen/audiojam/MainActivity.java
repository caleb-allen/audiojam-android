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
    private static final int LOW_FREQ = 18100;
    //width of each bucket in terms of frequency (~21.5332 Hz)
    private static final double BUCKET_SIZE = (((float)SAMPLERATE / 2) / (float)BUCKETS);
    //    private static final int LOW_FREQ = 17990;
    //01100001
    /**
     * how far apart is each bit in terms of buckets?
     * if BUCKETS is 512, this value should be 5
     * if BUCKETS is 1024, this value should be 10
     * etc
     */
    private static final int BINARY_BUCKET_DISTANCE = 10;
    private static final double BINARY_FREQ_INCREMENT = BUCKET_SIZE * BINARY_BUCKET_DISTANCE;
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
    private Queue<Long[]> times;
    private long iterations = 0;


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

        times = new LinkedBlockingQueue<>();

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (recording) {
                    iterations++;
                    long start = System.currentTimeMillis();
                    recorder.read(buffer, 0, BUCKETS);
                    DoubleFFT_1D fft = new DoubleFFT_1D(BUCKETS);
                    double[] data = new double[buffer.length];
                    for (int i = 0; i < buffer.length; i++) {
                        data[i] = (double) buffer[i];
                    }
                    fft.realForward(data);
                    long fftTime = System.currentTimeMillis();
                    Double[] dData = new Double[data.length];
                    for (int i = 0; i < data.length; i++) {
                        dData[i] = Math.abs(data[i]);
                    }

                    //high pass filter
//                    Double[] dData = new Double[data.length / 2];
//                    for (int i = 0; i < data.length / 2; i++) {
//                        dData[i] = data[i + (data.length / 2)];
//                    }
                    // remove frames from buffer
                    if (bufferFrames.size() >= FRAMES_THRESHOLD) {
                        Double[] frameToRemove = bufferFrames.poll();
                        for (int i = 0; i < averages.length; i++) {
                            averages[i] -= frameToRemove[i];
                        }
                    }
                    // add frame to buffer
                    if (bufferFrames.size() < FRAMES_THRESHOLD) {
                        bufferFrames.add(dData);
                        for (int i = 0; i < averages.length; i++) {
                            averages[i] += dData[i];
                        }
                    }

                    Double[] freqAvgs = new Double[BUCKETS];

                    for (int i = 0; i < freqAvgs.length; i++) {
                        freqAvgs[i] = averages[i] / averages.length;
                    }

                    //grab high frequencies and find median
                    int lowBucket = (int) (LOW_FREQ / BUCKET_SIZE);
                    Double[] highFreqs = Arrays.copyOfRange(freqAvgs, lowBucket, freqAvgs.length - 1);
                    Double[] sorted = highFreqs.clone();
                    Arrays.sort(sorted);
                    //median of high frequencies
                    double median = (sorted[sorted.length / 2] + sorted[(sorted.length / 2) + 1]) / 2;

                    double avgAllFreqs = 0;
                    for (Double d : freqAvgs) {
                        avgAllFreqs += d;
                    }
                    avgAllFreqs /= freqAvgs.length;



//                    Timber.d(median + "");
                    if (median > avgAllFreqs) {
//                        Timber.d("In loop");

//                        double avgFreqDiff = 0;
//                        for(int i = 0; i < 8; i++) {
////                            Timber.d((BINARY_FREQ_INCREMENT / BUCKET_SIZE) + "");
//
//                            int low = lowBucket + (int) ((i * (BINARY_FREQ_INCREMENT * 2)) / BUCKET_SIZE);
//                            int high = low + (int) (BINARY_FREQ_INCREMENT / BUCKET_SIZE);
//                            avgFreqDiff += Math.abs(freqAvgs[low] - freqAvgs[high]);
//                        }
//                        avgFreqDiff /= 8;
                        String binData = "";

                        for(int i = 0; i < 8; i++) {
//                            double lD = (i * (BINARY_FREQ_INCREMENT * 2)) / BUCKET_SIZE;
//                            Timber.d("double: " + lD);
//                            Timber.d("cast to int: " + (int) lD);
//                            Timber.d((BINARY_FREQ_INCREMENT / BUCKET_SIZE) + "");
                            int low = lowBucket + (int) ((i * (BINARY_FREQ_INCREMENT * 2)) / BUCKET_SIZE);
                            int high = low + (int) (BINARY_FREQ_INCREMENT / BUCKET_SIZE);

                            /*if (freqAvgs[low] > avgFreqDiff && freqAvgs[high] < avgFreqDiff) {
//                                Timber.d("0");
                                double l = freqAvgs[low];
                                double h = freqAvgs[high];
                                binData += "0";
                            } else if (freqAvgs[low] < avgFreqDiff && freqAvgs[high] > avgFreqDiff) {
//                                Timber.d("1");
                                binData += "1";
                            }*/
                            double l = freqAvgs[low];
                            double h = freqAvgs[high];
                            // avgFreqDiff / 6
                            if (freqAvgs[low] - freqAvgs[high] >= 0) {
                                binData += "0";
                            } else if (freqAvgs[high] - freqAvgs[low] >= 0) {
                                binData += "1";
                            }
                        }
                        if (binData.length() >= 8) {
                            Timber.d(binData);
                            int charCode = Integer.parseInt(binData, 2);
                            String s = new Character((char) charCode).toString();
                            Timber.d(s);
//                            Timber.d(avgFreqDiff + "");
                        }
                        //01100001

//                        if (freqAvgs[lowBucket] > median && freqAvgs[lowBucket + (int)(BINARY_FREQ_INCREMENT / BUCKET_SIZE)] < median) {
//                            Timber.d("0");
//                        } else if (freqAvgs[lowBucket] < median && freqAvgs[lowBucket + (int) (BINARY_FREQ_INCREMENT / BUCKET_SIZE)] > median) {
//                            Timber.d("1");
//                        }
                    }

                    long avgsTime = System.currentTimeMillis();


//                    LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);
                    BarFormatter bf = new BarFormatter(Color.CYAN, Color.CYAN);

                    XYSeries series = new SimpleXYSeries(Arrays.asList(freqAvgs), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
//                    XYSeries series = new SimpleXYSeries(Arrays.asList(dData), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
                    plot.clear();
                    plot.addSeries(series, bf);
                    plot.redraw();

                    long drawTime = System.currentTimeMillis();

                    Long[] ts = {
                            fftTime - start,
                            avgsTime - fftTime,
                            drawTime - avgsTime
                    };
                    times.add(ts);

                    while (times.size() > 50) {
                        times.poll();
                    }

                    if (iterations % 50 == 0) {
                        long avgFft = 0;
                        long avgAvg = 0;
                        long avgDraw = 0;
                        for (Long[] timeFrame : times) {
                            avgFft += timeFrame[0];
                            avgAvg += timeFrame[1];
                            avgDraw += timeFrame[2];
                        }
                        avgFft /= times.size();
                        avgAvg /= times.size();
                        avgDraw /= times.size();
                        String s = String.format("FFT: %s\nBuffer: %s\nGraph: %s",
                                avgFft,
                                avgAvg,
                                avgDraw);
                        Timber.d(s);

                    }
//                    try {
//                        Thread.sleep(33);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
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
