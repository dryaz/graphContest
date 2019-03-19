package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
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
    private static final int NUM_X_AXIS_MIN = 3;
    private static final int NUM_X_AXIS_MAX = 5;
    public static final int OFFSET_DRAW_NUM = 1;
    public static final int OFFSET_X_AXIS_DRAW_NUM = 8;
    private static final int DISTANCE_THRESHOLD = 5;

    private Path mPath = new Path();
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    private String mLineToToggle = null;

    private ChartData mChartData = null;

    private long mStartToggleTime = -1;
    private long mStartXAxisAnimTime = -1;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever;
    private int mLastXValuesStep = -1;
    private int mPrevLastXValuesStep = -1;
    private int mPrevNextIndexToDraw = -1;
    private boolean mHideAnimation = false;

    private Paint mAxisPaint;
    private Paint mAxisTextPaint;
    private Paint mTouchedCirclePaint;

    private int mAxisWidth;
    private int mAxisTextSize;
    private int mAxisSelectedCircleSize;

    private int mAxisXHeight;
    private int mPrevDistance;

    private float mTouchXValue = -1;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mAxisWidth = getContext().getResources().getDimensionPixelSize(R.dimen.axis_width);
        mAxisXHeight = getContext().getResources().getDimensionPixelSize(R.dimen.axis_x_height);
        mAxisTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.axis_text_size);
        mAxisSelectedCircleSize = getContext().getResources().getDimensionPixelSize(R.dimen.axis_selected_circle);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.GRAY);
        mAxisPaint.setAlpha(50);
        mAxisPaint.setStyle(Paint.Style.FILL);
        mAxisPaint.setStrokeWidth(mAxisWidth);

        mAxisTextPaint = new Paint();
        mAxisTextPaint.setColor(Color.GRAY);
        mAxisTextPaint.setTextSize(mAxisTextSize);

        mTouchedCirclePaint = new Paint();
        mTouchedCirclePaint.setStyle(Paint.Style.FILL);
        mTouchedCirclePaint.setColor(Color.WHITE);

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
        mAxisTextPaint.setAlpha(255);
        // Draw chart lines
        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        int firstPointToShow =
                Math.max((int) Math.floor(mLeftCurrentXBoarderValue / mStepXForMaxScale) - OFFSET_DRAW_NUM, 0);
        int lastPointToShow = Math.min((int) Math.floor(mRightCurrentXBoarderValue / mStepXForMaxScale) + OFFSET_DRAW_NUM,
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

        float lineToggleProgress = 1;
        if (mLineToToggle != null || maxPossibleYever != mLastMaxPossibleYever) {
            if (mStartToggleTime == -1) {
                mStartToggleTime = System.currentTimeMillis();
            }
            long elapsed = System.currentTimeMillis() - mStartToggleTime;
            if (mLineToToggle != null) {
                lineToggleProgress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
            } else {
                lineToggleProgress = Math.min((float) elapsed / SHIFT_ANIM_DURATION, 1);
            }
        }

        float masPossibleYeverComputed =
                mLastMaxPossibleYever + (maxPossibleYever - mLastMaxPossibleYever) * lineToggleProgress;
        float yStep = (float) getHeightWithoutXAxis() / masPossibleYeverComputed;

        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            mPath.reset();
            ChartData.YData yData = mChartData.getYValues().get(k);


            if (!yData.getVarName().equals(mLineToToggle)) {
                if (!yData.isShown()) {
                    continue;
                }
            }

            mPath.moveTo((firstPointToShow * mStepXForMaxScale - translation) * scale,
                    getHeightWithoutXAxis() - yData.getValues().get(0) * yStep);
            for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                mPath.lineTo((i * mStepXForMaxScale - translation) * scale,
                        getHeightWithoutXAxis() - yData.getValues().get(i) * yStep);
            }

            Paint paint = mPaints.get(yData.getVarName());
            if (paint == null) {
                throw new RuntimeException("There is no color info for " + yData.getVarName());
            }

            if (yData.getVarName().equals(mLineToToggle)) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * lineToggleProgress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - lineToggleProgress)), 0));
                }
            }

            canvas.drawPath(mPath, paint);
        }

        // Draw chart Y axis
        int yAxisStep = Math.round((float) maxPossibleYever / NUM_HOR_AXIS);
        int yDistance = getHeightWithoutXAxis() / (NUM_HOR_AXIS);
        for (int i = 0; i < NUM_HOR_AXIS; i++) {
            canvas.drawLine(0, getHeightWithoutXAxis() - mAxisWidth - yDistance * i,
                    getWidth(), getHeightWithoutXAxis() - mAxisWidth - yDistance * i, mAxisPaint);
            canvas.drawText(String.valueOf(yAxisStep * i), 0,
                    getHeightWithoutXAxis() - mAxisWidth - yDistance * i - mAxisTextSize / 2, mAxisTextPaint);
        }

        //Draw chart X axis
        int firstPointToShowForAxis = Math.max(firstPointToShow - OFFSET_X_AXIS_DRAW_NUM, 0);
        int lastPointToShowForAxis = Math.min(lastPointToShow + OFFSET_X_AXIS_DRAW_NUM, mChartData.getSize() - 1);
        int distance = lastPointToShow - firstPointToShow;
        if (mLastXValuesStep == -1) {
            mLastXValuesStep = Integer.highestOneBit(distance / NUM_X_AXIS_MAX - 1) * 2;
            mPrevLastXValuesStep = mLastXValuesStep;
            mPrevDistance = distance;
        } else if (distance / mLastXValuesStep < NUM_X_AXIS_MIN
                && Math.abs(distance - mPrevDistance) > DISTANCE_THRESHOLD) {
            mPrevLastXValuesStep = mLastXValuesStep;
            mLastXValuesStep /= 2;
            mStartXAxisAnimTime = System.currentTimeMillis();
            mHideAnimation = false;
        } else if (distance / mLastXValuesStep > NUM_X_AXIS_MAX
                && Math.abs(distance - mPrevDistance) > DISTANCE_THRESHOLD) {
            mPrevLastXValuesStep = mLastXValuesStep;
            mLastXValuesStep *= 2;
            mStartXAxisAnimTime = System.currentTimeMillis();
            mHideAnimation = true;
        }
        int nextIndexToDrawXAxisValue = 0;
        int nextIndexToDrawXAxisValueToAnimate = 0;
        int animatedStep = Math.max(mPrevLastXValuesStep, mLastXValuesStep);
        if (firstPointToShowForAxis != 0) {
            nextIndexToDrawXAxisValue = (mLastXValuesStep - firstPointToShowForAxis % mLastXValuesStep) + firstPointToShowForAxis;
            if (mStartXAxisAnimTime == -1) {
                mPrevNextIndexToDraw = nextIndexToDrawXAxisValue;
            } else {
                if (!mHideAnimation) {
                    // When we show new values we must be sure that opacity values still the same
                    nextIndexToDrawXAxisValue = mPrevNextIndexToDraw;
                }
                nextIndexToDrawXAxisValueToAnimate = mPrevNextIndexToDraw;
            }
        }

        float xAxisValuesProgress = 1;

        if (mStartXAxisAnimTime != -1) {
            if (Math.abs(nextIndexToDrawXAxisValueToAnimate - nextIndexToDrawXAxisValue) == animatedStep
                    || nextIndexToDrawXAxisValueToAnimate == nextIndexToDrawXAxisValue) {
                nextIndexToDrawXAxisValueToAnimate -= animatedStep / 2;
            }
            if (nextIndexToDrawXAxisValueToAnimate < 0) {
                nextIndexToDrawXAxisValueToAnimate += animatedStep;
            }

            long elapsed = System.currentTimeMillis() - mStartXAxisAnimTime;
            xAxisValuesProgress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
            if (mHideAnimation) {
                int alpha = Math.max((int) (255 * (1 - xAxisValuesProgress)), 0);
                mAxisTextPaint.setAlpha(alpha);
            } else {
                int alpha = Math.min(((int) (255 * xAxisValuesProgress)), 255);
                mAxisTextPaint.setAlpha(alpha);

            }
            while (nextIndexToDrawXAxisValueToAnimate < lastPointToShowForAxis) {
                String text = mChartData.getXStringValues().get(nextIndexToDrawXAxisValueToAnimate);
                canvas.drawText(text,
                        (nextIndexToDrawXAxisValueToAnimate * mStepXForMaxScale - translation) * scale,
                        getHeight() - (float) mAxisTextSize / 2, mAxisTextPaint);
                nextIndexToDrawXAxisValueToAnimate += animatedStep;
            }
        }

        while (nextIndexToDrawXAxisValue < lastPointToShowForAxis) {
            mAxisTextPaint.setAlpha(255);
            String text = mChartData.getXStringValues().get(nextIndexToDrawXAxisValue);
            canvas.drawText(text,
                    (nextIndexToDrawXAxisValue * mStepXForMaxScale - translation) * scale,
                    getHeight() - (float) mAxisTextSize / 2, mAxisTextPaint);
            if (xAxisValuesProgress < 1) {
                nextIndexToDrawXAxisValue += animatedStep;
            } else {
                nextIndexToDrawXAxisValue += mLastXValuesStep;
            }
        }
        // Draw info about touched section
        if (mTouchXValue > 0) {
            int nearestIndexTouched = Math.round((mTouchXValue / scale + translation) / mStepXForMaxScale);
            float xValToDraw = (nearestIndexTouched * mStepXForMaxScale - translation) * scale;
            canvas.drawLine(xValToDraw, 0, xValToDraw, getHeightWithoutXAxis(), mAxisPaint);
            for (int k = 0; k < mChartData.getYValues().size(); k++) {
                ChartData.YData yData = mChartData.getYValues().get(k);
                if (!yData.isShown()) continue;
                Paint paint = mPaints.get(yData.getVarName());
                if (paint == null) {
                    throw new RuntimeException("There is no color info for " + yData.getVarName());
                }
                canvas.drawCircle(xValToDraw,
                        getHeightWithoutXAxis() - yData.getValues().get(nearestIndexTouched) * yStep,
                        mAxisSelectedCircleSize, mTouchedCirclePaint);
                canvas.drawCircle(xValToDraw,
                        getHeightWithoutXAxis() - yData.getValues().get(nearestIndexTouched) * yStep,
                        mAxisSelectedCircleSize, paint);
            }
        }


        // Run draw loop in case animation is running.
        if (lineToggleProgress < 1 || xAxisValuesProgress < 1) {
            invalidate();
        } else {
            mStartXAxisAnimTime = -1;
            mStartToggleTime = -1;
            mPrevLastXValuesStep = mLastXValuesStep;
            if (mLineToToggle != null || mLastMaxPossibleYever != maxPossibleYever) {
                mLastMaxPossibleYever = maxPossibleYever;
                mLineToToggle = null;
                invalidate();
            }
        }

    }

    private int getHeightWithoutXAxis() {
        return getHeight() - mAxisXHeight;
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

    public void setChartData(ChartData data) {
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
