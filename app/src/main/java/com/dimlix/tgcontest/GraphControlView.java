package com.dimlix.tgcontest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class GraphControlView extends View {

    private static final int SET_SIZE = 100;

    private int[] mDataY = new int[SET_SIZE];

    private Path mPath = new Path();
    private Paint mPathPaint = new Paint();
    private float mStepX;

    public GraphControlView(Context context) {
        super(context);
        init();
    }

    public GraphControlView(Context context, @Nullable AttributeSet attrs) {
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

        if (mStepX == 0) {
            mStepX = (float) getWidth() / (SET_SIZE - 1);
        }

        float maxPossibleYever = 0;
        for (int i = 0; i < SET_SIZE; i++) {
            if (maxPossibleYever < mDataY[i]) maxPossibleYever = mDataY[i];
        }

        float yStep = getHeight() / maxPossibleYever;
        mPath.moveTo(0, mDataY[0] * yStep);
        for (int i = 1; i < SET_SIZE; i++) {
            mPath.lineTo(i * mStepX, mDataY[i] * yStep);
        }

        canvas.drawPath(mPath, mPathPaint);
    }
}
