package co.wompwomp.sunshine;

import android.content.Context;

import com.loopj.android.http.RequestParams;

/**
 * Created by kowlgi on 1/5/16.
 */
public class WompWompHTTPParams extends RequestParams {
    public WompWompHTTPParams(Context context) {
        super();
        super.put("inst_id", Installation.id(context));
    }
}
