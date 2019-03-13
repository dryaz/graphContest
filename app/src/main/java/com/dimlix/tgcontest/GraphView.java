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

    private static final int SET_SIZE = 100;
    private static final int DEFAULT_MAX_VALUE = 10000;

    private int[] mDataY = new int[SET_SIZE];

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();

    private int mMaxGrapValue = DEFAULT_MAX_VALUE;

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = DEFAULT_MAX_VALUE;

    private float mStepXForMaxScale;

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

        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        int firstPointToShow =
                Math.max((int) Math.floor(mLeftCurrentXBoarderValue / mStepXForMaxScale) - 1, 0);
        int lastPointToShow = Math.min((int) Math.floor(mRightCurrentXBoarderValue / mStepXForMaxScale) + 1,
                SET_SIZE - 1);

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

        mLeftCurrentXBoarderValue = (float) xValueLeftRegion / mMaxGrapValue * getWidth();;
        mRightCurrentXBoarderValue = (float) xValueRightRegion / mMaxGrapValue * getWidth();

        invalidate();
    }

    public int getMaxGrapValue() {
        return mMaxGrapValue;
    }

    /**
     * Like in seekbar define the max available progress.
     */
    public void setMaxGrapValue(int mMaxGrapValue) {
        this.mMaxGrapValue = mMaxGrapValue;
    }
}
