package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.dimlix.tgcontest.model.ChartData;

import java.util.HashMap;
import java.util.Map;

public class ChartView extends View {

    private static final int TOGGLE_ANIM_DURATION = 300;

    private Path mPath = new Path();
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    @Nullable
    private String mLineToToggle = null;

    @Nullable
    private ChartData mChartData = null;

    private long mStartToggleTime;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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

        long elapsed = System.currentTimeMillis() - mStartToggleTime;
        float progress = 1;
        if (mLineToToggle != null) {
            progress = (float) elapsed / TOGGLE_ANIM_DURATION;
        }

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

            int alpha = 255;
            if (yData.getVarName().equals(mLineToToggle)) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * progress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - progress)), 0));
                }
            }

            canvas.drawPath(mPath, paint);
        }

        if (progress < 1 && (mLineToToggle != null)) {
            invalidate();
        } else {
            mLastMaxPossibleYever = maxPossibleYever;
            if (mLineToToggle != null) {
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
