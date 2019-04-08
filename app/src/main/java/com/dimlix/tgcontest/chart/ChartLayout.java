package com.dimlix.tgcontest.chart;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic layout that contains {@link ChartView} that displays chart
 * and {@link ChartControlView} that control behaviour.
 */
public class ChartLayout extends LinearLayout implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {

    public static int MAX_DISCRETE_PROGRESS = 10000;

    private ChartView mChartView;
    private ChartControlView mChartControlView;

    private TextView mChartTitle;
    private TextView mChartSelectedRange;

    private ChartData mData;

    private Listener mListener;

    private int mLeftBoarder = 0;
    private int mRightBoarder = MAX_DISCRETE_PROGRESS;
    private Map<String, Chip> mChartCheckboxes;
    private Set<String> mDisbledCharts;

    private int mSideMargin;

    private ViewGroup mChipGroup;

    private int mChipHeight;

    public ChartLayout(Context context) {
        super(context);
        init(context);
    }

    public ChartLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.layout_chart, this);
        setOrientation(VERTICAL);

        mSideMargin = getContext().getResources().getDimensionPixelSize(R.dimen.side_margin);
        mChipHeight = context.getResources().getDimensionPixelSize(R.dimen.chip_height);
        mChartView = view.findViewById(R.id.chart);
        mChartControlView = view.findViewById(R.id.chartControl);
        mChipGroup = view.findViewById(R.id.groupLinesControls);

        mChartTitle = view.findViewById(R.id.tvChartTitle);
        mChartSelectedRange = view.findViewById(R.id.tvChartDate);

        mChartControlView.setListener(new ChartControlView.Listener() {
            @Override
            public void onBoarderChange(int left, int right) {
                setRegion(left, right);
            }

            @Override
            public void onViewTouched() {
                if (mListener != null) {
                    mListener.onInnerViewTouched();
                }
            }

            @Override
            public void onViewReleased() {
                if (mListener != null) {
                    mListener.onInnerViewReleased();
                }
            }
        });

        mChartView.setListener(new ChartView.Listener() {
            @Override
            public void onViewTouched() {
                if (mListener != null) {
                    mListener.onInnerViewTouched();
                }
            }

            @Override
            public void onViewReleased() {
                if (mListener != null) {
                    mListener.onInnerViewReleased();
                }
            }
        });
    }

    float leftPercentagePos;
    float rightPercentagePos;
    int firstPointToShow;
    int lastPointToShow;

    private void setRegion(int left, int right) {
        mLeftBoarder = left;
        mRightBoarder = right;
        mChartView.setMaxVisibleRegionPercent(mLeftBoarder, mRightBoarder);


        leftPercentagePos = (float) left / ChartLayout.MAX_DISCRETE_PROGRESS;
        rightPercentagePos = (float) right / ChartLayout.MAX_DISCRETE_PROGRESS;

        firstPointToShow = (int) (leftPercentagePos * (mData.getSize() - 1));
        lastPointToShow = (int) (rightPercentagePos * (mData.getSize() - 1));

        mChartSelectedRange.setText(getContext().getString(R.string.date_range,
                mData.getXStringValues().get(firstPointToShow).getFullDate(),
                mData.getXStringValues().get(lastPointToShow).getFullDate()));
    }

    public void setData(ChartData data) {
        mData = data;
        mChartView.setChartData(data);
        mChartControlView.setChartData(data);

        mChartSelectedRange.setText(getContext().getString(R.string.date_range,
                mData.getXStringValues().get(0).getFullDate(),
                mData.getXStringValues().get(mData.getSize() - 1).getFullDate()));

        int min = mLeftBoarder;
        int max = mRightBoarder;
        mChartView.setMaxVisibleRegionPercent(min, max);

        mChartControlView.setMinMax(min, max);

        mChartCheckboxes = new HashMap<>(data.getYValues().size());
        mDisbledCharts = new HashSet<>(data.getYValues().size());

        int states[][] = {{android.R.attr.state_selected}, {}};

        ChipGroup.LayoutParams params = new ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                mChipHeight);

        ShapeAppearanceModel shapeModel = new ShapeAppearanceModel();
        shapeModel.setAllCorners(CornerFamily.ROUNDED, Integer.MAX_VALUE);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.bgChartColor, typedValue, true);

        for (ChartData.YData yData : data.getYValues()) {

            int colorsSolid[] = {Color.parseColor(yData.getColor()), typedValue.data};
            int colorsStroke[] = {Color.TRANSPARENT, Color.parseColor(yData.getColor())};
            int colorsText[] = {Color.WHITE, Color.parseColor(yData.getColor())};

            Chip checkbox = new Chip(getContext());
            checkbox.setChipStartPaddingResource(R.dimen.side_margin);
            checkbox.setChipEndPaddingResource(R.dimen.side_margin);
            checkbox.setLayoutParams(params);
            checkbox.setCheckable(true);
            checkbox.setClickable(true);
            checkbox.setFocusable(true);
            checkbox.setTypeface(null, Typeface.BOLD);
            checkbox.setShapeAppearanceModel(shapeModel);
            checkbox.setTextColor(new ColorStateList(states, colorsText));
            checkbox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkbox.setChipStrokeWidth(4);
            checkbox.setChipStrokeColor(new ColorStateList(states, colorsStroke));
            checkbox.setCheckedIconResource(R.drawable.ic_done_white_24dp);
            mChartCheckboxes.put(yData.getVarName(), checkbox);
            checkbox.setChipBackgroundColor(new ColorStateList(states, colorsSolid));
            checkbox.setText(yData.getAlias());
            checkbox.setChecked(true);
            checkbox.setTag(yData.getVarName());
            checkbox.setOnCheckedChangeListener(this);

            checkbox.setOnLongClickListener(this);

            mChipGroup.addView(checkbox);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String yVarName = (String) buttonView.getTag();
        for (ChartData.YData yData : mData.getYValues()) {
            if (yData.getVarName().equals(yVarName)) {
                if (!isChecked) {
                    if (mDisbledCharts.size() + 1 == mChartCheckboxes.size()) {
                        buttonView.setChecked(true);
                        ObjectAnimator
                                .ofFloat(buttonView, "translationX", 0, 8, -8, 5, -5, 2, -2, 0)
                                .setDuration(300)
                                .start();
                        return;
                    } else {
                        mDisbledCharts.add(yData.getVarName());
                    }
                } else {
                    mDisbledCharts.remove(yData.getVarName());
                }
                yData.setShown(isChecked);
            }
        }
        mChartView.onYChartToggled(yVarName);
        mChartControlView.onYChartToggled(yVarName);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean onLongClick(View v) {
        ((CompoundButton) v).setChecked(true);
        for (CompoundButton box : mChartCheckboxes.values()) {
            box.setChecked(v == box);
        }
        return true;
    }

    public interface Listener {
        void onInnerViewTouched();

        void onInnerViewReleased();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("left", mLeftBoarder);
        bundle.putInt("right", mRightBoarder);
        String[] disableCharts = new String[mDisbledCharts.size()];
        mDisbledCharts.toArray(disableCharts);
        bundle.putStringArray("disableCharts", disableCharts);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) // implicit null check
        {
            setAnimationEnabled(false);
            Bundle bundle = (Bundle) state;
            mChartControlView.setMinMax(bundle.getInt("left"), bundle.getInt("right"));
            for (String yVarName : bundle.getStringArray("disableCharts")) {
                mChartCheckboxes.get(yVarName).setChecked(false);
            }
            mChartControlView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAnimationEnabled(true);
                }
            }, 100);
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    private void setAnimationEnabled(boolean isEnabled) {
        mChartControlView.setAnimationsEnabled(isEnabled);
        mChartView.setAnimationsEnabled(isEnabled);
    }

    public void reInit() {
        mChartView.reInit();
        mChartControlView.reInit();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.bgChartColor, typedValue, true);
        setBackgroundColor(typedValue.data);

        int states[][] = {{android.R.attr.state_selected}, {}};
        for (ChartData.YData yData : mData.getYValues()) {
            int colorsSolid[] = {Color.parseColor(yData.getColor()), typedValue.data};
            mChartCheckboxes.get(yData.getVarName()).setChipBackgroundColor(new ColorStateList(states, colorsSolid));
        }

        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        mChartTitle.setTextColor(typedValue.data);
        mChartSelectedRange.setTextColor(typedValue.data);
    }
}
