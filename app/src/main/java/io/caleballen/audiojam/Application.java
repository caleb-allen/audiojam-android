package io.caleballen.audiojam;

import timber.log.Timber;

/**
 * Created by caleb on 2/1/2017.
 */

public class Application extends android.app.Application {

    static Application instance;
    public static final boolean DEBUG = true;
    public static Application getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Timber.plant(new Timber.DebugTree());
    }
}
