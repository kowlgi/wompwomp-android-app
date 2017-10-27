package co.wompwomp.sunshine;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import net.danlew.android.joda.JodaTimeAndroid;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class MyApplication extends Application {
    private static MyApplication mInstance;
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics(), new Answers());

        // Detect memory leaks
        LeakCanary.install(this);

        // Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        JodaTimeAndroid.init(this);
        Stetho.initializeWithDefaults(this);

        mInstance = this;
        this.setAppContext(getApplicationContext());
    }

    public static MyApplication getInstance(){
        return mInstance;
    }

    public void setAppContext(Context mAppContext) {
        this.mAppContext = mAppContext;
    }
}
