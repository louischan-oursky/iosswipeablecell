package com.github.iawaknahc.iosswipeablecell;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public class ISCButtonContainerView extends ViewGroup {

    public static final int AlignLeft = 1;
    public static final int AlignRight = 2;

    protected int mAlign;
    protected View mButtonView;

    public ISCButtonContainerView(@NonNull View buttonView, int align) {
        super(buttonView.getContext());
        this.mAlign = AlignLeft;
        this.mButtonView = buttonView;
        this.addView(this.mButtonView);
        this.setAlign(align);
        inheritBackgroundColor();
    }

    protected void setAlign(int align) {
        if (align == AlignLeft || align == AlignRight) {
            this.mAlign = align;
        }
    }

    protected void inheritBackgroundColor() {

        Drawable buttonBackground = this.mButtonView.getBackground();
        if (buttonBackground instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) buttonBackground;
            // FIXME
//            this.setBackgroundColor(colorDrawable.getColor());
            this.setBackgroundColor(0xFF_FF_00_00);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        this.mButtonView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
        );
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int childWidth = this.mButtonView.getMeasuredWidth();
        int childHeight = this.mButtonView.getMeasuredHeight();

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
        this.mButtonView.layout(childLeft, childTop, childRight, childBottom);
    }
}
