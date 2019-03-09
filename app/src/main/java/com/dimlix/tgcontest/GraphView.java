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

    private static final int MAX = 10000;

    private int[] mDataY = new int[MAX];

    private int mVisibleRegionXMax = MAX;

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();

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
        int maxYValue = 0;
        for (int i = 0; i < mVisibleRegionXMax; i++) {
            if (maxYValue < mDataY[i]) maxYValue = mDataY[i];
        }

        float xStep = (float) getWidth() / mVisibleRegionXMax;
        float yStep = (float) getHeight() / maxYValue;

        mPath.reset();
        mPath.moveTo(0, mDataY[0] * yStep);
        int i = 1;
        float xVal = i * xStep;
        while (i < MAX && xVal < getWidth()) {
            xVal = i * xStep;
            float yVal = getHeight() - mDataY[i] * yStep;
            mPath.lineTo(i * xStep, getHeight() - mDataY[i] * yStep);
            i++;
        }
        canvas.drawPath(mPath, mPathPaint);
    }

    public void setMaxVisibleRegion(int xMax) {
        mVisibleRegionXMax = xMax;
        invalidate();
    }
}
