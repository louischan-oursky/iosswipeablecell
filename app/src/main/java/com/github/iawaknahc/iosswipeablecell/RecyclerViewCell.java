package com.github.iawaknahc.iosswipeablecell;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

public class RecyclerViewCell extends ViewGroup {

    protected TextView mTextView;

    public RecyclerViewCell(Context context) {
        super(context);
        this.setBackgroundColor(0xFF_FF_FF_FF);

        this.mTextView = new TextView(context);
        this.mTextView.setTextColor(0xFF_00_00_00);
        this.addView(this.mTextView);
    }

    public void bindPosition(int position) {
        this.mTextView.setText(Integer.toString(position));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = 200;
        this.mTextView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
        );
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int childWidth = this.mTextView.getMeasuredWidth();
        int childHeight = this.mTextView.getMeasuredHeight();

        int childLeft = (width - childWidth) / 2;
        int childRight = childLeft + childWidth;

        int childTop = (height - childHeight) / 2;
        int childBottom = childTop + childHeight;

        this.mTextView.layout(childLeft, childTop, childRight, childBottom);
    }
}
