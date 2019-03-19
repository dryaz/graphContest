package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.IntDef;
import android.support.annotation.InterpolatorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.Touch;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to to control displayed chart.
 * <p>
 * Some part is the same with {@link ChartView} but code is kept separate because
 * looks like it should be more flexible rather then common,
 * e.g. chart line toggling animation is different.
 */
public class ChartControlView extends View {
    // Animation duration for switching chart lies on/off
    private static final int TOGGLE_ANIM_DURATION = 300;
    // Distance between left and right draggable controls
    private static final int MIN_MAX_DIFF_THRESHOLD = (int) (ChartLayout.MAX_DISCRETE_PROGRESS * 0.1);

    @IntDef({
            TouchMode.NONE,
            TouchMode.LEFT_BOARDER,
            TouchMode.RIGHT_BOARDER,
            TouchMode.DRAG_REGION,
    })
    public @interface TouchMode {
        int NONE = 0;
        int LEFT_BOARDER = 1;
        int RIGHT_BOARDER = 2;
        int DRAG_REGION = 3;

    }

    // To avoid object creation during onDraw save path locally.
    private Path mPath = new Path();
    // Each line has it's own paint.
    private Map<String, Paint> mPaints = new HashMap<>();

    private float mLeftCurrentXBoarderValue = 0;
    private float mRightCurrentXBoarderValue = ChartLayout.MAX_DISCRETE_PROGRESS;

    private float mStepXForMaxScale;

    // Animation for toggled line and other lines are different --> keep id of toggled line.
    @Nullable
    private String mLineToToggle = null;

    @Nullable
    private ChartData mChartData = null;

    private long mStartToggleTime;
    // Needs to animate chart when user toggle line/
    private long mLastMaxPossibleYever;

    @Nullable
    private Listener mListener;

    private float mMinPos = 0;
    private float mMaxPos = ChartLayout.MAX_DISCRETE_PROGRESS;

    private Paint mMaskPaint;
    private Paint mDragPaint;

    private int mDragZoneWidth;
    private int mDragBoarderHeight;
    // Draggable control width and touch zone could be different, guidelines says to use at least
    // 48dp for touch objects but as per design width is different.
    private int mDragZoneTouchWidth;

    @TouchMode
    private int mCurrentMode = TouchMode.NONE;
    private float mLastXPost = -1;

    public ChartControlView(Context context) {
        super(context);
        init();
    }

    public ChartControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mDragZoneWidth = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_width);
        mDragZoneTouchWidth = getContext().getResources().getDimensionPixelSize(R.dimen.drag_zone_touch_width);
        mDragBoarderHeight = getContext().getResources().getDimensionPixelSize(R.dimen.drag_border_height);

        mMaskPaint = new Paint();
        mMaskPaint.setColor(Color.GRAY);
        mMaskPaint.setAlpha(30);
        mMaskPaint.setStyle(Paint.Style.FILL);

        mDragPaint = new Paint();
        mDragPaint.setColor(Color.GRAY);
        mDragPaint.setAlpha(90);
        mDragPaint.setStyle(Paint.Style.FILL);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mCurrentMode = TouchMode.NONE;
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float left = (float) mMinPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
                    float leftLeft = left - (float) mDragZoneTouchWidth / 2;
                    float leftRight = left + (float) mDragZoneTouchWidth / 2;

                    float right = (float) mMaxPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
                    float rightLeft = right - (float) mDragZoneTouchWidth / 2;
                    float rightRight = right + (float) mDragZoneTouchWidth / 2;
                    if (event.getX() >= leftLeft && event.getX() <= leftRight) {
                        mCurrentMode = TouchMode.LEFT_BOARDER;
                    } else if (event.getX() >= rightLeft && event.getX() <= rightRight) {
                        mCurrentMode = TouchMode.RIGHT_BOARDER;
                    } else if (event.getX() >= leftLeft && event.getX() <= rightRight) {
                        mCurrentMode = TouchMode.DRAG_REGION;
                        mLastXPost = event.getX();
                    }
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float min = mMinPos;
                    float max = mMaxPos;
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

    public void setListener(@Nullable Listener listener) {
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
        if (mLineToToggle != null) {
            progress = Math.min((float) elapsed / TOGGLE_ANIM_DURATION, 1);
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
            float masPossibleYeverComputed =
                    mLastMaxPossibleYever + (maxPossibleYever - mLastMaxPossibleYever) * progress;

            mPath.reset();
            ChartData.YData yData = mChartData.getYValues().get(k);

            if (!yData.getVarName().equals(mLineToToggle)) {
                if (!yData.isShown()) {
                    continue;
                }
            } else {
                if (yData.isShown()) {
                    masPossibleYeverComputed = maxPossibleYever;
                } else {
                    masPossibleYeverComputed = mLastMaxPossibleYever;
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

        if (progress < 1 && (mLineToToggle != null)) {
            invalidate();
        } else {
            mLastMaxPossibleYever = maxPossibleYever;
            if (mLineToToggle != null) {
                mLineToToggle = null;
                invalidate();
            }
        }

        // Draw left portion of mask
        float leftRight = (float) mMinPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
        canvas.drawRect(0, 0, leftRight, getHeight(), mMaskPaint);
        // Draw left draggable field
        canvas.drawRect(leftRight, 0, leftRight + mDragZoneWidth, getHeight(), mDragPaint);
        // Draw top border
        float rightLeft = (float) mMaxPos / ChartLayout.MAX_DISCRETE_PROGRESS * getWidth();
        canvas.drawRect(leftRight + mDragZoneWidth, 0, rightLeft - mDragZoneWidth,
                mDragBoarderHeight, mDragPaint);
        // Draw bottom border
        canvas.drawRect(leftRight + mDragZoneWidth, getHeight() - mDragBoarderHeight,
                rightLeft - mDragZoneWidth, getHeight(), mDragPaint);
        // Draw right draggable field
        canvas.drawRect(rightLeft - mDragZoneWidth, 0, rightLeft, getHeight(), mDragPaint);
        // Draw right portion of mask
        canvas.drawRect(rightLeft, 0, getWidth(), getHeight(), mMaskPaint);
    }

    public void setChartData(@NonNull final ChartData data) {

        if (getWidth() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    setChartData(data);
                }
            });
            return;
        }

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

        mStepXForMaxScale = (float) getWidth() / (mChartData.getSize() - 1);
        mLeftCurrentXBoarderValue = 0;
        mRightCurrentXBoarderValue = getWidth();

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

    public interface Listener {
        void onBoarderChange(int left, int right);
    }
}
