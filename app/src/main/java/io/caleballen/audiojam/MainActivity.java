package io.caleballen.audiojam;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.caleballen.audiojam.util.Sample;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLERATE = 44100;
    private static final int BUCKETS = 1024;
    private static final String SYNC_MESSAGE = "abcde";
//    private static final int PACKET_DURATION = 1000; // in milliseconds
    private static final int PACKET_DURATION = 130; // in milliseconds
    private static final int LOW_FREQ = 18100;
    //width of each bucket in terms of frequency (~21.5332 Hz)
    private static final double BUCKET_SIZE = (((float) SAMPLERATE / 2) / (float) BUCKETS);
    //    private static final int LOW_FREQ = 17990;
    //01100001
    /**
     * how far apart is each bit in terms of buckets?
     * if BUCKETS is 512, this value should be 5
     * if BUCKETS is 1024, this value should be 10
     * etc
     */
    private static final int BINARY_BUCKET_DISTANCE = 10;
    private static final int BYTES_PER_PACKET = 1;
    private static final int BITS_PER_PACKET = (8 * BYTES_PER_PACKET) + 1;//plus checking bit
    private static final double BINARY_FREQ_INCREMENT = BUCKET_SIZE * BINARY_BUCKET_DISTANCE;
    private static final int FRAMES_THRESHOLD = 2;
    //    private static final int SAMPLERATE = 8000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final boolean GRAPH_ENABLED = false;
    private AudioRecord recorder = null;
    private boolean recording = false;
    private Thread recordingThread = null;
    private XYPlot plot;
    private short buffer[] = new short[BUCKETS];
    private Queue<Double[]> bufferFrames;
    private Double[] averages;
    private Queue<Long[]> timings;
    private List<Sample> samples;
    private long iterations = 0;

    private TextView txtMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        plot = (XYPlot) findViewById(R.id.plot);
        txtMessage = (TextView) findViewById(R.id.txt_message);
        txtMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtMessage.setText("");
            }
        });
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

        timings = new LinkedBlockingQueue<>();
        samples = new ArrayList<>();

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


                    if (true) {

                        String binData = "";
                        for (int i = 0; i < BITS_PER_PACKET; i++) {

                            int low = lowBucket + (int) ((i * (BINARY_FREQ_INCREMENT * 2)) / BUCKET_SIZE);
                            int high = low + (int) (BINARY_FREQ_INCREMENT / BUCKET_SIZE);
                            double l = freqAvgs[low];
                            double h = freqAvgs[high];
                            // avgFreqDiff / 6
                            if (freqAvgs[low] - freqAvgs[high] >= freqAvgs[high]) {
                                binData += "0";
                            } else if (freqAvgs[high] - freqAvgs[low] >= freqAvgs[low]) {
                                binData += "1";
                            }
                        }

                        if (binData.length() == BITS_PER_PACKET) {
//                            Timber.d(binData);
                            char[] charArray = binData.toCharArray();
                            //determine if byte even (to verify data integrity)
                            int total = 0;
                            for (int i = 0; i < charArray.length; i++) {
                                char c = charArray[i];
                                int bit = Integer.parseInt(String.valueOf(c));
                                total += bit;
                            }
                            boolean even = false;
                            if (total % 2 == 0) {
                                even = true;
                            }
                            if (even) {
                                // data is good
                                String packet = "";
                                for (int i = 0; i < BYTES_PER_PACKET; i++) {
//                                    Timber.d(i * 8 + "");
//                                    Timber.d((i + 1) * 8 + "");
                                    String subData = binData.substring(i * 8, (i + 1) * 8);
//                                    Timber.d(subData);
                                    int charCode = Integer.parseInt(subData, 2);
                                    char c = (char) charCode;
                                    packet += c;
                                    samples.add(new Sample(c, System.currentTimeMillis()));
                                }
//                                Timber.d(packet);
                                String prev = txtMessage.getText().toString();
                                String next = "";
                                if (prev.length() == 0) {
                                    next = packet;
                                } else if (!prev.endsWith(packet)) {
                                    next = prev + packet;
                                }
                                if (!next.equals("")) {
                                    final String finalNext = next;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtMessage.setText(finalNext);
                                        }
                                    });
                                }

                                if (next.equals(SYNC_MESSAGE) && samples != null && !samples.isEmpty()) {
                                    Timber.d("Calculating start time...");
                                    // go through samples and calculate start time
                                    Map<Character, Integer> reductionValue = new HashMap<>();
                                    char[] chars = SYNC_MESSAGE.toCharArray();
                                    for (int i = 0; i < chars.length; i++) {
                                        char c = chars[i];
                                        reductionValue.put(c, i * PACKET_DURATION);
                                    }

                                    Timber.d("Reducing samples...");
                                    for (Sample sample : samples) {
                                        long time = sample.getTime();
                                        int reduceBy = reductionValue.get(sample.getChar());
                                        sample.setTime(time - reduceBy);
                                    }

                                    long low = samples.get(0).getTime();
                                    long high = samples.get(0).getTime();

                                    for (Sample sample : samples) {
                                        if (sample.getTime() < low) {
                                            low = sample.getTime();
                                        }
                                        if (sample.getTime() > high) {
                                            high = sample.getTime();
                                        }
                                    }
                                    Timber.d("High: " + high);
                                    Timber.d("Low: " + low);
                                    Timber.d("High - Low: " + (high - low));

                                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        CameraManager camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

                                        try {
                                            String[] cameraIds = camera.getCameraIdList();
                                            ArrayList<String> flashIds = new ArrayList<>();
                                            for (String cameraId : cameraIds) {
                                                CameraCharacteristics characteristics = camera.getCameraCharacteristics(cameraId);
                                                Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                                                if (flashAvailable != null && flashAvailable) {
                                                    flashIds.add(cameraId);
                                                }
                                            }

                                            for (String id : flashIds) {
                                                camera.setTorchMode(id, true);
                                            }

                                        } catch (CameraAccessException e) {
                                            e.printStackTrace();
                                            //TODO error you need to close camera app
                                        }
                                    } else {
                                        //TODO pre-lollipop flash
                                    }

                                    samples.clear();
                                }

                            } else {
                                Timber.d("Rejected, uneven");
                            }
                        }
                    }

                    long avgsTime = System.currentTimeMillis();

                    if (GRAPH_ENABLED) {
                        BarFormatter bf = new BarFormatter(Color.CYAN, Color.CYAN);
                        XYSeries series = new SimpleXYSeries(Arrays.asList(freqAvgs), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
                        plot.clear();
                        plot.addSeries(series, bf);
                        plot.redraw();
                    }


                    long drawTime = System.currentTimeMillis();

                    Long[] ts = {
                            fftTime - start,
                            avgsTime - fftTime,
                            drawTime - avgsTime
                    };
                    timings.add(ts);

                    while (timings.size() > 50) {
                        timings.poll();
                    }

                    if (iterations % 100 == 0) {
                        long avgFft = 0;
                        long avgAvg = 0;
                        long avgDraw = 0;
                        for (Long[] timeFrame : timings) {
                            avgFft += timeFrame[0];
                            avgAvg += timeFrame[1];
                            avgDraw += timeFrame[2];
                        }
                        avgFft /= timings.size();
                        avgAvg /= timings.size();
                        avgDraw /= timings.size();
                        String s = String.format("FFT: %s\nBuffer: %s\nGraph: %s",
                                avgFft,
                                avgAvg,
                                avgDraw);
                        Timber.d(s);

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
