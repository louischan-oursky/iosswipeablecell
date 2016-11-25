package com.github.iawaknahc.iosswipeablecell;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

public class ISCTranslateView extends ViewGroup implements Animator.AnimatorListener {

    private static final String LOG_TAG = "ISCTranslateView";

    protected float mPixelDensity = 1f;

    protected static final int TransitionNone = 0;
    protected static final int TransitionInteractiveStage1 = 1;
    protected static final int TransitionInteractiveStage2 = 2;
    protected static final int TransitionSettlingStage0 = 3;
    protected static final int TransitionSettlingStage1 = 4;
    protected static final int TransitionSettlingStage2 = 5;

    protected static final int SwipeProcessStage0 = 0;
    protected static final int SwipeProcessStage1 = 1;
    protected static final int SwipeProcessStage2 = 2;

    protected int mIntrinsicWidth;

    protected AnimatorSet mSettlingAnimator;
    protected TimeInterpolator mTimeInterpolator;
    protected int mOngoingSettlingTransition;

    protected int mOngoingInteractiveTransition;
    protected long mOngoingInteractiveTransitionStartedAt;
    protected float mOngoingInteractiveTranslateXStart;

    protected final int mGesture;
    protected int mLastInteractiveSwipeProcess;

    private boolean mNotifyParentOnSwipe;

    private ArrayList<Float> mChildTranslateX;

    public ISCTranslateView(Context context, int gesture) {
        super(context);
        this.mPixelDensity = context.getResources().getDisplayMetrics().density;
        this.mTimeInterpolator = new LinearInterpolator();
        this.mOngoingSettlingTransition = TransitionNone;
        this.mOngoingInteractiveTransition = TransitionNone;
        this.mGesture = gesture;
        this.mLastInteractiveSwipeProcess = SwipeProcessStage0;
        this.mChildTranslateX = new ArrayList<>();
        this.setWillNotDraw(false);
    }

    public void setViews(View.OnClickListener listener, ArrayList<ISCButtonView> views) {
        for (int i = 0; i < this.getChildCount(); ++i) {
            View child = this.getChildAt(i);
            child.setOnClickListener(null);
        }

        this.removeAllViews();

        if (views != null) {
            // we reverse the order so that the first view is drawn last
            for (int i = views.size() - 1; i >= 0; --i) {
                ISCButtonView child = views.get(i);
                child.setOnClickListener(listener);
                this.addView(views.get(i));
            }
        }
    }

    public void settleByCurrentSwipeProcess() {
        int swipeProcess = this.getStaticSwipeProcess();
        if (swipeProcess == SwipeProcessStage2) {
            this.settleToStage2(true);
        } else if (swipeProcess == SwipeProcessStage1) {
            this.settleToStage1();
        } else {
            this.settleToStage0();
        }
    }

    public void settleToStage0() {
        if (this.getTranslationX() != 0 && mOngoingSettlingTransition != TransitionSettlingStage0) {
            this.cancelSettlingTransition();
            mSettlingAnimator = new AnimatorSet();
            ObjectAnimator thisAnimator = ObjectAnimator.ofFloat(this, "translationX", 0);
            AnimatorSet.Builder builder = mSettlingAnimator.play(thisAnimator);
            ObjectAnimator contentViewAnimator = ObjectAnimator.ofFloat(this.getContentView(), "translationX", 0);
            builder.with(contentViewAnimator);
            for (int i = 0; i < this.getChildCount(); ++i) {
                ISCButtonView view = (ISCButtonView) this.getChildAt(i);
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "translationX", 0);
                builder.with(objectAnimator);
            }
            mSettlingAnimator.setDuration(100);
            mSettlingAnimator.setInterpolator(this.mTimeInterpolator);
            mSettlingAnimator.addListener(this);
            mOngoingSettlingTransition = TransitionSettlingStage0;
            mSettlingAnimator.start();
        }
    }

    public void settleToStage1() {
        if (mOngoingSettlingTransition != TransitionSettlingStage1) {
            this.cancelSettlingTransition();
            mSettlingAnimator = new AnimatorSet();
            ObjectAnimator thisAnimator = ObjectAnimator.ofFloat(this, "translationX", -this.getIntrinsicWidth()); // FIXME
            AnimatorSet.Builder builder = mSettlingAnimator.play(thisAnimator);
            ObjectAnimator contentViewAnimator = ObjectAnimator.ofFloat(this.getContentView(), "translationX", -this.getIntrinsicWidth()); // FIXME
            builder.with(contentViewAnimator);
            int n = this.getChildCount();
            if (n == 1) {
                View button = this.getChildAt(0);
                builder.with(ObjectAnimator.ofFloat(button, "translationX", 0));
            } else if (n > 1) {
                for (int i = 0; i < n; ++i) {
                    View view = this.getChildAt(i);
                    builder.with(ObjectAnimator.ofFloat(view, "translationX", -i * (-this.getIntrinsicWidth()) / n)); // FIXME
                }
            }
            mSettlingAnimator.setDuration(100);
            mSettlingAnimator.setInterpolator(this.mTimeInterpolator);
            mSettlingAnimator.addListener(this);
            mOngoingSettlingTransition = TransitionSettlingStage1;
            mSettlingAnimator.start();
        }
    }

    public void settleToStage2(boolean notifyParent) {
        if (mOngoingSettlingTransition != TransitionSettlingStage2) {
            mNotifyParentOnSwipe = notifyParent;
            this.cancelSettlingTransition();
            mSettlingAnimator = new AnimatorSet();
            ObjectAnimator thisAnimator = ObjectAnimator.ofFloat(this, "translationX", -this.getWidth()); // FIXME
            AnimatorSet.Builder builder = mSettlingAnimator.play(thisAnimator);
            ObjectAnimator contentViewAnimator = ObjectAnimator.ofFloat(this.getContentView(), "translationX", -this.getWidth()); // FIXME
            builder.with(contentViewAnimator);
            mSettlingAnimator.setDuration(100);
            mSettlingAnimator.setInterpolator(this.mTimeInterpolator);
            mSettlingAnimator.addListener(this);
            mOngoingSettlingTransition = TransitionSettlingStage2;
            mSettlingAnimator.start();
        }
    }

    public void translateInteractively(float translationX) {
        // special case: opposite direction
        // apply friction
        if (translationX * mGesture < 0) {
            float reducedTx = translationX / (2 * mPixelDensity);
            this.getContentView().setTranslationX(reducedTx);
            this.setTranslationX(reducedTx);
            mLastInteractiveSwipeProcess = SwipeProcessStage0;
            return;
        }

        int n = this.getChildCount();
        int swipeProcess = this.computeSwipeProcess(translationX);

        // special case: no children
        if (n <= 0) {
            this.getContentView().setTranslationX(translationX);
            this.setTranslationX(translationX);
            mLastInteractiveSwipeProcess = SwipeProcessStage0;
            return;
        }

        // compute generic translateX
        mChildTranslateX.clear();
        for (int i = 0; i < n; ++i) {
            mChildTranslateX.add(-i * translationX / n);
        }
        // compute translateX for primary button when there is only one button
        if (n == 1) {
            float absTx = Math.abs(translationX);
            ISCButtonView button = (ISCButtonView) this.getChildAt(0);
            float buttonIntrinsicWidth = button.getIntrinsicWidth();
            float diff = absTx - buttonIntrinsicWidth;
            float childTranslateX = 0;
            if (diff > 0) {
                float sign = translationX < 0 ? 1 : -1;
                float compensation = sign * diff * (1 - 1 / (2 * mPixelDensity));
                childTranslateX = compensation;
            }
            mChildTranslateX.set(0, childTranslateX);
        }
        if (swipeProcess == SwipeProcessStage2) {
            mChildTranslateX.set(this.getChildCount() - 1, 0f);
        }

        View primaryButton = this.getChildAt(this.getChildCount() - 1);
        if (swipeProcess == SwipeProcessStage1 && mLastInteractiveSwipeProcess == SwipeProcessStage2) {
            mOngoingInteractiveTransition = TransitionInteractiveStage1;
            mOngoingInteractiveTransitionStartedAt = System.currentTimeMillis();
            mOngoingInteractiveTranslateXStart = primaryButton.getTranslationX();
            invalidate();
        } else if (swipeProcess == SwipeProcessStage2 && mLastInteractiveSwipeProcess == SwipeProcessStage1) {
            mOngoingInteractiveTransition = TransitionInteractiveStage2;
            mOngoingInteractiveTransitionStartedAt = System.currentTimeMillis();
            mOngoingInteractiveTranslateXStart = primaryButton.getTranslationX();
            invalidate();
        } else if (mOngoingInteractiveTransition == TransitionNone) {
            for (int i = 0; i < n; ++i) {
                View view = this.getChildAt(i);
                view.setTranslationX(mChildTranslateX.get(i));
            }
        }

        this.getContentView().setTranslationX(translationX);
        this.setTranslationX(translationX);
        mLastInteractiveSwipeProcess = swipeProcess;
    }

    protected int getStaticSwipeProcess() {
        return this.computeSwipeProcess(this.getTranslationX());
    }

    protected int computeSwipeProcess(float translationX) {
        float width = this.getWidth();
        float intrinsicWidth = this.getIntrinsicWidth();
        if (this.getChildCount() == 1) {
            intrinsicWidth *= 2;
        }
        float absTx = Math.abs(translationX);
        if (translationX * mGesture >= 0) {
            if (absTx > width * 0.8 || absTx > intrinsicWidth * 1.5) {
                return SwipeProcessStage2;
            } else if (absTx >= intrinsicWidth / 2) {
                return SwipeProcessStage1;
            }
        }
        return SwipeProcessStage0;
    }

    protected int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    protected void cancelSettlingTransition() {
        if (mSettlingAnimator != null) {
            mOngoingSettlingTransition = TransitionNone;
            mSettlingAnimator.removeAllListeners();
            mSettlingAnimator.cancel();
            mSettlingAnimator = null;
        }
    }

    protected ISCCellView getCellView() {
        return (ISCCellView) this.getParent();
    }

    protected View getContentView() {
        return this.getCellView().getContentView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        for (int i = 0; i < this.getChildCount(); ++i) {
            View child = this.getChildAt(i);
            child.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            );
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        for (int i = 0; i < this.getChildCount(); ++i) {
            View child = this.getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            child.layout(0, 0, childWidth, childHeight);
        }

        // materialize intrinsicWidth
        int intrinsicWidth = 0;
        for (int i = 0; i < this.getChildCount(); ++i) {
            ISCButtonView view = (ISCButtonView) this.getChildAt(i);
            intrinsicWidth += view.getIntrinsicWidth();
        }
        mIntrinsicWidth = intrinsicWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mOngoingInteractiveTransition != TransitionNone) {
            float n = this.getChildCount();
            View primaryButton = this.getChildAt(this.getChildCount() - 1);
            float duration = 100;

            long currentTimeMillis = System.currentTimeMillis();
            float timeDiff = currentTimeMillis - mOngoingInteractiveTransitionStartedAt;

            float endValue = 0f;
            if (mOngoingInteractiveTransition == TransitionInteractiveStage1) {
                endValue = -(n - 1) * this.getTranslationX() / n;
                if (n == 1) {
                    float absTx = Math.abs(this.getTranslationX());
                    ISCButtonView button = (ISCButtonView) this.getChildAt(0);
                    float buttonIntrinsicWidth = button.getIntrinsicWidth();
                    float diff = absTx - buttonIntrinsicWidth;
                    if (diff > 0) {
                        float sign = this.getTranslationX() < 0 ? 1 : -1;
                        float compensation = sign * diff * (1 - 1 / (2 * mPixelDensity));
                        endValue = compensation;
                    }
                }
            } else if (mOngoingInteractiveTransition == TransitionInteractiveStage2) {
                endValue = 0f;
            }

            float targetValue = mOngoingInteractiveTranslateXStart + (endValue - mOngoingInteractiveTranslateXStart) / duration * timeDiff;
            if (timeDiff > duration) {
                targetValue = endValue;
                mOngoingInteractiveTransition = TransitionNone;
            } else {
                this.invalidate();
            }
            primaryButton.setTranslationX(targetValue);
        }
        super.onDraw(canvas);
    }

    @Override
    public void onAnimationStart(Animator animator) {

    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (animator == mSettlingAnimator) {
            if (mOngoingSettlingTransition == TransitionSettlingStage2) {
                for (int i = 0; i < this.getChildCount(); ++i) {
                    View child = this.getChildAt(i);
                    child.setTranslationX(0);
                }
                this.getContentView().setTranslationX(0);
                if (mNotifyParentOnSwipe) {
                    mNotifyParentOnSwipe = false;
                    this.getCellView().onDidSwipeFromRightToLeft(); // FIXME
                }
            }
            mOngoingSettlingTransition = TransitionNone;
        }
        animator.removeListener(this);
    }

    @Override
    public void onAnimationCancel(Animator animator) {

    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        // we do not repeat animation
    }
}
