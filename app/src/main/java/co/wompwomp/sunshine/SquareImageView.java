package co.wompwomp.sunshine;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by kowlgi on 10/12/15.
 */
//public class SquareImageView extends NetworkImageView{
public class SquareImageView extends ImageView {

    // Without this parameterized constructor, the compiler will
    // automatically create a default constructor that invokes super().
    // But, NetworkImageView doesn't have a default constructor, which
    // results in a compiler error.
    public SquareImageView(Context context) {
        super(context, null);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }
}
