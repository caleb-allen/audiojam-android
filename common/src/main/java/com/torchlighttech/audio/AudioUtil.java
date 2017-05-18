package com.torchlighttech.audio;

import com.torchlighttech.util.Logger;
import com.torchlighttech.util.Sample;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by caleb on 5/12/17.
 */

public class AudioUtil {
    public static final int SAMPLERATE = 44100;
    public static final int BUCKETS = 1024;
    //    private static final String SYNC_MESSAGE = "abcde";
    //    private static final int PACKET_DURATION = 1000; // in milliseconds
    public static final int PACKET_DURATION = 300; // in milliseconds
    public static final int LOW_FREQ = 18100;
    //width of each bucket in terms of frequency (~21.5332 Hz)
    public static final double BUCKET_SIZE = (((float) SAMPLERATE / 2) / (float) BUCKETS);
    //    private static final int LOW_FREQ = 17990;
    //01100001
    /**
     * how far apart is each bit in terms of buckets?
     * if BUCKETS is 512, this value should be 5
     * if BUCKETS is 1024, this value should be 10
     * etc
     */
    public static final int BINARY_BUCKET_DISTANCE = 10;
    public static final int BYTES_PER_PACKET = 1;
    public static final int BITS_PER_PACKET = (8 * BYTES_PER_PACKET) + 1;//plus checking bit
    public static final double BINARY_FREQ_INCREMENT = BUCKET_SIZE * BINARY_BUCKET_DISTANCE;
    public static final int FRAMES_THRESHOLD = 2;

//    private static List<Sample> samples;
    private static Queue<Double[]> bufferFrames = new LinkedBlockingQueue<>();
    public static Double[] averages = new Double[BUCKETS];
    public static long startTime = 0;
    public static List<Sample> samples = new ArrayList<>();



    public static double[] fft(short[] buffer) {
        DoubleFFT_1D fft = new DoubleFFT_1D(BUCKETS);
        double[] data = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            data[i] = (double) buffer[i];
        }
        fft.realForward(data);
        return data;
    }

    public static Double[] buffer(double[] data) {
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
    
    public static String fskDemodulation(Double[] freqAvgs, String prev) {
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
//                            Logger.i(binData);
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
//                                Logger.i(packet);

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
                Logger.i("Rejected, uneven");
            }
        }
        return null;
    }

    public static long calculateStartTime(String syncMessage, int showTimeStamp){
        Logger.i("Calculating start time...");
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
        Logger.i(ss);
        Logger.i(syncMessage);

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

        Logger.i("Reducing samples...");
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
//        Logger.i("High: " + high);
//        Logger.i("Low: " + low);
//        Logger.i("High - Low: " + (high - low));

        long startTime = low + (((high - PACKET_DURATION) - low) / 2);
        samples.clear();

        Logger.i("Start Time: " + startTime);
        Logger.i("showTimeStamp: " + showTimeStamp);

        Logger.i("How long ago: " + (System.currentTimeMillis() - startTime) / 1000);
        startTime -= showTimeStamp;
        Logger.i("Both: " + startTime);

        Logger.i("How long ago: " + (System.currentTimeMillis() - startTime) / 1000);
        Logger.i("How long ago: " + (System.currentTimeMillis() - startTime));

        return startTime;
    }

    public static boolean validateMessage(String next){
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
                    Logger.i("Showtimestamp: " + showTimeStamp);
                    syncMessage = "ab" + showTimeStamp;
                    timeStamp = Integer.parseInt(showTimeStamp, 10) * 1000;
                    Logger.i("String to Integer Timestamp: " + showTimeStamp + " -> "
                            + timeStamp);
                    Logger.i("Valid message: " + syncMessage);
                    validMessage = true;
                } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    validMessage = false;
                }
            }
            if (validMessage) {
                Logger.i(syncMessage);
//                if (!startedSequence) {
                    startTime = calculateStartTime(syncMessage, timeStamp);
                    return true;
//                }
            }
        }
        return false;
    }
}
