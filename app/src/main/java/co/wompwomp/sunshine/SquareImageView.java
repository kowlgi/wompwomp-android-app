package co.wompwomp.sunshine;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SquareImageView extends ImageView {
    FrameLayout.LayoutParams mLayoutParams = new FrameLayout.LayoutParams(0, 0);

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
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int scaledHeight = getDrawable().getIntrinsicHeight() * parentWidth / getDrawable().getIntrinsicWidth();
        mLayoutParams.width  = parentWidth;
        mLayoutParams.height = scaledHeight;
        this.setLayoutParams(mLayoutParams);
        setMeasuredDimension(parentWidth, scaledHeight);
    }
}
