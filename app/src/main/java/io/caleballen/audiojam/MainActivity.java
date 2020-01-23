package io.caleballen.audiojam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableField;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import com.torchlighttech.data.Event;
import com.torchlighttech.data.Show;
import io.caleballen.audiojam.databinding.ActivityMainBinding;
import com.torchlighttech.data.peripherals.IBinaryPeripheral;

import io.caleballen.audiojam.peripherals.ScreenColorManager;
import io.caleballen.audiojam.peripherals.TorchManager;

import com.torchlighttech.data.peripherals.Screen;
import com.torchlighttech.data.peripherals.Torch;
import com.torchlighttech.util.Sample;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLERATE = 44100;
    private static final int BUCKETS = 2048;
//    private static final int BUCKETS = 2048;
//    private static final String SYNC_MESSAGE = "abcde";
    //    private static final int PACKET_DURATION = 1000; // in milliseconds
    private static final int PACKET_DURATION = 300; // in milliseconds
    private static final int LOW_FREQ = 18100;
    private static final int GRAPH_LOWER_THRESHOLD = 2;
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
    private short buffer[] = new short[BUCKETS];
    private Queue<Double[]> bufferFrames;
    private Double[] averages;
    private Queue<Long[]> timings;
    private Timing timing;
    private List<Sample> samples;
    private long iterations = 0;

    public static boolean graphEnabled = true;
    private AudioRecord recorder = null;
    private boolean recording = false;

    private Thread recordingThread = null;

    private Show show;

    private XYPlot plot;
    private View colorView;

    private boolean startedSequence = false;
    private long startTime = 0;

    public final ObservableField<String> text = new ObservableField<>("");
    public final ObservableField<String> screenColor = new ObservableField<>("#ffffff");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);
        plot = (XYPlot) findViewById(R.id.plot);

        plot.setRangeLowerBoundary(GRAPH_LOWER_THRESHOLD, BoundaryMode.FIXED);
        plot.setRangeUpperBoundary(11, BoundaryMode.FIXED);
        plot.setDomainLowerBoundary(0, BoundaryMode.FIXED);
        plot.setDomainUpperBoundary(BUCKETS / 10, BoundaryMode.FIXED);

        colorView = findViewById(R.id.color_view);
        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
            }
        });

        screenColor.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (colorView.getVisibility() == View.GONE) {
                            colorView.setVisibility(View.VISIBLE);
                        }
                        colorView.setBackgroundColor(Color.parseColor(screenColor.get()));
                    }
                });
            }
        });

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

        ApiClient.getInstance().getShow(new Callback<Show>() {
            @Override
            public void success(Show s, Response response) {
                show = s;
                Collections.sort(show.events);
            }

            @Override
            public void failure(RetrofitError error) {
                Timber.e(error);
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
//                    String next = fskDemodulation(freqAvgs);
                    String next = null; // no op
                    if (next != null) {
                        text.set(next);
                    }
                    if (next != null && samples != null && !samples.isEmpty()) {
                        // show ID (2 chars)
                        int index = next.lastIndexOf("ab");
                        String showTimeStamp;
                        String syncMessage = "";
                        int timeStamp = 0;
                        boolean validMessage = false;
                        if (index >= 0) {
                            try {
                                showTimeStamp = next.substring(index + 2, index + 5);
                                Timber.i("Showtimestamp: " + showTimeStamp);
                                syncMessage = "ab" + showTimeStamp;
                                timeStamp = Integer.parseInt(showTimeStamp, 10) * 1000;
                                Timber.i("String to Integer Timestamp: " + showTimeStamp + " -> "
                                + timeStamp);
                                Timber.i("Valid message: " + syncMessage);
                                validMessage = true;
                            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                                validMessage = false;
                            }
                        }
                        if (validMessage) {
                            Timber.i(syncMessage);
                            if (!startedSequence) {
                                recording = false;
                                recorder.stop();
                                startedSequence = true;
                                startTime = calculateStartTime(syncMessage, timeStamp);
                                scheduleNextEvent();
                            }
                        }
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
        graphEnabled = enabled;
    }

    public void clearText() {
        text.set("");
        if (samples != null) {
            samples.clear();
        }
        if (startedSequence) {
            startedSequence = false;
        }
    }

    private void scheduleNextEvent(){
        Timer t = new Timer();
        for (final Event nextEvent : show.events) {

            long currentTime = System.currentTimeMillis();
            long delay = (nextEvent.startTime - (currentTime - startTime));
//            Timber.i("Delay: " + delay / 1000);
            if (delay < 0) {
                Timber.i(delay + " in the past, removing event");
                continue;
            }
            IBinaryPeripheral p = null;
//            p = nextEvent.peripheral.getClass().
            if (nextEvent.peripheral instanceof Torch) {
                p = new TorchManager(this);
            }else if(nextEvent.peripheral instanceof Screen){
                p = new ScreenColorManager((Screen) nextEvent.peripheral, screenColor);
            }

            //get peripheral
            final IBinaryPeripheral peripheral = p;

            if (p == null) {
                Timber.e("No peripheral assigned");
                continue;
            }
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    nextEvent.effect.execute(peripheral);
                }
            };
            t.schedule(task, delay);
        }
    }

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

    /**
     *
     * @param data
     * @return
     */
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
            freqAvgs[i] = Math.log(averages[i] / averages.length);
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
        //grab high frequencies and find median
//        Double[] upperFreqs = Arrays.copyOfRange(freqAvgs, lowBucket, freqAvgs.length - 1);
//        Double[] sorted = upperFreqs.clone();
//        Arrays.sort(upperFreqs);
//        double median = (sorted[sorted.length / 2] + sorted[(sorted.length / 2) + 1]) / 2;

        /*double mean = 0;
        for (Double d : freqAvgs) {
            avgAllFreqs += d;
        }
        mean /= freqAvgs.length;*/


        String binData = "";
        for (int i = 0; i < BITS_PER_PACKET; i++) {
            // which frequency index is the low value?
            int low = lowBucket + (int) ((i * (BINARY_FREQ_INCREMENT * 2)) / BUCKET_SIZE);
            // which frequency index is the high value?
            int high = low + (int) (BINARY_FREQ_INCREMENT / BUCKET_SIZE);
            double l = freqAvgs[low];
            double h = freqAvgs[high];
            // avgFreqDiff / 6
            /*if (l >= median && l >= h) {
                binData += "0";
            } else if (l <= median && h >= l) {
                binData += "1";
            }*/
            if (l - h >= h / 2) {
                binData += "0";
            } else if (h - l >= l / 2) {
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

    private long calculateStartTime(String syncMessage, int showTimeStamp){
        Timber.i("Calculating start time...");
        // go through samples and calculate start time
        Map<Character, Integer> reductionValue = new HashMap<>();
        char[] chars = syncMessage.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            reductionValue.put(c, i * PACKET_DURATION);
        }

        String ss = "";
        for (Sample s : samples) {
            ss += s.getChar();
        }
        Timber.i(ss);
        Timber.i(syncMessage);

        int sampleIndex = samples.size() - 1;
        List<Sample> newSamples = new ArrayList<>();
        for(int i = syncMessage.length() - 1; i >= 0; i--) {
            char c = syncMessage.charAt(i);
            //trace back from the end to start of the sync message to verify where the valid
            //samples started
            //e.g. if the samples are
            // 'asdfjdkdaaaaaaabbbbcccccddddeee'
            //and syncMessage is 'abcde'
            //we need to trace back from the samples to find the cutoff point

            while (sampleIndex >= 0 && samples.get(sampleIndex).getChar() == c) {
                newSamples.add(0, samples.get(sampleIndex));
                sampleIndex--;
            }
        }
        samples = newSamples;

        /*//take out any samples that
        int i = 0;
        while (i < samples.size()) {
            if (!reductionValue.containsKey(samples.get(i).getChar())) {
                Timber.w("Removing char from samples not found in SYNC Message: %s",
                        samples.get(i).getChar());
                samples.remove(i);
            }else{
                i++;
            }
        }*/

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
//        Timber.i("High: " + high);
//        Timber.i("Low: " + low);
//        Timber.i("High - Low: " + (high - low));

        long startTime = low + (((high - PACKET_DURATION) - low) / 2);
        samples.clear();

        Timber.i("Start Time: " + startTime);
        Timber.i("showTimeStamp: " + showTimeStamp);

        Timber.i("How long ago: " + (System.currentTimeMillis() - startTime) / 1000);
        startTime -= showTimeStamp;
        Timber.i("Both: " + startTime);

        Timber.i("How long ago: " + (System.currentTimeMillis() - startTime) / 1000);
        Timber.i("How long ago: " + (System.currentTimeMillis() - startTime));

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
