/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.wompwomp.sunshine.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import co.wompwomp.sunshine.BuildConfig;
import co.wompwomp.sunshine.Installation;
import co.wompwomp.sunshine.PermissionsDialogFragment;
import co.wompwomp.sunshine.R;
import co.wompwomp.sunshine.WompWompConstants;
import co.wompwomp.sunshine.provider.FeedContract;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import timber.log.Timber;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    private Utils() {};

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    public static Uri getLocalVideoUri(String filename, Context context){
        File videofile =  new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), filename);
        if(videofile.exists()) {
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", videofile);
        }

        try {
            if (Build.VERSION.SDK_INT > VERSION_CODES.LOLLIPOP_MR1 &&
                    context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                return null;
            }

            videofile.getParentFile().mkdirs();
            FileInputStream from = context.openFileInput(filename);
            Source src = Okio.source(from);
            BufferedSink dest = Okio.buffer(Okio.sink(videofile));
            dest.writeAll(src);
            dest.close();
            src.close();
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".co.wompwomp.sunshine.provider", videofile);
        } catch (Exception e) {
            Timber.e(e.toString());
            return null;
        }
    }

    public static Uri getLocalViewBitmapUri(String filename, View aView, Context context){
        Uri bmpUri = null;
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        filename += ".jpg";

        File file =  new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), filename);

        if(file.exists()) {
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".co.wompwomp.sunshine.provider", file);
        }

        //Create a Bitmap with the same dimensions
        Bitmap image = Bitmap.createBitmap(aView.getWidth(),
                aView.getHeight(),
                Bitmap.Config.RGB_565);
        Canvas myCanvas = new Canvas(image);

        //Draw the view inside the Bitmap
        aView.draw(myCanvas);

        final double SHRINK_FACTOR = 0.25;
        Bitmap watermark = BitmapFactory.decodeResource(context.getResources(), R.drawable.wompwomp_newicon_watermark);
        double new_width = (int) (aView.getWidth() * SHRINK_FACTOR);
        double new_height = (new_width/watermark.getWidth()) * watermark.getHeight(); /* scale proportionally */
        Bitmap scaledWatermark = Bitmap.createScaledBitmap(watermark,
                (int)new_width,
                (int)new_height,
                false);
        Paint bgPaint=new Paint();
        bgPaint.setAntiAlias(true);
        myCanvas.drawBitmap(scaledWatermark,
                aView.getWidth() - (int) new_width/* left */,
                aView.getHeight() - (int) new_height /* top */,
                bgPaint);

        // Store image to default external storage directory
        FileOutputStream out = null;
        try {
            if (Build.VERSION.SDK_INT > VERSION_CODES.LOLLIPOP_MR1 &&
                    context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") ==
                            PackageManager.PERMISSION_DENIED) {
                return null;
            }

            file.getParentFile().mkdirs();
            out = new FileOutputStream(file);
            image.compress(compressFormat, 90, out); //Output
            bmpUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".co.wompwomp.sunshine.provider", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(out != null) try { out.close(); } catch (Exception ignored) {}
        }
        return bmpUri;
    }

    public static void showShareToast(Context context) {
        Toast toast = Toast.makeText(context,
                context.getResources().getString(R.string.getting_ready_to_share),
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showCannotShareToast(Context context) {
        Toast toast = Toast.makeText(context,
                context.getResources().getString(R.string.cannot_share),
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showAppPageLaunchToast(Context context) {
        Toast toast = Toast.makeText(context,
                context.getResources().getString(R.string.launching_app_page),
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showVideoNotReadyForSharingYetToast(Context context) {
        Toast toast = Toast.makeText(context,
                context.getResources().getString(R.string.video_not_ready_for_sharing_yet),
                Toast.LENGTH_LONG);
        toast.show();
    }

    public static Intent getShareAppIntent(Context context) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.share_app) );
        shareIntent.setType("text/plain");
        return shareIntent;
    }

    public static String truncateAndAppendEllipsis(final String content, final int lastIndex) {
        if(content.length() <= lastIndex) {
            return content;
        }

        String result = content.substring(0, lastIndex);
        if (content.charAt(lastIndex) != ' ') {
            result = result.substring(0, result.lastIndexOf(" "));
        }

        return result + "...";
    }

    // http://stackoverflow.com/questions/18752202/check-if-application-is-installed-android
    public static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void launchPermissionsDialogIfNecessary(Context context) {
        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1 &&
                context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            /*
            Any app that declares the WRITE_EXTERNAL_STORAGE permission is implicitly granted the
            READ_EXTERNAL_STORAGE permission.
            */
            FragmentManager fm = ((AppCompatActivity)context).getSupportFragmentManager();
            PermissionsDialogFragment permissionsDialogFragment = PermissionsDialogFragment.newInstance();
            permissionsDialogFragment.show(fm, "dialog_permissions");
        }
    }

    public static void postToWompwomp(String url, Context context) {
        if(BuildConfig.DEBUG) return;

        OkHttpClient client = new OkHttpClient();
        DateTime dt = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        RequestBody formBody = new FormBody.Builder()
                .add("inst_id", Installation.id(context))
                .add("timestamp", fmt.print(dt))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                response.body().close();
            }
        });
    }

    public static void postToWompwomp(String url, String shareDestination, Context context) {
        if(BuildConfig.DEBUG) return;

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        DateTime dt = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        RequestBody formBody = new FormBody.Builder()
                .add("inst_id", Installation.id(context))
                .add("timestamp", fmt.print(dt))
                .add("destination", shareDestination)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                response.body().close();
            }
        });
    }

    public static ContentValues populateContentValues(Integer card_type, String timestamp, String versionCode) {
        ContentValues contentValues = new ContentValues();
        String entry_id = null;
        if(card_type == WompWompConstants.TYPE_RATE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_RATE;
        } else if(card_type == WompWompConstants.TYPE_SHARE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_SHARE;
        } else if(card_type == WompWompConstants.TYPE_UPGRADE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_UPGRADE;
        }

        contentValues.put(FeedContract.Entry.COLUMN_NAME_ENTRY_ID, entry_id);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT, versionCode);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI, "");
        contentValues.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_CREATED_ON, timestamp);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_CARD_TYPE, card_type);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_AUTHOR, "");
        return contentValues;
    }

    public static Bitmap getBitmap(String imageUri) {
        if (imageUri == null) return null;

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        Bitmap aBitmap = null;
        ResponseBody body = null;
        try {
            final URL url = new URL(imageUri);
            Call call = client.newCall(new Request.Builder().url(url).get().build());
            Response response = call.execute();
            body = response.body();
            aBitmap = BitmapFactory.decodeStream(body.byteStream());
        } catch (final IOException e) {
            Timber.e("Error in downloadBitmap - " + e);
        } finally {
            if (body != null) body.close();
        }

        return aBitmap;
    }

    public static boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        return file != null && file.exists();
    }

    public static boolean validVideoFile(Context context, String videofilename, Integer filesize){
        File internalVideofile = new File(context.getFilesDir(), videofilename);
        return internalVideofile.exists() && internalVideofile.length() == filesize;
    }
}
