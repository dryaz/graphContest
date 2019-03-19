package com.dimlix.tgcontest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.dimlix.tgcontest.chart.ChartLayout;
import com.dimlix.tgcontest.model.GraphData;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private boolean mIsLightTheme = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsLightTheme = savedInstanceState.getBoolean("THEME", true);
        }
        Log.e("!@#", " " + mIsLightTheme );
        if (mIsLightTheme) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppThemeDark);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_theme_toggle, menu);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle:
                if (mIsLightTheme) {
                    mIsLightTheme = false;
                    recreate();
                } else {
                    mIsLightTheme = true;
                    recreate();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("THEME", mIsLightTheme);
        super.onSaveInstanceState(outState);
    }

    public String loadGraphJSONFromAsset() {
        String json;
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
