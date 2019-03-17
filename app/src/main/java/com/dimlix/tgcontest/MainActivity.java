package com.dimlix.tgcontest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;

import com.dimlix.tgcontest.chart.ChartLayout;
import com.dimlix.tgcontest.chart.ChartView;
import com.dimlix.tgcontest.model.GraphData;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ChartLayout chartView = findViewById(R.id.chartLayout);

        GraphData graphData = new JsonGraphReader().getGraphDataFromJson(loadGraphJSONFromAsset());
        chartView.setData(graphData.getChartData().get(0));
    }

    public String loadGraphJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("chart_data_test.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
