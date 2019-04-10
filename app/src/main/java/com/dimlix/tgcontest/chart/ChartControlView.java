package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to to control displayed chart.
 * <p>
 * Some part is the same with {@link ChartView} but code is kept separate because
 * looks like it should be more flexible rather then common,
 * e.g. chart line toggling animation is different.
 */
class ChartControlView extends View {
    // Animation duration for switching chart lies on/off
    private static final int TOGGLE_ANIM_DURATION = 300;
    // Distance between left and right draggable controls
    private static final int MIN_MAX_DIFF_THRESHOLD = (int) (ChartLayout.MAX_DISCRETE_PROGRESS * 0.1);

    public @interface TouchMode {
        int NONE = 0;
        int LEFT_BOARDER = 1;
        int RIGHT_BOARDER = 2;
        int DRAG_REGION = 3;

    }

    // To avoid object creation during onDraw save path locally.
    private float[] mPathPoints;

    // Each line has it's own paint.
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    // Animation for toggled line and other lines are different --> keep id of toggled line.
    private Set<String> mLinesToToggle = new HashSet<>();

    private ChartData mChartData = null;

    private long mStartToggleTime = -1;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever;
    private long mLastMinPossibleYever;

    private long[] mLastMaxPossibleEachLine;
    private long[] mLastMinPossibleEachLine;

    private Listener mListener;

    private float mMinPos = 0;
    private float mMaxPos = ChartLayout.MAX_DISCRETE_PROGRESS;

    private Paint mMaskPaint;
    private Paint mDragPaint;
    private Paint mDragElementPaint;

    private int mDragZoneWidth;
    private int mDragBoarderHeight;
    // Draggable control width and touch zone could be different, guidelines says to use at least
    // 48dp for touch objects but as per design width is different.
    private int mDragZoneTouchWidth;

    private boolean mIsAnimationsEnabled = true;

    @TouchMode
    private int mCurrentMode = TouchMode.NONE;
    // Support second gesture to drag from both sides at the time
    @TouchMode
    private int mMultitouchMode = TouchMode.NONE;
    private float mLastXPost = -1;

    private int mDragThreshold;
    private float mXDesisionPos = -1;
    private float mYDesisionPos = -1;
    private boolean mDecisionMade = false;

    private int mSideMargin = -1;
    private int mDragZoneRadius = -1;
    private int mDragZoneElementHeight = -1;
    private int mDragZoneElementWidth = -1;

    public ChartControlView(Context context) {
        super(context);
        init();
    }

    public ChartControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void reInit() {
        init();
        invalidate();
    }

    private void init() {
        mSideMargin = getContext().getResources().getDimensionPixelSize(R.dimen.side_margin);
        mDragThreshold = getContext().getResources().getDimensionPixelSize(R.dimen.drag_threshold);
        mDragZoneWidth = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_width);
        mDragZoneTouchWidth = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_touch_width);
        mDragBoarderHeight = getContext().getResources().getDimensionPixelSize(R.dimen.drag_border_height);

        mDragZoneRadius = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_round_radius);
        mDragZoneElementHeight = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_element_height);
        mDragZoneElementWidth = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_element_width);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.maskColor, typedValue, true);

        mMaskPaint = new Paint();
        mMaskPaint.setColor(typedValue.data);
        mMaskPaint.setStyle(Paint.Style.FILL);

        theme.resolveAttribute(R.attr.maskDragColor, typedValue, true);
        mDragPaint = new Paint();
        mDragPaint.setColor(typedValue.data);
        mDragPaint.setStyle(Paint.Style.FILL);

        mDragElementPaint = new Paint();
        mDragElementPaint.setColor(Color.WHITE);
        mDragElementPaint.setStyle(Paint.Style.FILL);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Don't handle 3+ touches
                if (event.getActionIndex() > 1) return true;
                if (mListener != null) {
                    mListener.onViewTouched();
                }

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mCurrentMode = TouchMode.NONE;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                    if (event.getActionIndex() == 0) {
                        mCurrentMode = mMultitouchMode;
                    }
                    mMultitouchMode = TouchMode.NONE;
                }

                int maxTouchIndex = event.getPointerCount() - 1;

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                        || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    mDecisionMade = true;
                    float left = mMinPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
                    float leftLeft = left - (float) mDragZoneTouchWidth / 2;
                    float leftRight = left + (float) mDragZoneTouchWidth / 2;

                    float right = mMaxPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
                    float rightLeft = right - (float) mDragZoneTouchWidth / 2;
                    float rightRight = right + (float) mDragZoneTouchWidth / 2;

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mDecisionMade = false;
                        mYDesisionPos = event.getY();
                        mXDesisionPos = event.getX();
                        if (event.getX() >= leftLeft && event.getX() <= leftRight) {
                            mCurrentMode = TouchMode.LEFT_BOARDER;
                        } else if (event.getX() >= rightLeft && event.getX() <= rightRight) {
                            mCurrentMode = TouchMode.RIGHT_BOARDER;
                        } else if (event.getX() >= leftLeft && event.getX() <= rightRight) {
                            mCurrentMode = TouchMode.DRAG_REGION;
                            mLastXPost = event.getX();
                        }
                    } else if (mCurrentMode != TouchMode.DRAG_REGION) {
                        if (event.getX(maxTouchIndex) >= leftLeft && event.getX(maxTouchIndex) <= leftRight) {
                            mMultitouchMode = TouchMode.LEFT_BOARDER;
                        } else if (event.getX(maxTouchIndex) >= rightLeft && event.getX(maxTouchIndex) <= rightRight) {
                            mMultitouchMode = TouchMode.RIGHT_BOARDER;
                        } else if (event.getX(maxTouchIndex) >= leftLeft && event.getX(maxTouchIndex) <= rightRight
                                && mCurrentMode != TouchMode.LEFT_BOARDER
                                && mCurrentMode != TouchMode.RIGHT_BOARDER) {
                            mMultitouchMode = TouchMode.DRAG_REGION;
                            mLastXPost = event.getX(maxTouchIndex);
                        }
                    }
                }

                if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float min = mMinPos;
                    float max = mMaxPos;
                    if (Math.abs(mYDesisionPos - event.getY()) > mDragThreshold && !mDecisionMade) {
                        mListener.onViewReleased();
                        mDecisionMade = true;
                    }
                    if (Math.abs(mXDesisionPos - event.getX()) > mDragThreshold && !mDecisionMade) {
                        mDecisionMade = true;
                    }
                    switch (mCurrentMode) {
                        case TouchMode.LEFT_BOARDER:
                            min = (int) ((event.getX() / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS);
                            break;
                        case TouchMode.RIGHT_BOARDER:
                            max = (int) ((event.getX() / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS);
                            break;
                        case TouchMode.DRAG_REGION:
                            float diff = ((event.getX() - mLastXPost) / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS;
                            mLastXPost = event.getX();
                            if (min + diff < 0 || max + diff > ChartLayout.MAX_DISCRETE_PROGRESS) {
                                return true;
                            }
                            min += diff;
                            max += diff;
                            break;
                    }

                    switch (mMultitouchMode) {
                        case TouchMode.LEFT_BOARDER:
                            min = (int) ((event.getX(maxTouchIndex) / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS);
                            break;
                        case TouchMode.RIGHT_BOARDER:
                            max = (int) ((event.getX(maxTouchIndex) / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS);
                            break;
                        case TouchMode.DRAG_REGION:
                            float diff = ((event.getX(maxTouchIndex) - mLastXPost) / v.getWidth()) * ChartLayout.MAX_DISCRETE_PROGRESS;
                            mLastXPost = event.getX(maxTouchIndex);
                            if (min + diff < 0 || max + diff > ChartLayout.MAX_DISCRETE_PROGRESS) {
                                return true;
                            }
                            min += diff;
                            max += diff;
                            break;
                    }

                    setMinMax(min, max);
                }
                return true;
            }
        });
    }

    public void setMinMax(float min, float max) {
        if (max - min < MIN_MAX_DIFF_THRESHOLD) return;
        mMinPos = Math.max(min, 0);
        mMaxPos = Math.min(max, ChartLayout.MAX_DISCRETE_PROGRESS);
        if (mListener != null) {
            mListener.onBoarderChange(Math.round(mMinPos), Math.round(mMaxPos));
        }
        invalidate();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mChartData == null) {
            // TODO draw empty state if needed
            return;
        }

        if (mStepXForMaxScale == 0) {
            // Chart range is not set yet, skip.
            return;
        }

        long elapsed = System.currentTimeMillis() - mStartToggleTime;
        float progress = 1;
        if (!mLinesToToggle.isEmpty() && mIsAnimationsEnabled) {
            progress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
        }

        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        int firstPointToShow =
                Math.max((int) Math.floor(mLeftCurrentXBoarderValue / mStepXForMaxScale) - 1, 0);
        int lastPointToShow = Math.min((int) Math.floor(mRightCurrentXBoarderValue / mStepXForMaxScale) + 1,
                mChartData.getSize() - 1);

        if (mChartData.isDoubleYAxis()) {
            drawChartForEachLine(canvas, progress, scale, translation, firstPointToShow, lastPointToShow);
        } else {
            if (mChartData.isPercentage()) {
                drawPercentageChart(canvas, firstPointToShow, lastPointToShow);
            } else {
                drawCompoundChartLines(canvas, progress, scale, translation, firstPointToShow, lastPointToShow);
            }
        }
    }

    long[] maxPossibleYever = null;
    long[] minPossibleYever = null;
    float[] maxPossibleYeverComputed = null;
    float[] minPossibleYeverComputed = null;
    float[] yStep = null;

    private void drawChartForEachLine(Canvas canvas, float progress, float scale, float translation, int firstPointToShow, int lastPointToShow) {
        if (maxPossibleYever == null) {
            maxPossibleYever = new long[mLastMinPossibleEachLine.length];
            minPossibleYever = new long[mLastMinPossibleEachLine.length];
            maxPossibleYeverComputed = new float[mLastMinPossibleEachLine.length];
            minPossibleYeverComputed = new float[mLastMinPossibleEachLine.length];
            yStep = new float[mLastMinPossibleEachLine.length];
        }
        for (int i = 0; i < maxPossibleYever.length; i++) {
            maxPossibleYever[i] = 0;
            minPossibleYever[i] = Integer.MAX_VALUE;
        }
        boolean needAnim = false;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            ChartData.YData yData = mChartData.getYValues().get(k);
            if (!yData.isShown()) continue;
            // Extra iteration over visible fragment needed to find out y scale factor.
            for (int i = firstPointToShow; i < lastPointToShow; i++) {
                Long nextValue = yData.getValues().get(i);
                if (maxPossibleYever[k] < nextValue) maxPossibleYever[k] = nextValue;
                if (minPossibleYever[k] > nextValue) minPossibleYever[k] = nextValue;
            }

            if (mLastMaxPossibleEachLine[k] == -1) {
                mLastMaxPossibleEachLine[k] = maxPossibleYever[k];
            }
            if (mLastMinPossibleEachLine[k] == -1) {
                mLastMinPossibleEachLine[k] = minPossibleYever[k];
            }
            needAnim = needAnim
                    || mLastMaxPossibleEachLine[k] != maxPossibleYever[k]
                    || mLastMinPossibleEachLine[k] != minPossibleYever[k];

        }

        float x;
        float y;

        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            maxPossibleYeverComputed[k] =
                    mLastMaxPossibleEachLine[k] + (maxPossibleYever[k] - mLastMaxPossibleEachLine[k]) * progress;
            minPossibleYeverComputed[k] =
                    mLastMinPossibleEachLine[k] + (minPossibleYever[k] - mLastMinPossibleEachLine[k]) * progress;

            ChartData.YData yData = mChartData.getYValues().get(k);

            if (!mLinesToToggle.contains(yData.getVarName())) {
                if (!yData.isShown()) {
                    continue;
                }
            } else {
                if (yData.isShown()) {
                    maxPossibleYeverComputed[k] = maxPossibleYever[k];
                    minPossibleYeverComputed[k] = minPossibleYever[k];
                } else {
                    maxPossibleYeverComputed[k] = mLastMaxPossibleEachLine[k];
                    minPossibleYeverComputed[k] = mLastMinPossibleEachLine[k];
                }
            }
            float yStep = (float) getHeight() / (maxPossibleYeverComputed[k] - minPossibleYeverComputed[k]);
            x = (firstPointToShow * mStepXForMaxScale - translation) * scale;
            x = x + (1 - 2 * x / getWidth()) * mSideMargin;
            mPathPoints[0] = x;
            y = getHeight() - (yData.getValues().get(0) - minPossibleYeverComputed[k]) * yStep;
            y = y + (1 - 2 * y / getHeight()) * mDragBoarderHeight;
            mPathPoints[1] = y;
            int pointIndex = 2;
            for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                x = (i * mStepXForMaxScale - translation) * scale;
                x = x + (1 - 2 * x / getWidth()) * mSideMargin;
                y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed[k]) * yStep;
                y = y + (1 - 2 * y / getHeight()) * mDragBoarderHeight;
                mPathPoints[pointIndex] = x;
                mPathPoints[pointIndex + 1] = y;
                mPathPoints[pointIndex + 2] = x;
                mPathPoints[pointIndex + 3] = y;
                pointIndex += 4;
            }

            Paint paint = mPaints.get(yData.getVarName());
            if (paint == null) {
                throw new RuntimeException("There is no color info for " + yData.getVarName());
            }

            if (mLinesToToggle.contains(yData.getVarName())) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * progress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - progress)), 0));
                }
            }

            canvas.drawLines(mPathPoints, 0, pointIndex - 1, paint);
        }

        drawControlsOverlay(canvas);
        loop(maxPossibleYever, minPossibleYever, progress);
    }

    private void loop(long[] maxPossibleYever, long[] minPossibleYever, float progress) {
        if (progress < 1 && (!mLinesToToggle.isEmpty())) {
            invalidate();
        } else {
            mStartToggleTime = -1;
            for (int i = 0; i < mLastMaxPossibleEachLine.length; i++) {
                mLastMaxPossibleEachLine[i] = maxPossibleYever[i];
                mLastMinPossibleEachLine[i] = minPossibleYever[i];
            }
            if (!mLinesToToggle.isEmpty()) {
                mLinesToToggle.clear();
                invalidate();
            }
        }
    }

    private void drawPercentageChart(Canvas canvas, int firstPointToShow, int lastPointToShow) {
        boolean fillFirstLine = false;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            ChartData.YData yData = mChartData.getYValues().get(k);
            if (!yData.isShown()) continue;
            // Extra iteration over visible fragment needed to find out y scale factor.
            for (int i = firstPointToShow; i <= lastPointToShow; i++) {
                Long nextValue = yData.getValues().get(i);
                if (!fillFirstLine) {
                    desiredMaxArray[i] = nextValue;
                } else {
                    desiredMaxArray[i] += nextValue;
                }
            }
            fillFirstLine = true;
        }

        if (lastMaxArray == null) {
            lastMaxArray = desiredMaxArray.clone();
        }

        float lineToggleProgress = 1;
        if (mIsAnimationsEnabled && !mLinesToToggle.isEmpty()) {
            if (mStartToggleTime == -1) {
                mStartToggleTime = System.currentTimeMillis();
            }
            long elapsed = System.currentTimeMillis() - mStartToggleTime;
            if (!mLinesToToggle.isEmpty()) {
                lineToggleProgress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
            }
        }

        float scale = (float) getWidth() / (mRightCurrentXBoarderValue - mLeftCurrentXBoarderValue);
        float translation = mLeftCurrentXBoarderValue;

        drawChartLinesPercentage(canvas, firstPointToShow, lastPointToShow, lineToggleProgress, scale, translation);
        drawControlsOverlay(canvas);
        loop(lineToggleProgress);
    }

    boolean isAnimatedLine = false;
    float prevYStep;
    float nextYStep;
    byte direction;

    private void drawCompoundChartLines(Canvas canvas, float progress, float scale, float translation, int firstPointToShow, int lastPointToShow) {
        long maxPossibleYever = 0;
        long minPossibleYever = Integer.MAX_VALUE;
        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            long localMaxPossibleYever = 0;
            ChartData.YData yData = mChartData.getYValues().get(k);
            if (yData.isBar()) minPossibleYever = 0;
            if (!yData.isShown()) continue;
            // Extra iteration over visible fragment needed to find out y scale factor.
            for (int i = firstPointToShow; i < lastPointToShow; i++) {
                Long nextValue = yData.getValues().get(i);
                if (localMaxPossibleYever < nextValue) localMaxPossibleYever = nextValue;
                if (minPossibleYever > nextValue) minPossibleYever = nextValue;
            }
            if (mChartData.isStacked()) {
                maxPossibleYever += localMaxPossibleYever;
            } else if (localMaxPossibleYever > maxPossibleYever) {
                maxPossibleYever = localMaxPossibleYever;
            }
        }

        if (maxPossibleYever == 0) {
            // Prevent single line from flying up
            maxPossibleYever = mLastMaxPossibleYever;
        }

        if (minPossibleYever == Integer.MAX_VALUE) {
            // Prevent single line from flying up
            minPossibleYever = mLastMinPossibleYever;
        }

        prevYStep = (float) getHeight() / (maxPossibleYever);
        nextYStep = (float) getHeight() / (mLastMaxPossibleYever);

        float x;
        float y;

        boolean isFirstChart = true;

        for (int k = 0; k < mChartData.getYValues().size(); k++) {
            float maxPossibleYeverComputed =
                    mLastMaxPossibleYever + (maxPossibleYever - mLastMaxPossibleYever) * progress;
            float minPossibleYeverComputed =
                    mLastMinPossibleYever + (minPossibleYever - mLastMinPossibleYever) * progress;

            ChartData.YData yData = mChartData.getYValues().get(k);

            if (!mLinesToToggle.contains(yData.getVarName())) {
                if (!yData.isShown()) {
                    continue;
                }
            } else {
                if (yData.isShown()) {
                    maxPossibleYeverComputed = maxPossibleYever;
                    minPossibleYeverComputed = minPossibleYever;
                } else {
                    maxPossibleYeverComputed = mLastMaxPossibleYever;
                    minPossibleYeverComputed = mLastMinPossibleYever;
                }
            }
            float yStep = (float) getHeight() / (maxPossibleYeverComputed - minPossibleYeverComputed);

            isAnimatedLine = mLinesToToggle.contains(yData.getVarName());
            if (isAnimatedLine) {
                if (maxPossibleYever > mLastMaxPossibleYever) {
                    direction = 1;
                } else {
                    direction = -1;
                }
            }

            Paint paint = mPaints.get(yData.getVarName());
            if (paint == null) {
                throw new RuntimeException("There is no color info for " + yData.getVarName());
            }
            int pointIndex;
            if (!yData.isBar()) {
                x = (firstPointToShow * mStepXForMaxScale - translation) * scale;
                x = x + (1 - 2 * x / getWidth()) * mSideMargin;
                mPathPoints[0] = x;
                y = getHeight() - (yData.getValues().get(0) - minPossibleYeverComputed) * yStep;
                y = y + (1 - 2 * y / getHeight()) * mDragBoarderHeight;
                mPathPoints[1] = y;
                pointIndex = 2;
                for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                    x = (i * mStepXForMaxScale - translation) * scale;
                    x = x + (1 - 2 * x / getWidth()) * mSideMargin;
                    y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * yStep;
                    y = y + (1 - 2 * y / getHeight()) * mDragBoarderHeight;
                    mPathPoints[pointIndex] = x;
                    mPathPoints[pointIndex + 1] = y;
                    mPathPoints[pointIndex + 2] = x;
                    mPathPoints[pointIndex + 3] = y;
                    pointIndex += 4;
                }
            } else {
                if (isFirstChart) {
                    pointIndex = 0;
                    isFirstChart = false;
                    for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                        x = (i * mStepXForMaxScale - translation) * scale;
                        x = x + (1 - 2 * x / getWidth()) * mSideMargin;
                        if (isAnimatedLine) {
                            if (yData.isShown()) {
                                y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * prevYStep * progress;
                            } else {
                                y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * nextYStep * (1 - progress);
                            }
                        } else {
                            y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * yStep;
                        }
                        y = y + (1 - 2 * y / getHeight()) * mDragBoarderHeight;
                        mPathPoints[pointIndex] = x;
                        mPathPoints[pointIndex + 1] = getHeight() - mDragBoarderHeight;
                        mPathPoints[pointIndex + 2] = x;
                        mPathPoints[pointIndex + 3] = y;
                        pointIndex += 4;
                    }
                } else {
                    float shift;
                    pointIndex = 0;
                    for (int i = firstPointToShow + 1; i <= lastPointToShow; i++) {
                        if (isAnimatedLine) {
                            if (yData.isShown()) {
                                y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * prevYStep * progress;
                            } else {
                                y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * nextYStep * (1 - progress);
                            }
                        } else {
                            y = getHeight() - (yData.getValues().get(i) - minPossibleYeverComputed) * yStep;
                        }
                        y = y + (1 - 2 * y / getHeight());
                        shift = getHeight() - mPathPoints[pointIndex + 3];
                        mPathPoints[pointIndex + 1] = getHeight() - shift;
                        mPathPoints[pointIndex + 3] = y - shift;
                        pointIndex += 4;
                    }
                }
            }


            if (mLinesToToggle.contains(yData.getVarName())) {
                if (yData.isShown()) {
                    paint.setAlpha(Math.min(((int) (255 * progress)), 255));
                } else {
                    paint.setAlpha(Math.max((int) (255 * (1 - progress)), 0));
                }
            }

            canvas.drawLines(mPathPoints, 0, pointIndex - 1, paint);
        }

        drawControlsOverlay(canvas);
        loop(maxPossibleYever, minPossibleYever, progress);
    }

    private int getHeightWithoutXAxis() {
        return getHeight() - mDragBoarderHeight;
    }

    List<Path> percentagePath;
    long[] lastMaxArray;
    long[] desiredMaxArray;

    private void drawChartLinesPercentage(Canvas canvas, int firstPointToShow, int lastPointToShow, float lineToggleProgress,
                                          float scale, float translation) {
        // Draw chart lines
        float x;
        float xWithMargin;
        float y;
        float yToCompute;

        int firstShownIndex = 0;
        for (int i = 0; i < mChartData.getYValues().size(); i++) {
            if (mChartData.getYValues().get(i).isShown() || mLinesToToggle.contains(mChartData.getYValues().get(i).getVarName())) {
                firstShownIndex = i;
                break;
            }
        }

        x = (firstPointToShow * mStepXForMaxScale - translation) * scale;
        xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
        Path p = percentagePath.get(0);
        p.reset();
        p.moveTo(xWithMargin, getHeightWithoutXAxis());
        p.lineTo(xWithMargin, mDragBoarderHeight);
        x = ((lastPointToShow) * mStepXForMaxScale - translation) * scale;
        xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
        p.lineTo(xWithMargin, mDragBoarderHeight);
        p.lineTo(xWithMargin, getHeightWithoutXAxis());
        p.close();

        Paint paint = mPaints.get(mChartData.getYValues().get(firstShownIndex).getVarName());

        canvas.drawPath(p, paint);

        if (firstShownIndex >= percentagePath.size()) {
            return;
        }

        x = (firstPointToShow * mStepXForMaxScale - translation) * scale;
        xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;

        for (int i = 0; i < percentagePath.size(); i++) {
            percentagePath.get(i).reset();
            percentagePath.get(i).moveTo(xWithMargin, getHeightWithoutXAxis());
        }

        for (int i = firstPointToShow; i <= lastPointToShow; i++) {
            long valueToCompute;

            valueToCompute = (long) (lastMaxArray[i] + (desiredMaxArray[i] - lastMaxArray[i]) * (lineToggleProgress));

            x = (i * mStepXForMaxScale - translation) * scale;
            xWithMargin = x + (1 - 2 * x / getWidth()) * mSideMargin;
            if (mLinesToToggle.contains(mChartData.getYValues().get(firstShownIndex).getVarName())) {
                if (mChartData.getYValues().get(firstShownIndex).isShown()) {
                    y = (float) mChartData.getYValues().get(firstShownIndex).getValues().get(i) / valueToCompute * lineToggleProgress;
                } else {
                    y = (float) mChartData.getYValues().get(firstShownIndex).getValues().get(i) / valueToCompute * (1-lineToggleProgress);
                }
            } else {
                y = (float) mChartData.getYValues().get(firstShownIndex).getValues().get(i) / valueToCompute;
            }
            y = mDragBoarderHeight + (getHeight() - 2 * mDragBoarderHeight) * y;
            percentagePath.get(firstShownIndex).lineTo(xWithMargin, y);
            for (int k = firstShownIndex + 1; k < mChartData.getYValues().size() - 1; k++) {
                ChartData.YData yData = mChartData.getYValues().get(k);
                if (!yData.isShown() && !mLinesToToggle.contains(yData.getVarName())) continue;
                if (mLinesToToggle.contains(yData.getVarName())) {
                    if (yData.isShown()) {
                        yToCompute = (float) yData.getValues().get(i) / valueToCompute * lineToggleProgress;
                    } else {
                        yToCompute = (float) yData.getValues().get(i) / valueToCompute * (1 - lineToggleProgress);
                    }
                } else {
                    yToCompute = (float) yData.getValues().get(i) / valueToCompute;
                }
                y += (getHeight() - 2 * mDragBoarderHeight) * yToCompute;
                percentagePath.get(k).lineTo(xWithMargin, y);
                if (i == lastPointToShow) {
                    percentagePath.get(k).lineTo(xWithMargin, getHeightWithoutXAxis());
                }
            }
            if (i == lastPointToShow) {
                percentagePath.get(firstShownIndex).lineTo(xWithMargin, getHeightWithoutXAxis());
            }
        }
        for (int i = firstShownIndex; i < percentagePath.size(); i++) {
            if (!mChartData.getYValues().get(i).isShown() && !mLinesToToggle.contains(mChartData.getYValues().get(i).getVarName())) continue;
            percentagePath.get(i).close();
            for (int j = i + 1; j < percentagePath.size() + 1; j++) {
                if (mChartData.getYValues().get(j).isShown() || mLinesToToggle.contains(mChartData.getYValues().get(j).getVarName())) {
                    canvas.drawPath(percentagePath.get(i), mPaints.get(mChartData.getYValues().get(j).getVarName()));
                    break;
                }
            }
        }
    }


    private void drawControlsOverlay(Canvas canvas) {
        // Draw left portion of mask
        float leftRight = mMinPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
        canvas.save();
        canvas.clipRect(0, mDragBoarderHeight, leftRight + mDragZoneWidth, getHeight() - mDragBoarderHeight);
        canvas.drawRoundRect(0, mDragBoarderHeight, leftRight + mDragZoneRadius + mDragZoneWidth, getHeight() - mDragBoarderHeight, mDragZoneRadius, mDragZoneRadius, mMaskPaint);
        canvas.restore();

        // Draw left draggable field
        canvas.save();
        canvas.clipRect(leftRight, 0, leftRight + mDragZoneWidth, getHeight());
        canvas.drawRoundRect(leftRight, 0, leftRight + mDragZoneWidth * 2, getHeight(), mDragZoneRadius, mDragZoneRadius, mDragPaint);
        canvas.restore();

        // Draw element on left draggable field
        float leftElement = leftRight + (mDragZoneWidth - mDragZoneElementWidth) / 2;
        int topElement = (getHeight() - mDragZoneElementHeight) / 2;
        canvas.drawRoundRect(leftElement, topElement, leftElement + mDragZoneElementWidth, topElement + mDragZoneElementHeight, mDragZoneRadius, mDragZoneRadius, mDragElementPaint);

        // Draw top border
        float rightLeft = mMaxPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
        canvas.drawRect(leftRight + mDragZoneWidth, 0, rightLeft - mDragZoneWidth,
                mDragBoarderHeight, mDragPaint);

        // Draw bottom border
        canvas.drawRect(leftRight + mDragZoneWidth, getHeight() - mDragBoarderHeight,
                rightLeft - mDragZoneWidth, getHeight(), mDragPaint);

        // Draw right portion of mask
        canvas.save();
        canvas.clipRect(rightLeft - mDragZoneWidth, mDragBoarderHeight, getWidth() - mDragBoarderHeight, getHeight());
        canvas.drawRoundRect(rightLeft - mDragZoneRadius - mDragZoneWidth, mDragBoarderHeight, getWidth(), getHeight() - mDragBoarderHeight, mDragZoneRadius, mDragZoneRadius, mMaskPaint);
        canvas.restore();

        // Draw right draggable field
        canvas.save();
        canvas.clipRect(rightLeft - mDragZoneWidth, 0, rightLeft, getHeight());
        canvas.drawRoundRect(rightLeft - mDragZoneWidth * 2, 0, rightLeft, getHeight(), mDragZoneRadius, mDragZoneRadius, mDragPaint);
        canvas.restore();

        // Draw element on right draggable field
        leftElement = rightLeft - (mDragZoneWidth + mDragZoneElementWidth) / 2;
        topElement = (getHeight() - mDragZoneElementHeight) / 2;
        canvas.drawRoundRect(leftElement, topElement, leftElement + mDragZoneElementWidth, topElement + mDragZoneElementHeight, mDragZoneRadius, mDragZoneRadius, mDragElementPaint);
    }

    private void loop(long maxPossibleYever, long minPossibleYever, float progress) {
        if (progress < 1 && (!mLinesToToggle.isEmpty())) {
            invalidate();
        } else {
            mStartToggleTime = -1;
            mLastMaxPossibleYever = maxPossibleYever;
            mLastMinPossibleYever = minPossibleYever;
            if (!mLinesToToggle.isEmpty()) {
                mLinesToToggle.clear();
                invalidate();
            }
        }
    }

    private void loop(float lineToggleProgress) {
        // Run draw loop in case animation is running.
        if (lineToggleProgress < 1 ) {
            invalidate();
        } else {
            if (lineToggleProgress >= 1) {
                for (int i = 0; i < lastMaxArray.length; i++) {
                    lastMaxArray[i] = desiredMaxArray[i];
                }
                mStartToggleTime = -1;
            }
            if (!mLinesToToggle.isEmpty()) {
                mLinesToToggle.clear();
                invalidate();
            }
        }
    }

    public void setChartData(final ChartData data) {

        if (getWidth() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    setChartData(data);
                }
            });
            return;
        }

        lastMaxArray = null;
        desiredMaxArray = new long[data.getYValues().get(0).getSize()];

        percentagePath = new ArrayList<>(data.getYValues().size() - 1);
        for (int i = 0; i < data.getYValues().size() - 1; i++) {
            percentagePath.add(new Path());
        }

        if (data.isDoubleYAxis()) {
            mLastMaxPossibleEachLine = new long[data.getYValues().size()];
            mLastMinPossibleEachLine = new long[data.getYValues().size()];
            for (int i = 0; i < mLastMaxPossibleEachLine.length; i++) {
                mLastMaxPossibleEachLine[i] = -1;
                mLastMinPossibleEachLine[i] = -1;
            }
        }

        mPathPoints = new float[data.getXValues().size() * 4];

        mChartData = data;
        mPaints.clear();
        for (ChartData.YData yData : mChartData.getYValues()) {
            Paint paint = new Paint();

            paint.setColor(Color.parseColor(yData.getColor()));
            if (mChartData.isPercentage()) {
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                paint.setStyle(Paint.Style.STROKE);
            }
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeWidth(4);

            mPaints.put(yData.getVarName(), paint);
        }

        mStepXForMaxScale = (float) getWidth() / (mChartData.getSize() - 1);
        mLeftCurrentXBoarderValue = 0;
        mRightCurrentXBoarderValue = getWidth();

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
        void onBoarderChange(int left, int right);

        void onViewTouched();

        void onViewReleased();
    }
}
