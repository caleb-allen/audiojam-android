package io.caleballen.audiojam;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.things.pio.Gpio;

import java.io.IOException;

import timber.log.Timber;

public class PiActivity extends Activity {
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 100;

    private Handler mHandler = new Handler();
    private boolean mLedState = false;
    BoardManager boardManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Starting BlinkActivity");

        boardManager = new BoardManager();

        mHandler.post(mBlinkRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending blink Runnable from the handler.
        mHandler.removeCallbacks(mBlinkRunnable);
        // Close the Gpio pin.
        boardManager.disconnectAll();
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            // Toggle the GPIO state
            mLedState = !mLedState;
//            Timber.d("State set to " + mLedState);
            boardManager.enableAllPins(mLedState);
            // Reschedule the same runnable in {#INTERVAL_BETWEEN_BLINKS_MS} milliseconds
            mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);

        }
    };
}