package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic layout that contains {@link ChartView} that displays chart
 * and {@link ChartControlView} that control behaviour.
 */
public class ChartLayout extends LinearLayout implements CompoundButton.OnCheckedChangeListener {

    public static int MAX_DISCRETE_PROGRESS = 10000;

    private ChartView mChartView;
    private ChartControlView mChartControlView;

    private List<CheckBox> mCheckBoxes = new ArrayList<>();
    private ChartData mData;

    private Listener mListener;

    private int mLeftBoarder = 0;
    private int mRightBoarder = MAX_DISCRETE_PROGRESS;
    private Map<String, CompoundButton> mChartCheckboxes;
    private Set<String> mDisbledCharts;

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
        mChartView = view.findViewById(R.id.chart);
        mChartControlView = view.findViewById(R.id.chartControl);

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

    private void setRegion(int left, int right) {
        mLeftBoarder = left;
        mRightBoarder = right;
        mChartView.setMaxVisibleRegionPercent(mLeftBoarder, mRightBoarder);
    }

    public void setData(ChartData data) {
        mData = data;
        mChartView.setChartData(data);
        mChartControlView.setChartData(data);

        int min = mLeftBoarder;
        int max = mRightBoarder;
        mChartView.setMaxVisibleRegionPercent(min, max);

        mChartControlView.setMinMax(min, max);

        mChartCheckboxes = new HashMap<>(data.getYValues().size());
        mDisbledCharts = new HashSet<>(data.getYValues().size());

        for (CheckBox checkBox : mCheckBoxes) {
            checkBox.setOnCheckedChangeListener(null);
            removeView(checkBox);
        }

        int states[][] = {{android.R.attr.state_checked}, {}};

        LayoutInflater inflater = LayoutInflater.from(getContext());

        int count = 0;
        for (ChartData.YData yData : data.getYValues()) {
            CheckBox checkbox = (CheckBox) inflater.inflate(R.layout.chart_checkbox, null);
            mChartCheckboxes.put(yData.getVarName(), checkbox);
            int colors[] = {Color.parseColor(yData.getColor()), Color.BLACK};
            checkbox.setButtonTintList(new ColorStateList(states, colors));
            checkbox.setText(yData.getAlias());
            checkbox.setChecked(true);
            checkbox.setTag(yData.getVarName());
            checkbox.setOnCheckedChangeListener(this);
            addView(checkbox);
            if (count++ < data.getYValues().size() - 1) {
                View deli = inflater.inflate(R.layout.chart_checkbox_delimeter, this, false);
                addView(deli);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String yVarName = (String) buttonView.getTag();
        for (ChartData.YData yData : mData.getYValues()) {
            if (yData.getVarName().equals(yVarName)) {
                yData.setShown(isChecked);
                if (!isChecked) {
                    mDisbledCharts.add(yData.getVarName());
                } else {
                    mDisbledCharts.remove(yData.getVarName());
                }
            }
        }
        mChartView.onYChartToggled(yVarName);
        mChartControlView.onYChartToggled(yVarName);
    }

    public void setListener(Listener listener) {
        mListener = listener;
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
}
