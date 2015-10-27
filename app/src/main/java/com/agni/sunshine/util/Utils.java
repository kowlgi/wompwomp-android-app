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

package com.agni.sunshine.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    private Utils() {};


    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static void enableStrictMode() {
        if (Utils.hasGingerbread()) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

//            if (Utils.hasHoneycomb()) {
//                threadPolicyBuilder.penaltyFlashScreen();
//                vmPolicyBuilder
//                        .setClassInstanceLimit(ImageGridActivity.class, 1)
//                        .setClassInstanceLimit(ImageDetailActivity.class, 1);
//            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

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

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    public static Uri getLocalViewBitmapUri(View aView, Context context){
        // Example: Extract Bitmap from ImageView drawable
        // final Bitmap bmp  = ((BitmapDrawable) networkImageview.getDrawable()).getBitmap();

        //Create a Bitmap with the same dimensions
        Bitmap image = Bitmap.createBitmap(aView.getWidth(),
                aView.getHeight(),
                Bitmap.Config.RGB_565);
        //Draw the view inside the Bitmap
        aView.draw(new Canvas(image));

        // Store image to default external storage directory
        Uri bmpUri = null;
        try {
            if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1 &&
                    context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                return  null;
            }

            File file =  new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png");
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, out); //Output
            bmpUri = Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmpUri;
    }

    public static boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    public static Object getObjectFromFile (String filePath, Context context) throws Exception {
        FileInputStream fis = context.openFileInput(filePath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        //Make sure you close all streams.
        ois.close();
        return obj;
    }
}
