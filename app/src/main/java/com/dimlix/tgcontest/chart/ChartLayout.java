package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

import java.util.ArrayList;
import java.util.List;

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
                mChartView.setMaxVisibleRegionPercent(left, right);
            }

            @Override
            public void onViewTouched() {
                if (mListener != null) {
                    mListener.onInnerViewTouched();
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
        });
    }

    public void setData(ChartData data) {
        mData = data;
        mChartView.setChartData(data);
        mChartControlView.setChartData(data);

        int min = 0;
        int max = MAX_DISCRETE_PROGRESS;
        mChartView.setMaxVisibleRegionPercent(min, max);

        mChartControlView.setMinMax(min, max);

        for (CheckBox checkBox : mCheckBoxes) {
            checkBox.setOnCheckedChangeListener(null);
            removeView(checkBox);
        }

        int states[][] = {{android.R.attr.state_checked}, {}};

        LayoutInflater inflater = LayoutInflater.from(getContext());

        int count = 0;
        for (ChartData.YData yData : data.getYValues()) {
            CheckBox checkbox = (CheckBox) inflater.inflate(R.layout.chart_checkbox, null);
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
    }
}
