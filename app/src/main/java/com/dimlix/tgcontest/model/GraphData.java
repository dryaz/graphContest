package com.dimlix.tgcontest.model;

import java.util.ArrayList;
import java.util.List;

public class GraphData {
    private List<ChartData> mChartData = new ArrayList<>();

    public List<ChartData> getChartData() {
        return mChartData;
    }


    public void addChartData(ChartData data) {
        mChartData.add(data);
    }

    @Override
    public String toString() {
        return "GraphData{" +
                "mChartData=" + mChartData +
                '}';
    }
}
