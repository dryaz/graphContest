package com.dimlix.tgcontest.chart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.dimlix.tgcontest.R;
import com.dimlix.tgcontest.model.ChartData;

public class ChartLayout extends LinearLayout {

    public static int MAX_DISCRETE_PROGRESS = 10000;

    private ChartView mChartView;
    // TODO implement chartControlViewHere
    private ChartView mChartControlView;

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
    }

    public void setData(ChartData data) {
        mChartView.setChartData(data);
        mChartControlView.setChartData(data);

        mChartView.setMaxVisibleRegionPercent(0, MAX_DISCRETE_PROGRESS / 2);
        mChartControlView.setMaxVisibleRegionPercent(0, MAX_DISCRETE_PROGRESS);
    }
}
