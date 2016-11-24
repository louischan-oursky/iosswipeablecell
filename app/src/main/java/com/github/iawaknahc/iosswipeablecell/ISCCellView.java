package com.github.iawaknahc.iosswipeablecell;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

public class ISCCellView<ContentView extends View> extends ViewGroup implements Animator.AnimatorListener, View.OnClickListener {

    public interface ISCCellViewActionDelegate<ContentView extends View> {
        void onWillSwipeFromRightToLeft(ISCCellView<ContentView> cellView);
        void onDidSwipeFromRightToLeft(ISCCellView<ContentView> cellView);
    }

    private static final String LOG_TAG = "ISCCellView";

    // delegate
    protected ISCCellViewActionDelegate<ContentView> mActionDelegate;

    // views
    protected RecyclerView mRecyclerView;
    protected RecyclerView.OnScrollListener mOnScrollListener;
    protected ContentView mContentView;
    protected ISCButtonView mButtonContainerViewRightFirst;
    protected View mButtonRightFirst;
    // views

    // gesture
    protected static final int TouchStatePossible = 0;
    protected static final int TouchStateFailed = 1;
    protected static final int TouchStateBegan = 2;

    // -1 and 1 is chosen to allow it acts as sign conveniently
    protected static final int GestureNone = 0;
    protected static final int GestureSwipeFromRightToLeft = -1;
    protected static final int GestureSwipeFromLeftToRight = 1;

    protected static final int SwipeProcessStage0 = 0;
    protected static final int SwipeProcessStage1 = 1;
    protected static final int SwipeProcessStage2 = 2;

    protected int mTouchState;
    protected int mGesture;
    protected int mSwipeProcess;
    protected boolean mRecognizeSwipeFromRightToLeft;
    protected boolean mRecognizeSwipeFromLeftToRight;

    protected float mInitialTranslateX;
    protected float mDownX;
    protected float mDownY;
    protected float mLastMoveX;
    // gesture

    // animation/transition
    protected static final int TransitionNone = 0;
    protected static final int TransitionInteractiveStage1 = 1;
    protected static final int TransitionInteractiveStage2 = 2;
    protected static final int TransitionSettlingStage0 = 3;
    protected static final int TransitionSettlingStage1 = 4;
    protected static final int TransitionSettlingStage2 = 5;
    protected AnimatorSet mAnimatorSet;
    protected ObjectAnimator mAnimatorContentView;
    protected ObjectAnimator mAnimatorButtonContainerViewRightFirst;
    protected TimeInterpolator mTimeInterpolator;
    protected boolean mHasTransitioned;
    protected int mOngoingTransition;
    // animation/transition

    public ISCCellView(RecyclerView recyclerView, ContentView contentView, View buttonRightFirst) {
        super(contentView.getContext());

        this.mRecyclerView = recyclerView;
        this.mOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // if the list scrolled and we have recognized gesture
                // settleToStage0
                if (ISCCellView.this.mSwipeProcess != SwipeProcessStage0) {
                    ISCCellView.this.settleToStage0();
                }
            }
        };
        this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);

        this.mRecognizeSwipeFromRightToLeft = true;
        this.mRecognizeSwipeFromLeftToRight = false;

        this.mTimeInterpolator = new LinearInterpolator();

        this.mButtonRightFirst = buttonRightFirst;
        this.mButtonContainerViewRightFirst = new ISCButtonView(
                this.mButtonRightFirst,
                ISCButtonView.AlignLeft
        );
        this.mButtonRightFirst.setOnClickListener(this);
        this.addView(this.mButtonContainerViewRightFirst);

        this.mContentView = contentView;
        this.addView(this.mContentView);
    }

    // public apis
    public ContentView getContentView() {
        return this.mContentView;
    }

    public void setActionDelegate(ISCCellViewActionDelegate delegate) {
        this.mActionDelegate = delegate;
    }
    // public apis

    // layout
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mContentView.measure(widthMeasureSpec, heightMeasureSpec);

        int width = this.mContentView.getMeasuredWidth();
        int height = this.mContentView.getMeasuredHeight();

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
    // layout

    // touch state machine
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        this.stepTouchStateStateMachine(event);
        if (mTouchState == TouchStateBegan) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.stepTouchStateStateMachine(event);
        return true;
    }

    protected void stepTouchStateStateMachine(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = motionEvent.getX();
                mDownY = motionEvent.getY();
                if (mSwipeProcess == SwipeProcessStage1) {
                    mInitialTranslateX = this.mContentView.getTranslationX();
                } else {
                    mInitialTranslateX = 0;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                switch (mTouchState) {
                    case TouchStatePossible: {
                        ViewConfiguration viewConfiguration = ViewConfiguration.get(this.getContext());
                        float scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
                        float x = motionEvent.getX();
                        if (x - mDownX > scaledTouchSlop && mRecognizeSwipeFromLeftToRight) {
                            mTouchState = TouchStateBegan;
                            if (mGesture == GestureNone) {
                                mGesture = GestureSwipeFromLeftToRight;
                            }
                            this.requestDisallowInterceptTouchEvent(true);
                            break;
                        } else if (mDownX - x > scaledTouchSlop && mRecognizeSwipeFromRightToLeft) {
                            mTouchState = TouchStateBegan;
                            if (mGesture == GestureNone) {
                                mGesture = GestureSwipeFromRightToLeft;
                            }
                            this.requestDisallowInterceptTouchEvent(true);
                            break;
                        }
                        break;
                    }
                    case TouchStateBegan: {
                        float moveX = motionEvent.getX();
                        float threshold = this.mButtonRightFirst.getWidth();
                        float dx = moveX - mDownX + mInitialTranslateX;
                        float dxSign = dx < 0 ? -1 : 1;
                        float absDx = Math.abs(dx);
                        float boundDx = dxSign * Math.min(threshold, absDx);
                        int swipeProcess = this.getSwipeProcess();
                        if (dx * mGesture >= 0) {
                            if (swipeProcess == SwipeProcessStage2) {
                                this.transitionToStage2Interactively(dx);
                            } else if (swipeProcess == SwipeProcessStage1) {
                                this.transitionToStage1Interactively(dx, boundDx);
                            } else {
                                this.transitionToStage0Interactively(dx, boundDx);
                            }
                            mSwipeProcess = swipeProcess;
                        }
                    }
                    default: {
                        break;
                    }
                }
                mLastMoveX = motionEvent.getX();
                break;
            }
            case MotionEvent.ACTION_UP: {
                switch (mTouchState) {
                    case TouchStatePossible:
                    case TouchStateFailed: {
                        this.settleToStage0();
                        break;
                    }
                    case TouchStateBegan: {
                        float moveX = motionEvent.getX();
                        float threshold = this.mButtonRightFirst.getWidth();
                        float dx = moveX - mDownX;
                        float dxSign = dx < 0 ? -1 : 1;
                        float boundDx = dxSign * threshold;
                        int swipeProcess = this.getSwipeProcess();
                        if (swipeProcess == SwipeProcessStage0) {
                            mGesture = GestureNone;
                            mTouchState = TouchStatePossible;
                            mSwipeProcess = SwipeProcessStage0;
                            this.settleToStage0();
                        } else if (swipeProcess == SwipeProcessStage1) {
                            // remember last gesture
                            mTouchState = TouchStatePossible;
                            mSwipeProcess = SwipeProcessStage1;
                            this.settleToStage1(boundDx);
                        } else if (swipeProcess == SwipeProcessStage2) {
                            float translateX = this.getFullTranslateX();
                            mGesture = GestureNone;
                            mTouchState = TouchStatePossible;
                            mSwipeProcess = SwipeProcessStage0;
                            this.settleToStage2(translateX);
                        }
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mGesture = GestureNone;
                mTouchState = TouchStatePossible;
                mSwipeProcess = SwipeProcessStage0;
                this.settleToStage0();
                break;
            }
            default: {
                break;
            }
        }
    }

    protected int getSwipeProcess() {
        float contentWidth = this.getWidth();
        float buttonWidth = this.mButtonRightFirst.getWidth();
        float translateX = this.mContentView.getTranslationX();
        float absTranslateX = Math.abs(translateX);
        if (absTranslateX >= contentWidth * 0.45) {
            return SwipeProcessStage2;
        } else if (absTranslateX >= buttonWidth) {
            return SwipeProcessStage1;
        }
        return SwipeProcessStage0;
    }

    protected float getFullTranslateX() {
        float translateX = this.getWidth() * mGesture;
        return translateX;
    }
    // touch state machine

    // transition
    protected void transitionToStage0Interactively(float contentTranslateX, float buttonTranslateX) {
        this.mContentView.setTranslationX(contentTranslateX);
        this.mButtonContainerViewRightFirst.setTranslationX(buttonTranslateX);
    }

    protected void transitionToStage1Interactively(float translateX, float boundTranslateX) {
        float t2 = this.mButtonContainerViewRightFirst.getTranslationX();

        this.mContentView.setTranslationX(translateX);

        if (t2 == boundTranslateX || mHasTransitioned) {
            mHasTransitioned = false;
            this.mButtonContainerViewRightFirst.setTranslationX(boundTranslateX);
        } else if (mOngoingTransition != TransitionInteractiveStage1) {
            this.cancelOngoingTransition();
            this.notifyUserForRecognizedSwipeByVibration();
            mHasTransitioned = false;
            mOngoingTransition = TransitionInteractiveStage1;
            mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", boundTranslateX);
            mAnimatorButtonContainerViewRightFirst.setDuration(100);
            mAnimatorButtonContainerViewRightFirst.setInterpolator(this.mTimeInterpolator);
            mAnimatorButtonContainerViewRightFirst.addListener(this);
            mAnimatorButtonContainerViewRightFirst.start();
        }
    }

    protected void transitionToStage2Interactively(float translateX) {
        float t1 = this.mContentView.getTranslationX();
        float t2 = this.mButtonContainerViewRightFirst.getTranslationX();

        this.mContentView.setTranslationX(translateX);

        if (t1 == t2 || mHasTransitioned) {
            mHasTransitioned = false;
            mButtonContainerViewRightFirst.setTranslationX(translateX);
        } else if (mOngoingTransition != TransitionInteractiveStage2) {
            this.cancelOngoingTransition();
            this.notifyUserForRecognizedSwipeByVibration();
            mHasTransitioned = false;
            mOngoingTransition = TransitionInteractiveStage2;
            mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", translateX);
            mAnimatorButtonContainerViewRightFirst.setDuration(100);
            mAnimatorButtonContainerViewRightFirst.setInterpolator(this.mTimeInterpolator);
            mAnimatorButtonContainerViewRightFirst.addListener(this);
            mAnimatorButtonContainerViewRightFirst.start();
        }
    }

    protected void settleToStage0() {
        this.cancelOngoingTransition();

        mOngoingTransition = TransitionSettlingStage0;

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", 0);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", 0);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);
        mAnimatorSet.addListener(this);
        mAnimatorSet.start();
    }

    protected void settleToStage1(float boundTranslateX) {
        this.cancelOngoingTransition();

        mOngoingTransition = TransitionSettlingStage1;

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", boundTranslateX);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", boundTranslateX);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);
        mAnimatorSet.addListener(this);
        mAnimatorSet.start();
    }

    protected void settleToStage2(float translateX) {
        // FIXME: handle direction
        if (mActionDelegate != null) {
            mActionDelegate.onWillSwipeFromRightToLeft(this);
        }

        this.cancelOngoingTransition();

        mOngoingTransition = TransitionSettlingStage2;

        mAnimatorSet = new AnimatorSet();

        mAnimatorContentView = ObjectAnimator.ofFloat(mContentView, "translationX", mContentView.getTranslationX(), translateX);
        mAnimatorButtonContainerViewRightFirst = ObjectAnimator.ofFloat(mButtonContainerViewRightFirst, "translationX", mButtonContainerViewRightFirst.getTranslationX(), translateX);

        mAnimatorSet.play(mAnimatorContentView).with(mAnimatorButtonContainerViewRightFirst);
        mAnimatorSet.setDuration(100);
        mAnimatorSet.setInterpolator(this.mTimeInterpolator);
        mAnimatorSet.addListener(this);
        mAnimatorSet.start();
    }

    protected void cancelOngoingTransition() {
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
    }
    // transition

    // vibration
    protected void notifyUserForRecognizedSwipeByVibration() {
        try {
            Context context = this.getContext();
            int result = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                return;
            }
            vibrator.vibrate(50);
        } catch (Exception e) {
            // ignore vibration
        }
    }

    // vibration

    // implements AnimatorListener
    @Override
    public void onAnimationStart(Animator animator) {

    }

    @Override
    public void onAnimationEnd(Animator animator) {
        mHasTransitioned = true;
        if (mOngoingTransition == TransitionSettlingStage0 || mOngoingTransition == TransitionSettlingStage2) {
            mGesture = GestureNone;
            mTouchState = TouchStatePossible;
            mSwipeProcess = SwipeProcessStage0;
            if (mOngoingTransition == TransitionSettlingStage2) {
                if (mActionDelegate != null) {
                    mActionDelegate.onDidSwipeFromRightToLeft(this);
                }
                this.mContentView.setTranslationX(0);
                this.mButtonContainerViewRightFirst.setTranslationX(0);
            }
        }
        mOngoingTransition = TransitionNone;
        animator.removeListener(this);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        mOngoingTransition = TransitionNone;
        animator.removeListener(this);
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        // we do not have any repeat animation
    }
    // implements AnimatorListener

    // implements OnClickListener
    @Override
    public void onClick(View view) {
        float translateX = this.getFullTranslateX();
        this.settleToStage2(translateX);
    }
    // implements OnClickListener
}
