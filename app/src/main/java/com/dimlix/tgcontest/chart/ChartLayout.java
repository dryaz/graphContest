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

public class ChartLayout extends LinearLayout implements CompoundButton.OnCheckedChangeListener {

    public static int MAX_DISCRETE_PROGRESS = 10000;

    private ChartView mChartView;
    private ChartControlView mChartControlView;

    private List<CheckBox> mCheckBoxes = new ArrayList<>();
    private ChartData mData;

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
        });
    }

    public void setData(ChartData data) {
        mData = data;
        // TODO reset view after data is set?
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

        for (ChartData.YData yData : data.getYValues()) {
            CheckBox checkbox = (CheckBox) inflater.inflate(R.layout.chart_checkbox, null);
            int colors[] = {Color.parseColor(yData.getColor()), Color.BLACK};
            checkbox.setButtonTintList(new ColorStateList(states, colors));
            checkbox.setText(yData.getAlias());
            checkbox.setChecked(true);
            // TODO add delimiter
            checkbox.setTag(yData.getVarName());
            checkbox.setOnCheckedChangeListener(this);

            addView(checkbox);
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
}
