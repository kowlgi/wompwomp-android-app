package co.wompwomp.sunshine;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.FacebookSdk;
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

        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();

        Crashlytics crashlytics = new Crashlytics.Builder()
                .core(crashlyticsCore)
                .build();

        Answers answers = new Answers();

        Fabric fabric;
        if(BuildConfig.DEBUG) {
             fabric = new Fabric.Builder(this)
                    .kits(crashlytics)
                    .build();
        }
        else {
            fabric = new Fabric.Builder(this)
                    .kits(crashlytics, answers)
                    .build();
        }
        // Crash and usage analytics
        Fabric.with(fabric);
        // Detect memory leaks
        LeakCanary.install(this);

        // Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        FacebookSdk.sdkInitialize(getApplicationContext());
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
