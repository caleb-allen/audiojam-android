package io.caleballen.audiojam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

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

import com.torchlighttech.api.ApiClient;
import com.torchlighttech.data.Show;
import io.caleballen.audiojam.databinding.ActivityMainBinding;
import com.torchlighttech.events.IBinaryEffect;
import io.caleballen.audiojam.peripherals.TorchManager;
import com.torchlighttech.util.Sample;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLERATE = 44100;
    private static final int BUCKETS = 1024;
    private static final String SYNC_MESSAGE = "abcde";
    //    private static final int PACKET_DURATION = 1000; // in milliseconds
    private static final int PACKET_DURATION = 300; // in milliseconds
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
    public static boolean graphEnabled = false;
    private AudioRecord recorder = null;
    private boolean recording = false;
    private Thread recordingThread = null;
    private XYPlot plot;
    private short buffer[] = new short[BUCKETS];
    private Queue<Double[]> bufferFrames;
    private Double[] averages;
    private Queue<Long[]> timings;
    private Timing timing;
    private List<Sample> samples;
    private long iterations = 0;

    private Handler torchTimerHandler;
    private boolean startedSequence = false;
    private long startTime = 0;

    public final ObservableField<String> text = new ObservableField<>("");

    private IBinaryEffect torch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);
        plot = (XYPlot) findViewById(R.id.plot);

        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
        plot.setRangeUpperBoundary(100, BoundaryMode.FIXED);
        plot.setDomainLowerBoundary(0, BoundaryMode.FIXED);
        plot.setDomainUpperBoundary(BUCKETS, BoundaryMode.FIXED);


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

        Timber.i("Buffer Size: " + bufferSize);
        recorder.startRecording();
        recording = true;

        timings = new LinkedBlockingQueue<>();
        timing = new Timing();
        binding.setTime(timing);
        samples = new ArrayList<>();

        torch = new TorchManager(this);

        ApiClient.getApiClient().getShow().enqueue(new Callback<Show>() {
            @Override
            public void onResponse(Call<Show> call, Response<Show> response) {
                Timber.i(response.body().name);
            }

            @Override
            public void onFailure(Call<Show> call, Throwable t) {
                Timber.e(t);
            }
        });


        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (recording) {
                    iterations++;
                    long start = System.currentTimeMillis();
                    double[] data = fft();
                    long fftTime = System.currentTimeMillis();
                    Double[] freqAvgs = buffer(data);
                    String next = fskDemodulation(freqAvgs);
                    if (next != null) {
                        text.set(next);
                    }
                    if (next != null && next.equals(SYNC_MESSAGE) && samples != null && !samples.isEmpty()) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                    if (!startedSequence) {
                        startedSequence = true;
                        startTime = calculateStartTime();
                        scheduleNextTorch();
                    }
//                            }
//                        });
                    }
                    long avgsTime = System.currentTimeMillis();
                    graphData(freqAvgs);

                    long drawTime = System.currentTimeMillis();

                    timing(start, fftTime, avgsTime, drawTime);
                }
            }
        });
        recordingThread.start();
    }

    private void graphData(Double[] freqAvgs) {
        if (graphEnabled) {
            BarFormatter bf = new BarFormatter(Color.CYAN, Color.CYAN);
            XYSeries series = new SimpleXYSeries(Arrays.asList(freqAvgs), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Frequencies");
            plot.clear();
            plot.addSeries(series, bf);
            plot.redraw();
        } else {
            plot.clear();
        }
    }

    public void setGraphEnabled(boolean enabled) {
        Timber.i("Graph method");
        graphEnabled = enabled;
    }

    public void clearText() {
        text.set("");
        if (torchTimerHandler != null) {
            torchTimerHandler.removeCallbacks(torchRunnable);
        }
        if (torch.isEnabled()) {
            torch.setEnabled(false);
        }
        if (startedSequence) {
            startedSequence = false;
        }
    }

    private void scheduleNextTorch(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (torchTimerHandler == null) {
                    torchTimerHandler = new Handler();
                }
                long currentTime = System.currentTimeMillis();
                //time between turning light on and off
                long increment = 250;
                int period = (int) (((currentTime - startTime) / increment) + 1);
                long delay = (increment * (period)) - (currentTime - startTime);
                torchTimerHandler.postDelayed(torchRunnable, delay);
            }
        });
    }

    private Runnable torchRunnable = new Runnable() {
        @Override
        public void run() {
            torch.setEnabled(!torch.isEnabled());
            scheduleNextTorch();
        }
    };

    //--------audio processing------------

    private double[] fft() {
        recorder.read(buffer, 0, BUCKETS);
        DoubleFFT_1D fft = new DoubleFFT_1D(BUCKETS);
        double[] data = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            data[i] = (double) buffer[i];
        }
        fft.realForward(data);
        return data;
    }

    private Double[] buffer(double[] data) {
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

        return freqAvgs;

        //grab high frequencies and find median
                    /*Double[] highFreqs = Arrays.copyOfRange(freqAvgs, lowBucket, freqAvgs.length - 1);
                    Double[] sorted = highFreqs.clone();
                    Arrays.sort(sorted);*/
        //median of high frequencies
                    /*double median = (sorted[sorted.length / 2] + sorted[(sorted.length / 2) + 1]) / 2;

                    double avgAllFreqs = 0;
                    for (Double d : freqAvgs) {
                        avgAllFreqs += d;
                    }
                    avgAllFreqs /= freqAvgs.length;*/
    }

    private String fskDemodulation(Double[] freqAvgs) {
        int lowBucket = (int) (LOW_FREQ / BUCKET_SIZE);
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
//                            Timber.i(binData);
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
                    String subData = binData.substring(i * 8, (i + 1) * 8);
                    int charCode = Integer.parseInt(subData, 2);
                    char c = (char) charCode;
                    packet += c;
                    samples.add(new Sample(c, System.currentTimeMillis()));
                }
//                                Timber.i(packet);
                String prev = text.get();
                String next = "";
                if (prev.length() == 0) {
                    next = packet;
                } else if (!prev.endsWith(packet)) {
                    next = prev + packet;
                }
                if (!next.equals("")) {
                    return next;
                }else{
                    return prev;
                }
            } else {
                Timber.i("Rejected, uneven");
            }
        }
        return null;
    }

    private long calculateStartTime(){
        Timber.i("Calculating start time...");
        // go through samples and calculate start time
        Map<Character, Integer> reductionValue = new HashMap<>();
        char[] chars = SYNC_MESSAGE.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            reductionValue.put(c, i * PACKET_DURATION);
        }

        //take out any samples that
        int i = 0;
        while (i < samples.size()) {
            if (!reductionValue.containsKey(samples.get(i).getChar())) {
                Timber.w("Removing char from samples not found in SYNC Message: %s",
                        samples.get(i).getChar());
                samples.remove(i);
            }else{
                i++;
            }
        }

        Timber.i("Reducing samples...");
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
        Timber.i("High: " + high);
        Timber.i("Low: " + low);
        Timber.i("High - Low: " + (high - low));

        long startTime = low + (((high - PACKET_DURATION) - low) / 2);
        samples.clear();

        return startTime;
    }

    private void timing(long start, long fftTime, long avgsTime, long drawTime){
        Long[] ts = {
                fftTime - start,
                avgsTime - fftTime,
                drawTime - avgsTime
        };
        timings.add(ts);

        while (timings.size() > 50) {
            timings.poll();
        }

        if (iterations % 20 == 0) {
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

            timing.setAvgFft(avgFft);
            timing.setAvgAvg(avgAvg);
            timing.setAvgDraw(avgDraw);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        recording = false;
        recorder.stop();
    }
}
