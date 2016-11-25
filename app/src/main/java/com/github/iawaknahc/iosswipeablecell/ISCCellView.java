package com.github.iawaknahc.iosswipeablecell;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ISCCellView<ContentView extends View> extends ViewGroup implements View.OnClickListener {

    public interface ISCCellViewActionDelegate<ContentView extends View> {
        void onDidSwipeFromRightToLeft(ISCCellView<ContentView> cellView);
    }

    private static final String LOG_TAG = "ISCCellView";

    // delegate
    protected ISCCellViewActionDelegate<ContentView> mActionDelegate;
    // delegate

    // views
    protected RecyclerView mRecyclerView;
    protected RecyclerView.OnScrollListener mOnScrollListener;
    protected ContentView mContentView;
    protected ISCTranslateView mLeftTranslateView;
    protected ISCTranslateView mRightTranslateView;
    // views

    // gesture
    protected static final int TouchStatePossible = 0;
    protected static final int TouchStateFailed = 1;
    protected static final int TouchStateBegan = 2;
    // -1 and 1 is chosen to allow it acts as sign conveniently
    protected static final int GestureSwipeFromRightToLeft = -1;
    protected static final int GestureSwipeFromLeftToRight = 1;

    protected int mTouchState;

    protected float mInitialTranslateX;
    protected float mDownX;
    protected float mDownY;
    // gesture

    public ISCCellView(RecyclerView recyclerView, ContentView contentView) {
        super(contentView.getContext());

        this.mRecyclerView = recyclerView;
        this.mOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // if the list scrolled and we have recognized gesture
                // settleToStage0
                ISCCellView.this.settleToStage0();
            }
        };
        this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);

        this.mContentView = contentView;
        this.mLeftTranslateView = new ISCTranslateView(this.getContext(), GestureSwipeFromLeftToRight);
        this.mRightTranslateView = new ISCTranslateView(this.getContext(), GestureSwipeFromRightToLeft);

        this.addView(this.mLeftTranslateView);
        this.addView(this.mRightTranslateView);
        this.addView(this.mContentView);
    }

    // public apis
    public ContentView getContentView() {
        return this.mContentView;
    }

    public void setActionDelegate(ISCCellViewActionDelegate delegate) {
        this.mActionDelegate = delegate;
    }

    public void setLeftButtons(ArrayList<View> leftButtons) {
        ArrayList<ISCButtonView> buttonViews = new ArrayList<>();
        if (leftButtons != null) {
            for (int i = 0; i < leftButtons.size(); ++i) {
                View view = leftButtons.get(i);
                ISCButtonView buttonView = new ISCButtonView(view, ISCButtonView.AlignRight, i);
                buttonViews.add(buttonView);
            }
        }
        this.mLeftTranslateView.setViews(this, buttonViews);
    }

    public void setRightButtons(ArrayList<View> rightButtons) {
        ArrayList<ISCButtonView> buttonViews = new ArrayList<>();
        if (rightButtons != null) {
            for (int i = 0; i < rightButtons.size(); ++i) {
                View view = rightButtons.get(i);
                ISCButtonView buttonView = new ISCButtonView(view, ISCButtonView.AlignLeft, i);
                buttonViews.add(buttonView);
            }
        }
        this.mRightTranslateView.setViews(this, buttonViews);
    }
    // public apis

    // layout
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mContentView.measure(widthMeasureSpec, heightMeasureSpec);
        int width = this.mContentView.getMeasuredWidth();
        int height = this.mContentView.getMeasuredHeight();
        this.mLeftTranslateView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        this.mRightTranslateView.measure(
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
        this.mLeftTranslateView.layout(-width, 0, 0, height);
        this.mRightTranslateView.layout(width, 0, width + width, height);
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
                mInitialTranslateX = this.mContentView.getTranslationX();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                switch (mTouchState) {
                    case TouchStatePossible: {
                        ViewConfiguration viewConfiguration = ViewConfiguration.get(this.getContext());
                        float scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
                        float x = motionEvent.getX();
                        float diffX = x - mDownX;
                        boolean isPan = Math.abs(diffX) > scaledTouchSlop;
                        if (isPan) {
                            mTouchState = TouchStateBegan;
                            this.requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                    }
                    case TouchStateBegan: {
                        float moveX = motionEvent.getX();
                        float dx = moveX - mDownX + mInitialTranslateX;
                        this.mRightTranslateView.translateInteractively(dx);
                        break;
                    }
                    default: {
                        break;
                    }
                }
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
                        mTouchState = TouchStatePossible;
                        this.settleByCurrentSwipeProcess();
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mTouchState = TouchStatePossible;
                this.settleToStage0();
                break;
            }
            default: {
                break;
            }
        }
    }
    // touch state machine

    // transition
    protected void settleByCurrentSwipeProcess() {
        this.mRightTranslateView.settleByCurrentSwipeProcess();
    }

    protected void settleToStage0() {
        this.mRightTranslateView.settleToStage0();
    }

    protected void settleToStage2() {
        this.mRightTranslateView.settleToStage2();
    }
    // transition

    void onDidSwipeFromRightToLeft() {
        if (mActionDelegate != null) {
            mActionDelegate.onDidSwipeFromRightToLeft(this);
        }
    }

    // implements OnClickListener
    @Override
    public void onClick(View view) {
        this.settleToStage2();
    }
    // implements OnClickListener
}
