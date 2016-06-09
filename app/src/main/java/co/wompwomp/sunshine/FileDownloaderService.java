package co.wompwomp.sunshine;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.webkit.URLUtil;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import co.wompwomp.sunshine.util.Utils;
import co.wompwomp.sunshine.util.VideoFileInfo;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * helper methods.
 */
public class FileDownloaderService extends IntentService {
    private static final String ACTION_DOWNLOAD_FILES = "co.wompwomp.sunshine.action.download_files";
    private static final String FILE_LIST = "file_list";
    private static HashSet<String> downloadsInProgress = null;

    public FileDownloaderService() {
        super("FileDownloaderService");
    }

    public static void startDownload(Context context, ArrayList<VideoFileInfo> fileUrisAndSizes) {
        Intent intent = new Intent(context, FileDownloaderService.class);
        intent.setAction(ACTION_DOWNLOAD_FILES);

        Bundle bundle = new Bundle();
        bundle.putSerializable(FILE_LIST, fileUrisAndSizes);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD_FILES.equals(action)) {
                final Bundle bundle = intent.getExtras();
                ArrayList<VideoFileInfo> fileUrisAndSizes = (ArrayList<VideoFileInfo>) bundle.getSerializable(FILE_LIST);
                downloadFiles(fileUrisAndSizes);
            }
        }
    }

    private void downloadFiles(ArrayList<VideoFileInfo> fileUrisAndSizes) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        for(int i = 0; i < fileUrisAndSizes.size(); i++){
            String videouri = fileUrisAndSizes.get(i).getVideouri();
            int filesize = fileUrisAndSizes.get(i).getFilesize();
            final String filename = URLUtil.guessFileName(videouri, null, null);
            if(Utils.validVideoFile(this, filename, filesize)) {
                // the file already exists, so skip this file download
                // and continue to the next file in the list
                continue;
            }

            if(downloadsInProgress == null) {
                downloadsInProgress = new HashSet<>();
            }

            if(downloadsInProgress.contains(filename)) {
                continue;
            }

            try {
                URL url = new URL(videouri);
                Call newCall = client.newCall(new Request.Builder().url(url).get().build());
                newCall.enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        downloadsInProgress.remove(filename);
                    }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        FileOutputStream output = openFileOutput(filename, Context.MODE_PRIVATE);
                        BufferedSink sink = Okio.buffer(Okio.sink(output));
                        sink.writeAll(response.body().source());
                        sink.close();
                        response.body().close();
                        downloadsInProgress.remove(filename);
                    }
                });

                downloadsInProgress.add(filename);

            } catch (Exception e) {
                downloadsInProgress.remove(filename);
            }
        }
    }
}
