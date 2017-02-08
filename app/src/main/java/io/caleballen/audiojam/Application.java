package io.caleballen.audiojam;

import timber.log.Timber;

/**
 * Created by caleb on 2/1/2017.
 */

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
