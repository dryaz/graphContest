package com.dimlix.tgcontest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphView extends View {

    private static final int SET_SIZE = 10;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int ANIMATION_DURATION = 500;

    private int[] mDataY = new int[SET_SIZE];

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();

    private int mMaxGrapValue = DEFAULT_MAX_VALUE;

    private float mLeftDesiredXBoarderValue = 0;
    private float mRightDesiredXBoarderValue = DEFAULT_MAX_VALUE;

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = DEFAULT_MAX_VALUE;

    private float mLeftLastSetXBoarderValue = 0;
    private float mRightLastSetXBoarderValue = DEFAULT_MAX_VALUE;

    private float mStepXForMaxScale;


    private long mStartTime = 0;

    private int mCalcAnimDuration = 0;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        for (int i = 0; i < SET_SIZE; i++) {
            mDataY[i] = (int) (Math.random() * 200);
        }
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeWidth(2);
    }

    private float lastProgress = 0;
    private float lastRightValue = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();

        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        int firstPointToShow =
                Math.max((int) Math.floor(mLeftCurrentXBoarderValue / mStepXForMaxScale), 0);
        int lastPointToShow = Math.min((int) Math.floor(mRightCurrentXBoarderValue / mStepXForMaxScale) + 1,
                SET_SIZE - 1);

        Log.e("!@#", "firstPointToShow " + firstPointToShow);
        Log.e("!@#", "lastPointToShow " + lastPointToShow);

        float maxPossibleYever = 0;
        for (int i = 0; i < SET_SIZE; i++) {
            if (maxPossibleYever < mDataY[i]) maxPossibleYever = mDataY[i];
        }

        float yStep = getHeight() / maxPossibleYever;
        mPath.moveTo((firstPointToShow * mStepXForMaxScale - translation) * scale,
                mDataY[0] * yStep);
        for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
            mPath.lineTo((i * mStepXForMaxScale - translation) * scale,
                    mDataY[i] * yStep);
        }

        canvas.drawPath(mPath, mPathPaint);
        float elapsedAnim = System.currentTimeMillis() - mStartTime;
        if (elapsedAnim < mCalcAnimDuration) {
            float progress = elapsedAnim / mCalcAnimDuration;
            mLeftCurrentXBoarderValue = mLeftLastSetXBoarderValue + (mLeftDesiredXBoarderValue - mLeftLastSetXBoarderValue) * progress;
            mRightCurrentXBoarderValue = mRightLastSetXBoarderValue + (mRightDesiredXBoarderValue - mRightLastSetXBoarderValue) * progress;
            Log.e("!@#1", "progress: " + progress);
            Log.e("!@#1", "progress DIFF: " + (progress - lastProgress));
            lastProgress = progress;
            invalidate();
        } else {
            if (mLeftCurrentXBoarderValue == mLeftDesiredXBoarderValue
                    && mRightCurrentXBoarderValue == mRightDesiredXBoarderValue) {
                mLeftLastSetXBoarderValue = mLeftDesiredXBoarderValue;
                mRightLastSetXBoarderValue = mRightDesiredXBoarderValue;
                return;
            } else {
                mLeftCurrentXBoarderValue = mLeftDesiredXBoarderValue;
                mRightCurrentXBoarderValue = mRightDesiredXBoarderValue;
                invalidate();
            }
        }
        Log.e("!@#", "mLeftCurrentXBoarderValue: " + mLeftCurrentXBoarderValue);
        Log.e("!@#1", "mRightCurrentXBoarderValue: " + mRightCurrentXBoarderValue);
        Log.e("!@#1", "mRightCurrentXBoarderValue diff: " + (mRightCurrentXBoarderValue - lastRightValue));
        lastRightValue = mRightCurrentXBoarderValue;
    }

    /**
     * We assume that whole seekbar graph is descrete from 0 to {@link #mMaxGrapValue}.
     * User can't stop it's scroll on e.g. 0.5 point, from 0 graph will jump to 1 directly.
     * Like seekbar - when dev. set max to 2, there is 3 point {0,1,2} and user can't stop at any
     * other value.
     *
     * @param xValueRightRegion Value from [0..{@link #mMaxGrapValue}] - right border of selected region
     * @param xValueLeftRegion  Value from [0..{@link #mMaxGrapValue}] - left border of selected region
     */
    public void setMaxVisibleRegionPercent(int xValueLeftRegion, int xValueRightRegion) {
        if (getWidth() == 0) {
            postInvalidate();
            return;
        }

        mStepXForMaxScale = (float) getWidth() / (SET_SIZE - 1);
        mStartTime = System.currentTimeMillis();

        mLeftDesiredXBoarderValue = xValueLeftRegion;
        mRightDesiredXBoarderValue = (float) xValueRightRegion / mMaxGrapValue * getWidth();

        if (mLeftCurrentXBoarderValue == 0) {
            mLeftCurrentXBoarderValue = mLeftDesiredXBoarderValue;
        }

        if (mRightCurrentXBoarderValue == 0) {
            mRightCurrentXBoarderValue = mRightDesiredXBoarderValue;
        }

        mLeftLastSetXBoarderValue = mLeftCurrentXBoarderValue;
        mRightLastSetXBoarderValue = mRightCurrentXBoarderValue;

        // TODO compute how long animation should be to keep consistent speed
        float currentWidth = Math.abs(mLeftCurrentXBoarderValue - mRightCurrentXBoarderValue);
        float desiredWidth = Math.abs(mLeftDesiredXBoarderValue - mRightDesiredXBoarderValue);
        float diffWidth = Math.abs(currentWidth - desiredWidth);
        if (diffWidth > 3 * getWidth() / mMaxGrapValue) {
            mCalcAnimDuration = (int) (diffWidth * ANIMATION_DURATION / getWidth());
        } else {
            mCalcAnimDuration = 0;
        }
        Log.e("!@#", "diff widht: " + diffWidth);
        Log.e("!@#", "mCalcAnimDuration " + mCalcAnimDuration);

        invalidate();
    }

    public int getmMaxGrapValue() {
        return mMaxGrapValue;
    }

    /**
     * Like in seekbar define the max available progress.
     */
    public void setmMaxGrapValue(int mMaxGrapValue) {
        this.mMaxGrapValue = mMaxGrapValue;
    }
}
