package co.wompwomp.sunshine;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

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
        Fabric.with(fabric);
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
