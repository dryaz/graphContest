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
    private static final int ANIM_DURATION_FOR_100_PERCENT = 10000;
    private static final long DELAY_60_FPS = 16; // 16 ms

    private int[] mDataY = new int[SET_SIZE];

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();

    private float mDesiredXStep;
    private float mDesiredYStep;
    private float mCurrentXStep = 0;
    private float mCurrentYStep = 0;

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

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(0, mDataY[0] * mCurrentYStep);
        int i = 1;
        float xVal = i * mCurrentXStep;
        while (i < SET_SIZE && xVal < getWidth()) {
            xVal = i * mCurrentXStep;
            mPath.lineTo(i * mCurrentXStep, getHeight() - mDataY[i] * mCurrentYStep);
            i++;
        }
        canvas.drawPath(mPath, mPathPaint);
        float elapsedAnim = System.currentTimeMillis() - mStartTime;
        Log.e("!@#", "Elapsed: " + elapsedAnim);
        if (elapsedAnim < mCalcAnimDuration
                && mCurrentXStep != mDesiredXStep
                && mCurrentYStep != mDesiredYStep) {
            float progress = elapsedAnim / mCalcAnimDuration;
            Log.e("!@#", "progress: " + progress);
            mCurrentXStep = mCurrentXStep + (mDesiredXStep - mCurrentXStep) * progress;
            Log.e("!@#", "mCurrentXStep: " + mCurrentXStep);
            Log.e("!@#", "mDesiredXStep: " + mDesiredXStep);
            mCurrentYStep = mCurrentYStep + (mDesiredYStep - mCurrentYStep) * progress;
            postInvalidateDelayed(DELAY_60_FPS);
        } else {
            mCurrentXStep = mDesiredXStep;
            mCurrentYStep = mDesiredYStep;
            if (mCurrentXStep == mDesiredXStep && mCurrentYStep == mDesiredYStep) return;
            invalidate();
        }
    }

    public void setMaxVisibleRegion(int xValueRightRegion) {
        if (getWidth() == 0) {
            postInvalidate();
            return;
        }
        mStartTime = System.currentTimeMillis();
        float mMaxYValueforSelectedRegion = mDataY[0];
        for (int i = 1; i < xValueRightRegion; i++) {
            if (mMaxYValueforSelectedRegion < mDataY[i]) mMaxYValueforSelectedRegion = mDataY[i];
        }

        mDesiredXStep = (float) getWidth() / xValueRightRegion;
        mDesiredYStep = (float) getHeight() / mMaxYValueforSelectedRegion;

        if (mCurrentXStep == 0) {
            mCurrentXStep = mDesiredXStep;
        }
        if (mCurrentYStep == 0) {
            mCurrentYStep = mDesiredYStep;
        }

        Log.e("!@#", "getWidth " + getWidth());
        // Max range of values that we should want to animate
        // From showing only 1 value on graph up to showing all #SET_SIZE points
        // Switching by this distance must be animated in ANIM_DURATION_FOR_100_PERCENT ms
        float maxStepDistance = getWidth() - (float) getWidth() / SET_SIZE;
        // Diff between desired and current steps size to compute animation with
        float mStepDiff = Math.abs(mCurrentXStep - mDesiredXStep);

        // Choose what part of total animation should be applied for current desired tranlaction
        // It is needed to make graph changing speed consistent regardless the desired diff
        mCalcAnimDuration = (int) (mStepDiff / maxStepDistance * ANIM_DURATION_FOR_100_PERCENT);

        Log.e("!@#", "delta " + mCalcAnimDuration);
        Log.e("!@#", "animDurationForPart " + mCalcAnimDuration);
        Log.e("!@#", "mCalcAnim " + mCalcAnimDuration);

        invalidate();
    }
}
