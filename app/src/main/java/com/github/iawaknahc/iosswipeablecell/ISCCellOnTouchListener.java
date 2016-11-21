package com.github.iawaknahc.iosswipeablecell;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class ISCCellOnTouchListener implements View.OnTouchListener {

    private static final String LOG_TAG = "ISCCellOnTouchListener";

    protected static final int StatePossible = 0;
    protected static final int StateFailed = 1;
    protected static final int StateChanged = 2;

    protected static final int GestureNone = 0;
    protected static final int GestureSwipeFromRightToLeft = 1;
    protected static final int GestureSwipeFromLeftToRight = 2;

    protected static final int SwipeProcessStage0 = 0;
    protected static final int SwipeProcessStage1 = 1;
    protected static final int SwipeProcessStage2 = 2;

    protected int mState;
    protected int mGesture;
    protected int mSwipeProcess;
    protected boolean mRecognizeSwipeFromRightToLeft;
    protected boolean mRecognizeSwipeFromLeftToRight;

    protected float mDownX;
    protected float mDownY;
    protected float mLastMoveX;

    public ISCCellOnTouchListener(boolean recognizeSwipeFromRightToLeft, boolean recognizeSwipeFromLeftToRight) {
        this.mRecognizeSwipeFromRightToLeft = recognizeSwipeFromRightToLeft;
        this.mRecognizeSwipeFromLeftToRight = recognizeSwipeFromLeftToRight;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        ISCCellView<?> cellView = this.castView(view);
        int action = motionEvent.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = motionEvent.getX();
                mDownY = motionEvent.getY();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                switch (mState) {
                    case StatePossible: {
                        ViewConfiguration viewConfiguration = this.getViewConfiguration(view);
                        float scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
                        float x = motionEvent.getX();
                        if (x - mDownX > scaledTouchSlop && mRecognizeSwipeFromLeftToRight) {
                            mState = StateChanged;
                            if (mGesture == GestureNone) {
                                mGesture = GestureSwipeFromLeftToRight;
                            }
                            cellView.requestDisallowInterceptTouchEvent(true);
                            return true;
                        } else if (mDownX - x > scaledTouchSlop && mRecognizeSwipeFromRightToLeft) {
                            mState = StateChanged;
                            if (mGesture == GestureNone) {
                                mGesture = GestureSwipeFromRightToLeft;
                            }
                            cellView.requestDisallowInterceptTouchEvent(true);
                            return true;
                        }
                        break;
                    }
                    case StateChanged: {
                        float moveX = motionEvent.getX();
                        float threshold = cellView.getButtonRightFirst().getWidth();
                        float dx = moveX - mDownX;
                        float dxSign = dx < 0 ? -1 : 1;
                        float absDx = Math.abs(dx);
                        float boundDx = dxSign * Math.min(threshold, absDx);
                        int swipeProcess = this.getSwipeProcess(cellView, motionEvent);
                        if ((dx <= 0 && mGesture == GestureSwipeFromRightToLeft) || (dx >= 0 && mGesture == GestureSwipeFromLeftToRight)) {
                            if (swipeProcess == SwipeProcessStage2) {
                                cellView.animateStage2Interactively(dx);
                            } else if (swipeProcess == SwipeProcessStage1) {
                                cellView.animateStage1Interactively(dx, boundDx);
                            } else {
                                cellView.animateStage0Interactively(dx, boundDx);
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
                switch (mState) {
                    case StatePossible:
                    case StateFailed: {
                        cellView.animateToIdle();
                        break;
                    }
                    case StateChanged: {
                        float moveX = motionEvent.getX();
                        float threshold = cellView.getButtonRightFirst().getWidth();
                        float dx = moveX - mDownX;
                        float dxSign = dx < 0 ? -1 : 1;
                        float absDx = Math.abs(dx);
                        float boundDx = dxSign * Math.min(threshold, absDx);
                        int swipeProcess = getSwipeProcess(cellView, motionEvent);
                        if (swipeProcess == SwipeProcessStage0) {
                            mGesture = GestureNone;
                            mState = StatePossible;
                            mSwipeProcess = SwipeProcessStage0;
                            cellView.animateToIdle();
                        } else if (swipeProcess == SwipeProcessStage1) {
                            // remember last gesture
                            mState = StatePossible;
                            mSwipeProcess = SwipeProcessStage1;
                            cellView.animateStage1(boundDx);
                        } else if (swipeProcess == SwipeProcessStage2) {
                            float translateY = cellView.getWidth();
                            if (mGesture == GestureSwipeFromRightToLeft) {
                                translateY *= -1;
                            }
                            mGesture = GestureNone;
                            mState = StatePossible;
                            mSwipeProcess = SwipeProcessStage0;
                            cellView.animateStage2(translateY);
                        }
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mGesture = GestureNone;
                mState = StatePossible;
                mSwipeProcess = SwipeProcessStage0;
                cellView.animateToIdle();
                break;
            }
            default: {
                break;
            }
        }

        return false;
    }

    protected int getSwipeProcess(ISCCellView<?> cellView, MotionEvent motionEvent) {
        float contentWidth = cellView.getWidth();
        float buttonWidth = cellView.getButtonRightFirst().getWidth();
        float dx = motionEvent.getX() - mDownX;
        float absDx = Math.abs(dx);

        if (absDx >= contentWidth * 0.65) {
            return SwipeProcessStage2;
        } else if (absDx >= buttonWidth) {
            return SwipeProcessStage1;
        }

        return SwipeProcessStage0;
    }

    protected ViewConfiguration getViewConfiguration(View view) {
        return ViewConfiguration.get(view.getContext());
    }

    protected ISCCellView<?> castView(View view) {
        return (ISCCellView<?>) view;
    }
}
