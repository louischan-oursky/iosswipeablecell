package com.github.iawaknahc.iosswipeablecell;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public class ISCButtonView extends ViewGroup {

    public static final int AlignLeft = 1;
    public static final int AlignRight = 2;

    protected int mAlign;
    protected View mView;

    public ISCButtonView(@NonNull View buttonView, int align) {
        super(buttonView.getContext());
        this.mView = buttonView;

        this.addView(this.mView);
        this.setAlign(align);
        this.inheritBackgroundColor();
    }

    protected void setAlign(int align) {
        if (align == AlignLeft || align == AlignRight) {
            this.mAlign = align;
        } else {
            this.mAlign = AlignLeft;
        }
    }

    protected void inheritBackgroundColor() {
        Drawable buttonBackground = this.mView.getBackground();
        if (buttonBackground instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) buttonBackground;
            this.setBackgroundColor(colorDrawable.getColor());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        this.mView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int childWidth = this.mView.getMeasuredWidth();
        int childHeight = this.mView.getMeasuredHeight();

        int childTop = (height - childHeight) / 2;
        int childBottom = childTop + childHeight;

        int childLeft = 0;
        int childRight = 0;
        if (this.mAlign == AlignLeft) {
            childLeft = 0;
            childRight = childWidth;
        } else if (this.mAlign == AlignRight) {
            childRight = width;
            childLeft = width - childWidth;
        }
        this.mView.layout(childLeft, childTop, childRight, childBottom);
    }
}
