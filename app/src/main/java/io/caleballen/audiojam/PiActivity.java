package io.caleballen.audiojam;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;

import com.torchlighttech.data.Show;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class PiActivity extends Activity {
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 100;

//    private Handler mHandler = new Handler();
//    private boolean mLedState = false;
    BoardManager boardManager;
    ShowRunner showRunner;
    MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Starting BlinkActivity");
        boardManager = new BoardManager();
        ApiClient.getInstance().getShow(new Callback<Show>() {
            @Override
            public void success(final Show show, Response response) {
                mediaPlayer = MediaPlayer.create(PiActivity.this, R.raw.uprising);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        long startTime = System.currentTimeMillis();
                        mediaPlayer.start();
                        showRunner = new ShowRunner(show, boardManager, startTime);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Timber.e(error);
            }
        });



//        mHandler.post(mBlinkRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending blink Runnable from the handler.
//        mHandler.removeCallbacks(mBlinkRunnable);
        // Close the Gpio pin.
        boardManager.disconnectAll();
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}