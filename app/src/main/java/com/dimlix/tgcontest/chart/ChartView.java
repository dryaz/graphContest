package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to display chart data.
 * <p>
 * Some part is the same with {@link ChartControlView} but code is kept separate because
 * looks like it should be more flexible rather then common,
 * e.g. chart line toggling animation is different.
 */
public class ChartView extends View {

    private static final int TOGGLE_ANIM_DURATION = 300;
    // When user change view region chart y axis is animated with this duration.
    private static final int SHIFT_ANIM_DURATION = 100;
    private static final int NUM_HOR_AXIS = 6;

    private Path mPath = new Path();
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    @Nullable
    private String mLineToToggle = null;

    @Nullable
    private ChartData mChartData = null;

    private long mStartToggleTime = -1;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever;

    private Paint mAxisPaint;
    private Paint mAxisTextPaint;
    private int mAxisWidth;
    private int mAxisTextSize;

    private float mTouchXValue = -1;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mAxisWidth = getContext().getResources().getDimensionPixelSize(R.dimen.axis_width);
        mAxisTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.axis_text_size);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.GRAY);
        mAxisPaint.setAlpha(50);
        mAxisPaint.setStyle(Paint.Style.FILL);
        mAxisPaint.setStrokeWidth(mAxisWidth);

        mAxisTextPaint = new Paint();
        mAxisTextPaint.setColor(Color.GRAY);
        mAxisTextPaint.setTextSize(mAxisTextSize);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_MOVE) {
                    mTouchXValue = event.getX();
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mTouchXValue = -1;
                }
                invalidate();
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mChartData == null) {
            // TODO draw empty state if needed
            return;
        }

        if (mStepXForMaxScale == 0) {
            // Chart range is not set yet.
            return;
        }

        // Draw chart lines
        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        int firstPointToShow =
                Math.max((int) Math.floor(mLeftCurrentXBoarderValue / mStepXForMaxScale) - 1, 0);
        int lastPointToShow = Math.min((int) Math.floor(mRightCurrentXBoarderValue / mStepXForMaxScale) + 1,
                mChartData.getSize() - 1);

        long maxPossibleYever = 0;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            ChartData.YData yData = mChartData.getYValues().get(k);
            if (!yData.isShown()) continue;
            // Extra iteration over visible fragment needed to find out y scale factor.
            for (int i = firstPointToShow; i < lastPointToShow; i++) {
                Long nextValue = yData.getValues().get(i);
                if (maxPossibleYever < nextValue) maxPossibleYever = nextValue;
            }
        }

        if (maxPossibleYever == 0) {
            // Prevent single line from flying up
            maxPossibleYever = mLastMaxPossibleYever;
        }

        float progress = 1;
        if (mLineToToggle != null || maxPossibleYever != mLastMaxPossibleYever) {
            if (mStartToggleTime == -1) {
                mStartToggleTime = System.currentTimeMillis();
            }
            long elapsed = System.currentTimeMillis() - mStartToggleTime;
            if (mLineToToggle != null) {
                progress = (float) elapsed / TOGGLE_ANIM_DURATION;
            } else {
                progress = (float) elapsed / SHIFT_ANIM_DURATION;
            }
        }

        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            mPath.reset();
            ChartData.YData yData = mChartData.getYValues().get(k);
            float masPossibleYeverComputed =
                    mLastMaxPossibleYever + (maxPossibleYever - mLastMaxPossibleYever) * progress;

            if (!yData.getVarName().equals(mLineToToggle)) {
                if (!yData.isShown()) {
                    continue;
                }
            }

            float yStep = (float) getHeight() / masPossibleYeverComputed;
            mPath.moveTo((firstPointToShow * mStepXForMaxScale - translation) * scale,
                    getHeight() - yData.getValues().get(0) * yStep);
            for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                mPath.lineTo((i * mStepXForMaxScale - translation) * scale,
                        getHeight() - yData.getValues().get(i) * yStep);
            }

            Paint paint = mPaints.get(yData.getVarName());
            if (paint == null) {
                throw new RuntimeException("There is no color info for " + yData.getVarName());
            }

            if (yData.getVarName().equals(mLineToToggle)) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * progress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - progress)), 0));
                }
            }

            canvas.drawPath(mPath, paint);
        }

        // Draw chart axis
        int yAxisStep = Math.round((float) maxPossibleYever / NUM_HOR_AXIS);
        int yDistance = getHeight() / (NUM_HOR_AXIS);
        for (int i = 0; i < NUM_HOR_AXIS; i++) {
            canvas.drawLine(0, getHeight() - mAxisWidth - yDistance * i,
                    getWidth(), getHeight() - mAxisWidth - yDistance * i, mAxisPaint);
            canvas.drawText(String.valueOf(yAxisStep * i), 0,
                    getHeight() - mAxisWidth - yDistance * i - mAxisTextSize / 2, mAxisTextPaint);
        }

        // Draw info about touched section
        if (mTouchXValue > 0) {
            int nearestIndexTouched = Math.round((mTouchXValue / scale + translation) / mStepXForMaxScale);
            float xValToDraw = (nearestIndexTouched * mStepXForMaxScale - translation) * scale;
            canvas.drawLine(xValToDraw, 0, xValToDraw, getHeight(), mAxisPaint);
        }


        // Run draw loop in case animation is running.
        if (progress < 1) {
            invalidate();
        } else {
            mStartToggleTime = -1;
            if (mLineToToggle != null || mLastMaxPossibleYever != maxPossibleYever) {
                mLastMaxPossibleYever = maxPossibleYever;
                mLineToToggle = null;
                invalidate();
            }
        }

    }

    /**
     * We assume that whole seekbar graph is descrete from 0 to {@link ChartLayout#MAX_DISCRETE_PROGRESS}.
     * User can't stop it's scroll on e.g. 0.5 point, from 0 graph will jump to 1 directly.
     * Like seekbar - when dev. set max to 2, there is 3 point {0,1,2} and user can't stop at any
     * other value.
     *
     * @param xValueRightRegion Value from [0..{@link ChartLayout#MAX_DISCRETE_PROGRESS}] -
     *                          right border of selected region
     * @param xValueLeftRegion  Value from [0..{@link ChartLayout#MAX_DISCRETE_PROGRESS}] -
     *                          left border of selected region
     */
    public void setMaxVisibleRegionPercent(final int xValueLeftRegion, final int xValueRightRegion) {
        if (mChartData == null) {
            throw new IllegalStateException("Set data first");
        }

        if (getWidth() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    setMaxVisibleRegionPercent(xValueLeftRegion, xValueRightRegion);
                }
            });
            return;
        }

        mStepXForMaxScale = (float) getWidth() / (mChartData.getSize() - 1);
        mLeftCurrentXBoarderValue = (float) xValueLeftRegion / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
        mRightCurrentXBoarderValue = (float) xValueRightRegion / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();

        invalidate();
    }

    public void setChartData(@NonNull ChartData data) {
        mChartData = data;
        mPaints.clear();
        for (ChartData.YData yData : mChartData.getYValues()) {
            Paint paint = new Paint();

            paint.setColor(Color.parseColor(yData.getColor()));
            paint.setStyle(Paint.Style.STROKE);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeWidth(4);

            mPaints.put(yData.getVarName(), paint);
        }
        invalidate();
    }

    /**
     * Show/hide chart line on chart
     *
     * @param yVarName name of affected line
     */
    void onYChartToggled(String yVarName) {
        mStartToggleTime = System.currentTimeMillis();
        mLineToToggle = yVarName;
        invalidate();
    }
}
