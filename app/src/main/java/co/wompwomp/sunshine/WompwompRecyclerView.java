package co.wompwomp.sunshine;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by kowlgi on 1/8/16.
 */
public class WompwompRecyclerView extends RecyclerView {
    public WompwompRecyclerView(Context context) {
        super(context);
    }

    public WompwompRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WompwompRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int getMinFlingVelocity() {
        return 10 * super.getMinFlingVelocity();
    }
}
