package com.dimlix.tgcontest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dimlix.tgcontest.chart.ChartLayout;
import com.dimlix.tgcontest.model.GraphData;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GraphData graphData = new JsonGraphReader().getGraphDataFromJson(loadGraphJSONFromAsset());
        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup container = findViewById(R.id.container);
        for (int i = 0; i < 1/*graphData.getChartData().size() */; i++) {
            ChartLayout chartView = (ChartLayout) inflater.inflate(R.layout.item_chart, container, false);
            chartView.setData(graphData.getChartData().get(i));
            container.addView(chartView);
        }
    }

    public String loadGraphJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("chart_data.json");
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
