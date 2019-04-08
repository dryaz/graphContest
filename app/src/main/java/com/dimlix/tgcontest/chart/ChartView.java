package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to display chart data.
 * <p>
 * Some part is the same with {@link ChartControlView} but code is kept separate because
 * looks like it should be more flexible rather then common,
 * e.g. chart line toggling animation is different.
 * <p>
 * Package protected class, use ChartLayout as a single ViewGroup which contains both
 * {@link ChartView} and {@link ChartControlView}.
 */
class ChartView extends View {

    private static final int TOGGLE_ANIM_DURATION = 300;
    // When user change view region chart y axis is animated with this duration.
    private static final int SHIFT_ANIM_DURATION = 150;
    private static final int NUM_HOR_AXIS = 6;
    private static final int NUM_X_AXIS_MIN = 3;
    private static final int NUM_X_AXIS_MAX = 5;
    public static final int OFFSET_DRAW_NUM = 2;
    public static final int OFFSET_X_AXIS_DRAW_NUM = 8;
    private static final int DISTANCE_THRESHOLD = 4;
    public static final int INFO_PANEL_SHIFT = 50;
    public static final int MAX_AXIS_ALPHA = 19;
    private static final long TOUCH_THRESHOLD = 500;
    private static final long CLICK_THRESHOLD = 100;

    private float[] mPathPoints;
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    //    private String mLineToToggle = null;
    private Set<String> mLinesToToggle = new HashSet<>();

    private ChartData mChartData = null;

    private long mStartToggleTime = -1;
    private long mStartXAxisAnimTime = -1;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever = -1;
    private long mLastMinPossibleYever = -1;

    private long mLastMaxMinPossibleYeverDiff = -1;

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

    private int mSideMargin;

    private int mAxisXHeight;
    private int mPrevDistance;

    private float mTouchXValue = -1;

    private Listener mListener;

    private InfoPanelViewHolder mInfoPanelViewHolder;

    private boolean mIsAnimationsEnabled = true;

    private int mDragThreshold;
    private long mFirstIteractionTime = -1;
    private float mXDesisionPos = -1;
    private float mYDesisionPos = -1;
    private boolean mDecisionMade = false;

    private float mLastInfoPanelPositionX = -1;
    private boolean mInTouchPanelBounds = false;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void init() {
        mDragThreshold = getContext().getResources().getDimensionPixelSize(R.dimen.drag_threshold);
        mAxisWidth = getContext().getResources().getDimensionPixelSize(R.dimen.axis_width);
        mAxisXHeight = getContext().getResources().getDimensionPixelSize(R.dimen.axis_x_height);
        mAxisTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.axis_text_size);
        mAxisSelectedCircleSize = getContext().getResources().getDimensionPixelSize(R.dimen.axis_selected_circle);

        mSideMargin = getContext().getResources().getDimensionPixelSize(R.dimen.side_margin);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.axisChartColor, typedValue, true);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(typedValue.data);
        mAxisPaint.setAlpha(MAX_AXIS_ALPHA);
        mAxisPaint.setStyle(Paint.Style.FILL);
        mAxisPaint.setStrokeWidth(mAxisWidth);

        theme.resolveAttribute(R.attr.axisChartTextColor, typedValue, true);
        mAxisTextPaint = new Paint();
        mAxisTextPaint.setColor(typedValue.data);
        mAxisTextPaint.setTextSize(mAxisTextSize);

        theme.resolveAttribute(R.attr.axisChartCouchedCircleFillColor, typedValue, true);
        mTouchedCirclePaint = new Paint();
        mTouchedCirclePaint.setStyle(Paint.Style.FILL);
        mTouchedCirclePaint.setColor(typedValue.data);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mListener != null) {
                    mListener.onViewTouched();
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (mFirstIteractionTime < 0) {
                        mInTouchPanelBounds = mLastInfoPanelPositionX > 0
                                && event.getY() > INFO_PANEL_SHIFT
                                && event.getY() < INFO_PANEL_SHIFT + mInfoPanelViewHolder.mInfoView.getHeight()
                                && event.getX() > mLastInfoPanelPositionX
                                && event.getX() < mLastInfoPanelPositionX + mInfoPanelViewHolder.mInfoView.getWidth();
                        if (mInTouchPanelBounds) {
                            mTouchXValue = -1;
                        }
                        mFirstIteractionTime = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - mFirstIteractionTime > TOUCH_THRESHOLD) {
                        mDecisionMade = true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mDecisionMade = false;
                        mYDesisionPos = event.getY();
                        mXDesisionPos = event.getX();
                    } else {
                        if (Math.abs(mYDesisionPos - event.getY()) > mDragThreshold && !mDecisionMade) {
                            mListener.onViewReleased();
                            mFirstIteractionTime = -1;
                            mTouchXValue = -1;
                            mDecisionMade = false;
                        }
                        if (Math.abs(mXDesisionPos - event.getX()) > mDragThreshold && !mDecisionMade) {
                            mDecisionMade = true;
                        }
                        if (mDecisionMade) {
                            mTouchXValue = event.getX();
                        }
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mInTouchPanelBounds) {
                        mTouchXValue = -1;
                    } else {
                        mTouchXValue = event.getX();
                    }
                    mFirstIteractionTime = -1;
                }
                invalidate();
                return true;
            }
        });
    }

    void reInit() {
        init();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.infoBgColor, typedValue, true);
        mInfoPanelViewHolder.mInfoView.setBackgroundTintList(ColorStateList.valueOf(typedValue.data));
        theme.resolveAttribute(R.attr.infoDateTextColor, typedValue, true);
        mInfoPanelViewHolder.mInfoViewTitle.setTextColor(typedValue.data);
        for (Pair<TextView, TextView> views : mInfoPanelViewHolder.mLineValue) {
            views.first.setTextColor(typedValue.data);
        }
        invalidate();
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
        mAxisPaint.setAlpha(MAX_AXIS_ALPHA);

        int firstPointToShow =
                Math.max((int) Math.floor((mLeftCurrentXBoarderValue - mSideMargin) / mStepXForMaxScale) - OFFSET_DRAW_NUM, 0);
        int lastPointToShow = Math.min((int) Math.ceil((mRightCurrentXBoarderValue + mSideMargin) / mStepXForMaxScale) + OFFSET_DRAW_NUM,
                mChartData.getSize() - 1);

        long maxPossibleYever = 0;
        long minPossibleYever = Integer.MAX_VALUE;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            ChartData.YData yData = mChartData.getYValues().get(k);
            if (!yData.isShown()) continue;
            // Extra iteration over visible fragment needed to find out y scale factor.
            for (int i = firstPointToShow; i < lastPointToShow; i++) {
                Long nextValue = yData.getValues().get(i);
                if (maxPossibleYever < nextValue) maxPossibleYever = nextValue;
                if (minPossibleYever > nextValue) minPossibleYever = nextValue;
            }
        }

        if (mLastMaxPossibleYever == -1) {
            mLastMaxPossibleYever = maxPossibleYever;
        }

        if (mLastMinPossibleYever == -1) {
            mLastMinPossibleYever = minPossibleYever;
        }

        mLastMaxMinPossibleYeverDiff = mLastMaxPossibleYever - mLastMinPossibleYever;

        if (maxPossibleYever == 0) {
            // Prevent single line from flying up
            maxPossibleYever = mLastMaxPossibleYever;
        }

        if (minPossibleYever == Integer.MAX_VALUE) {
            // Prevent single line from flying up
            maxPossibleYever = mLastMinPossibleYever;
        }

        float lineToggleProgress = 1;
        if (mIsAnimationsEnabled
                && (!mLinesToToggle.isEmpty() || maxPossibleYever != mLastMaxPossibleYever || minPossibleYever != mLastMinPossibleYever)) {
            if (mStartToggleTime == -1) {
                mStartToggleTime = System.currentTimeMillis();
            }
            long elapsed = System.currentTimeMillis() - mStartToggleTime;
            if (!mLinesToToggle.isEmpty()) {
                lineToggleProgress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
            } else {
                lineToggleProgress = Math.min((float) elapsed / SHIFT_ANIM_DURATION, 1);
            }
        }

        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        float maxPossibleYeverComputed =
                mLastMaxPossibleYever + (maxPossibleYever - mLastMaxPossibleYever) * lineToggleProgress;
        float minPossibleYeverComputed =
                mLastMinPossibleYever + (minPossibleYever - mLastMinPossibleYever) * lineToggleProgress;
        float yStep = (float) getHeightWithoutXAxis() / (maxPossibleYeverComputed - minPossibleYeverComputed);

        drawChartLines(canvas, (long) minPossibleYeverComputed, firstPointToShow, lastPointToShow, lineToggleProgress, scale, translation, yStep);
        drawChartYAxis(canvas, maxPossibleYever, minPossibleYever, lineToggleProgress);
        drawTouchedInfo(canvas, (long) minPossibleYeverComputed, scale, translation, yStep);

        float xAxisValuesProgress = drawXAxis(canvas, firstPointToShow, lastPointToShow, scale, translation);
        loop(maxPossibleYever, minPossibleYever, lineToggleProgress, xAxisValuesProgress);
    }

    private float drawXAxis(Canvas canvas, int firstPointToShow, int lastPointToShow, float scale, float translation) {
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
            nextIndexToDrawXAxisValue = Math.max((mLastXValuesStep - firstPointToShowForAxis % mLastXValuesStep) + firstPointToShowForAxis, 0);
            if (mStartXAxisAnimTime == -1) {
                mPrevNextIndexToDraw = nextIndexToDrawXAxisValue;
            } else {
                if (!mHideAnimation) {
                    // When we show new values we must be sure that opacity values still the same
                    nextIndexToDrawXAxisValue = Math.max(mPrevNextIndexToDraw, 0);
                }
                nextIndexToDrawXAxisValueToAnimate = mPrevNextIndexToDraw;
            }
        }

        float xAxisValuesProgress = 1;

        if (mStartXAxisAnimTime != -1 && mIsAnimationsEnabled) {
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
                String text = mChartData.getXStringValues().get(nextIndexToDrawXAxisValueToAnimate).getDateMonthOnly();
                float x = (nextIndexToDrawXAxisValueToAnimate * mStepXForMaxScale - translation) * scale;
                float xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
                canvas.drawText(text, xWithMargin, getHeight() - (float) mAxisTextSize / 2,
                        mAxisTextPaint);
                nextIndexToDrawXAxisValueToAnimate += animatedStep;
            }
        }

        while (nextIndexToDrawXAxisValue < lastPointToShowForAxis) {
            mAxisTextPaint.setAlpha(255);
            String text = mChartData.getXStringValues().get(nextIndexToDrawXAxisValue).getDateMonthOnly();
            float x = (nextIndexToDrawXAxisValue * mStepXForMaxScale - translation) * scale;
            float xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
            canvas.drawText(text, xWithMargin, getHeight() - (float) mAxisTextSize / 2, mAxisTextPaint);
            if (xAxisValuesProgress < 1) {
                nextIndexToDrawXAxisValue += animatedStep;
            } else {
                nextIndexToDrawXAxisValue += mLastXValuesStep;
            }
        }
        return xAxisValuesProgress;
    }

    private void loop(long maxPossibleYever, long minPossibleYever, float lineToggleProgress, float xAxisValuesProgress) {
        // Run draw loop in case animation is running.
        if (lineToggleProgress < 1 || xAxisValuesProgress < 1) {
            invalidate();
        } else {
            if (xAxisValuesProgress >= 1) {
                mStartXAxisAnimTime = -1;
                mPrevLastXValuesStep = mLastXValuesStep;
            }
            if (lineToggleProgress >= 1) {
                mStartToggleTime = -1;
            }
            if (!mLinesToToggle.isEmpty() || mLastMaxPossibleYever != maxPossibleYever || mLastMinPossibleYever != minPossibleYever) {
                mLastMaxPossibleYever = maxPossibleYever;
                mLastMinPossibleYever = minPossibleYever;
                mLinesToToggle.clear();
                invalidate();
            }
        }
    }

    private void drawTouchedInfo(Canvas canvas, long minPossibleComputed, float scale, float translation, float yStep) {
        // Draw info about touched section
        if (mTouchXValue > 0) {
            float xWithMarginToSearch = mTouchXValue - (1 - 2 * mTouchXValue / getWidth()) * mSideMargin;
            int nearestIndexTouched = Math.round((xWithMarginToSearch / scale + translation) / mStepXForMaxScale);
            if (nearestIndexTouched >= mChartData.getSize() - 1) {
                nearestIndexTouched = mChartData.getSize() - 1;
            } else if (nearestIndexTouched < 0) {
                nearestIndexTouched = 0;
            }
            float x = (nearestIndexTouched * mStepXForMaxScale - translation) * scale;
            float xValToDraw = x + (1 - 2 * x / getWidth()) * mSideMargin;
            canvas.drawLine(xValToDraw, 0, xValToDraw, getHeightWithoutXAxis() - mAxisWidth, mAxisPaint);
            for (int k = 0; k < mChartData.getYValues().size(); k++) {
                ChartData.YData yData = mChartData.getYValues().get(k);
                Pair<TextView, TextView> tvPair = mInfoPanelViewHolder.mLineValue.get(k);
                Long yValue = yData.getValues().get(nearestIndexTouched);
                if (!yData.isShown()) {
                    tvPair.first.setVisibility(GONE);
                    tvPair.second.setVisibility(GONE);
                    continue;
                } else {
                    tvPair.first.setVisibility(VISIBLE);
                    tvPair.second.setVisibility(VISIBLE);
                    tvPair.second.setText(Utils.prettyFormat(yValue));
                }
                Paint paint = mPaints.get(yData.getVarName());
                if (paint == null) {
                    throw new RuntimeException("There is no color info for " + yData.getVarName());
                }
                canvas.drawCircle(xValToDraw,
                        getHeightWithoutXAxis() - (yValue - minPossibleComputed) * yStep,
                        mAxisSelectedCircleSize, mTouchedCirclePaint);
                canvas.drawCircle(xValToDraw,
                        getHeightWithoutXAxis() - (yValue - minPossibleComputed) * yStep,
                        mAxisSelectedCircleSize, paint);
            }

            mInfoPanelViewHolder.mInfoViewTitle.setText(mChartData.getXStringValues().get(nearestIndexTouched).getExtendedDate());

            int widthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(ViewGroup.LayoutParams.WRAP_CONTENT), MeasureSpec.UNSPECIFIED);
            int heightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(ViewGroup.LayoutParams.WRAP_CONTENT), MeasureSpec.UNSPECIFIED);
            mInfoPanelViewHolder.mInfoView.measure(widthSpec, heightSpec);
            int infoWidth = mInfoPanelViewHolder.mInfoView.getMeasuredWidth();
            mInfoPanelViewHolder.mInfoView.layout(0, 0, infoWidth, mInfoPanelViewHolder.mInfoView.getMeasuredHeight());
            canvas.save();
            float translateValue;
            if (mTouchXValue > getWidth() / 2) {
                translateValue = mTouchXValue - INFO_PANEL_SHIFT - infoWidth;
            } else {
                translateValue = mTouchXValue + INFO_PANEL_SHIFT;
            }
            mLastInfoPanelPositionX = translateValue;
            canvas.translate(translateValue, INFO_PANEL_SHIFT);
            mInfoPanelViewHolder.mInfoView.draw(canvas);
            canvas.restore();
        } else {
            mLastInfoPanelPositionX = -1;
        }
    }

    private void drawChartYAxis(Canvas canvas, long maxPossibleYever, long minPossibleYever, float lineToggleProgress) {
        // Draw chart Y axis
        int yAxisStep = Math.round((float) (maxPossibleYever - minPossibleYever) / NUM_HOR_AXIS);
        int prevYAxisStep = Math.round((float) (mLastMaxPossibleYever - mLastMinPossibleYever) / NUM_HOR_AXIS);
        int yDistance = getHeightWithoutXAxis() / (NUM_HOR_AXIS);
        byte animDirection;
        if (mLastMaxPossibleYever > maxPossibleYever || mLastMinPossibleYever < minPossibleYever) {
            animDirection = -1;
        } else if (mLastMaxPossibleYever < maxPossibleYever || mLastMinPossibleYever > minPossibleYever) {
            animDirection = 1;
        } else {
            animDirection = 0;
        }
        for (int i = 0; i < NUM_HOR_AXIS; i++) {
            int y = (getHeightWithoutXAxis() - mAxisWidth - yDistance * i);
            y += (animDirection * (lineToggleProgress - 1) * yDistance) * (i + 1);
            if (mLastMaxPossibleYever != maxPossibleYever || mLastMinPossibleYever != minPossibleYever) {
                mAxisTextPaint.setAlpha((int) ((lineToggleProgress) * 255));
                mAxisPaint.setAlpha((int) ((lineToggleProgress) * MAX_AXIS_ALPHA));
            }
            canvas.drawLine(mSideMargin, y, getWidth(), y, mAxisPaint);
            canvas.drawText(Utils.coolFormat(yAxisStep * (i) + minPossibleYever), mSideMargin, y - (float) mAxisTextSize / 2, mAxisTextPaint);
            if (mLastMaxPossibleYever != maxPossibleYever || mLastMinPossibleYever != minPossibleYever) {
                int yOfPrev = (getHeightWithoutXAxis() - mAxisWidth - yDistance * i);
                yOfPrev += (animDirection * (lineToggleProgress) * yDistance) * (i + 1);
                mAxisTextPaint.setAlpha((int) ((1 - lineToggleProgress) * 255));
                mAxisPaint.setAlpha((int) ((1 - lineToggleProgress) * MAX_AXIS_ALPHA));
                canvas.drawLine(mSideMargin, yOfPrev, getWidth() + mSideMargin, yOfPrev, mAxisPaint);
                canvas.drawText(Utils.coolFormat(prevYAxisStep * (i) + minPossibleYever), mSideMargin, yOfPrev - (float) mAxisTextSize / 2, mAxisTextPaint);

            }
        }
    }

    private void drawChartLines(Canvas canvas, long computedMinPossibleY, int firstPointToShow, int lastPointToShow, float lineToggleProgress, float scale, float translation, float yStep) {
        // Draw chart lines
        float x;
        float xWithMargin;
        float y;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            ChartData.YData yData = mChartData.getYValues().get(k);


            if (!mLinesToToggle.contains(yData.getVarName())) {
                if (!yData.isShown()) {
                    continue;
                }
            }

            x = (firstPointToShow * mStepXForMaxScale - translation) * scale;
            xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
            mPathPoints[0] = xWithMargin;
            mPathPoints[1] = getHeightWithoutXAxis() - (yData.getValues().get(0) - computedMinPossibleY) * yStep;

            int pointIndex = 2;
            for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                x = (i * mStepXForMaxScale - translation) * scale;
                xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
                y = getHeightWithoutXAxis() - (yData.getValues().get(i) - computedMinPossibleY) * yStep;
                mPathPoints[pointIndex] = xWithMargin;
                mPathPoints[pointIndex + 1] = y;
                mPathPoints[pointIndex + 2] = xWithMargin;
                mPathPoints[pointIndex + 3] = y;
                pointIndex += 4;
            }

            Paint paint = mPaints.get(yData.getVarName());
            if (paint == null) {
                throw new RuntimeException("There is no color info for " + yData.getVarName());
            }

            if (mLinesToToggle.contains(yData.getVarName())) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * lineToggleProgress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - lineToggleProgress)), 0));
                }
            }

            canvas.drawLines(mPathPoints, 0, pointIndex - 1, paint);
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup infoView = (ViewGroup) inflater.inflate(R.layout.float_info_panel, null);
        TextView infoViewTitle = infoView.findViewById(R.id.tvTitle);

        mPathPoints = new float[data.getXValues().size() * 4];

        mChartData = data;
        mPaints.clear();
        List<Pair<TextView, TextView>> valueViews = new ArrayList<>(mChartData.getYValues().size());
        for (ChartData.YData yData : mChartData.getYValues()) {
            Paint paint = new Paint();

            int lineColor = Color.parseColor(yData.getColor());
            paint.setColor(lineColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeWidth(4);

            mPaints.put(yData.getVarName(), paint);
            ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.item_float_info_panel, infoView, false);
            TextView value = viewGroup.findViewById(R.id.tvValue);
            TextView axisName = viewGroup.findViewById(R.id.tvAxis);
            value.setTextColor(lineColor);
            axisName.setText(yData.getAlias());
            valueViews.add(Pair.create(axisName, value));
            infoView.addView(viewGroup);
        }
        mInfoPanelViewHolder = new InfoPanelViewHolder(infoView, infoViewTitle, valueViews);
        invalidate();
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        mIsAnimationsEnabled = animationsEnabled;
    }

    /**
     * Show/hide chart line on chart
     *
     * @param yVarName name of affected line
     */
    void onYChartToggled(String yVarName) {
        if (mStartToggleTime == -1) {
            mStartToggleTime = System.currentTimeMillis();
        }
        mLinesToToggle.add(yVarName);
        invalidate();
    }

    public interface Listener {
        void onViewTouched();

        void onViewReleased();
    }

    private class InfoPanelViewHolder {
        ViewGroup mInfoView;
        TextView mInfoViewTitle;
        List<Pair<TextView, TextView>> mLineValue;

        public InfoPanelViewHolder(ViewGroup infoView, TextView infoViewTitle, List<Pair<TextView, TextView>> lineValue) {
            mInfoView = infoView;
            mInfoViewTitle = infoViewTitle;
            mLineValue = lineValue;
        }
    }
}
