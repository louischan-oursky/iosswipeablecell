package com.github.iawaknahc.iosswipeablecell;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

public class ISCCellView<ContentView extends View> extends ViewGroup implements Animator.AnimatorListener {

    private static final String LOG_TAG = "ISCCellView";

    protected ContentView mContentView;

    protected ISCButtonContainerView mButtonContainerViewRightFirst;
    protected TextView mButtonRightFirst;

    protected AnimatorSet mAnimatorSet;
    protected ObjectAnimator mAnimatorContentView;
    protected ObjectAnimator mAnimatorButtonContainerViewRightFirst;

    protected TimeInterpolator mTimeInterpolator;

    protected boolean mHasTransitioned;
    protected boolean mIsTransitioning;

    protected int mCellHeight;

    public ISCCellView(ContentView contentView, int cellHeight) {
        super(contentView.getContext());

        this.setOnTouchListener(new ISCCellOnTouchListener(
                true,
                false
        ));

        this.mCellHeight = cellHeight;
        this.mTimeInterpolator = new LinearInterpolator();

        this.mButtonRightFirst = new TextView(this.getContext());
        this.mButtonRightFirst.setPadding(10, 10, 10, 10);
        this.mButtonRightFirst.setText("Archive");
        this.mButtonRightFirst.setTextColor(0xFF_FF_FF_FF);
        this.mButtonRightFirst.setBackgroundColor(0xFF_00_00_FF);
        this.mButtonContainerViewRightFirst = new ISCButtonContainerView(
                this.mButtonRightFirst,
                ISCButtonContainerView.AlignLeft
        );
        this.addView(this.mButtonContainerViewRightFirst);

        this.mContentView = contentView;
        this.addView(this.mContentView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = this.mCellHeight;

        this.mContentView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        this.mButtonContainerViewRightFirst.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        this.mContentView.layout(0, 0, width, height);
        this.mButtonContainerViewRightFirst.layout(width, 0, width + width, height);
    }

    public ContentView getContentView() {
        return this.mContentView;
    }

    public View getButtonRightFirst() {
        return this.mButtonRightFirst;
    }

    public void animateStage0Interactively(float contentTranslateX, float buttonTranslateX) {
        this.mContentView.setTranslationX(contentTranslateX);
        this.mButtonContainerViewRightFirst.setTranslationX(buttonTranslateX);
    }

    public void animateStage1Interactively(float translateX, float boundTranslateX) {
        float t2 = this.mButtonContainerViewRightFirst.getTranslationX();

        this.mContentView.setTranslationX(translateX);

        if (t2 == boundTranslateX || mHasTransitioned) {
            mHasTransitioned = false;
            this.mButtonContainerViewRightFirst.setTranslationX(boundTranslateX);
        } else if (!mIsTransitioning) {
            mIsTransitioning = true;
            mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", boundTranslateX);
            mAnimatorButtonContainerViewRightFirst.setDuration(100);
            mAnimatorButtonContainerViewRightFirst.setInterpolator(this.mTimeInterpolator);
            mAnimatorButtonContainerViewRightFirst.addListener(this);
            Log.d(LOG_TAG, "animateStage1Interactively start");
            mAnimatorButtonContainerViewRightFirst.start();
        }
    }

    public void animateStage2Interactively(float translateX) {
        float t1 = this.mContentView.getTranslationX();
        float t2 = this.mButtonContainerViewRightFirst.getTranslationX();

        this.mContentView.setTranslationX(translateX);

        if (t1 == t2 || mHasTransitioned) {
            mHasTransitioned = false;
            mButtonContainerViewRightFirst.setTranslationX(translateX);
        } else if (!mIsTransitioning) {
            mIsTransitioning = true;
            mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", translateX);
            mAnimatorButtonContainerViewRightFirst.setDuration(100);
            mAnimatorButtonContainerViewRightFirst.setInterpolator(this.mTimeInterpolator);
            mAnimatorButtonContainerViewRightFirst.addListener(this);
            Log.d(LOG_TAG, "animateStage2Interactively start");
            mAnimatorButtonContainerViewRightFirst.start();
        }
    }

    public void animateStage1(float boundTranslateX) {
        if (mAnimatorContentView != null) {
            mAnimatorContentView.cancel();
            mAnimatorContentView = null;
        }
        if (mAnimatorButtonContainerViewRightFirst != null) {
            mAnimatorButtonContainerViewRightFirst.cancel();
            mAnimatorButtonContainerViewRightFirst = null;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", boundTranslateX);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", boundTranslateX);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);

        mAnimatorSet.start();
    }

    public void animateStage2(float translateX) {
        if (mAnimatorContentView != null) {
            mAnimatorContentView.cancel();
            mAnimatorContentView = null;
        }
        if (mAnimatorButtonContainerViewRightFirst != null) {
            mAnimatorButtonContainerViewRightFirst.cancel();
            mAnimatorButtonContainerViewRightFirst = null;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", mContentView.getTranslationX(), translateX);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", mButtonContainerViewRightFirst.getTranslationX(), translateX);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);

        mAnimatorSet.start();
    }

    public void animateToIdle() {
        if (mAnimatorContentView != null) {
            mAnimatorContentView.cancel();
            mAnimatorContentView = null;
        }
        if (mAnimatorButtonContainerViewRightFirst != null) {
            mAnimatorButtonContainerViewRightFirst.cancel();
            mAnimatorButtonContainerViewRightFirst = null;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", 0);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", 0);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);

        mAnimatorSet.start();
    }

    @Override
    public void onAnimationStart(Animator animator) {
        Log.d(LOG_TAG, "onAnimationStart ");
        mIsTransitioning = true;
        mHasTransitioned = false;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        Log.d(LOG_TAG, "onAnimationEnd " );
        mHasTransitioned = true;
        mIsTransitioning = false;
        animator.removeListener(this);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        Log.d(LOG_TAG, "onAnimationCancel");
        mHasTransitioned = false;
        mIsTransitioning = false;
        animator.removeListener(this);
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        Log.d(LOG_TAG, "onAnimationRepeat");
    }
}
