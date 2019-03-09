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

    private static final int MAX = 100;
    private static final int ANIM_DURATION = 400;
    private static final long DELAY_60_FPS = 16; // 16 ms
    private static final float SHIFT_FOR_ANIM_DELAY = (float) DELAY_60_FPS / ANIM_DURATION;

    private int[] mDataY = new int[MAX];

    private int mVisibleRegionXMax = MAX;

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();

    private float mDelafyFor60FPS = (float) 1000 / 60;

    private float mDesiredXStep;
    private float mDesiredYStep;
    private float mCurrentXStep = 0;
    private float mCurrentYStep = 0;
    private float mMaxYValueforSelectedRegion = 0;

    private long mStartTime = 0;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        for (int i = 0; i < MAX; i++) {
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
        while (i < MAX && xVal < getWidth()) {
            xVal = i * mCurrentXStep;
            mPath.lineTo(i * mCurrentXStep, getHeight() - mDataY[i] * mCurrentYStep);
            i++;
        }
        canvas.drawPath(mPath, mPathPaint);
        float elapsedAnim = System.currentTimeMillis() - mStartTime;
        if (elapsedAnim < ANIM_DURATION) {
            float progress = elapsedAnim / ANIM_DURATION;
            mCurrentXStep = mCurrentXStep + (mDesiredXStep - mCurrentXStep) * progress;
            mCurrentYStep = mCurrentYStep + (mDesiredYStep - mCurrentYStep) * progress;
            Log.e("!@#", "com.dimlix.tgcontest.GraphView.onDraw:70 " + progress);
            invalidate();
        } else {
            mCurrentXStep = mDesiredXStep;
            mCurrentYStep = mDesiredYStep;
            invalidate();
            if (mCurrentXStep == mDesiredXStep && mCurrentYStep == mDesiredYStep) return;
        }
    }

    public void setMaxVisibleRegion(int xMax) {
        mStartTime = System.currentTimeMillis();
        for (int i = 0; i < mVisibleRegionXMax; i++) {
            if (mMaxYValueforSelectedRegion < mDataY[i]) mMaxYValueforSelectedRegion = mDataY[i];
        }

        mDesiredXStep = (float) getWidth() / mVisibleRegionXMax;
        mDesiredYStep = (float) getHeight() / mMaxYValueforSelectedRegion;

        if (mCurrentXStep == 0) {
            mCurrentXStep = mDesiredXStep;
        }
        if (mCurrentYStep == 0) {
            mCurrentYStep = mDesiredYStep;
        }

        mVisibleRegionXMax = xMax;
        invalidate();
    }
}
