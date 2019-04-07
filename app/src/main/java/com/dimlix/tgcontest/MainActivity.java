package com.dimlix.tgcontest;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;

import com.dimlix.tgcontest.chart.ChartLayout;
import com.dimlix.tgcontest.model.GraphData;

import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    public static final String THEME = "THEME";
    private boolean mIsLightTheme = true;

    private ScrollView mScrollView;
    private ViewGroup mContainer;
    private Toolbar mToolbar;
    private MenuItem mToggleMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsLightTheme = savedInstanceState.getBoolean(THEME, true);
        }
        if (mIsLightTheme) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppThemeDark);
        }
        setContentView(R.layout.activity_main);

        mScrollView = findViewById(R.id.scroll);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // TODO persist data across activity recreations, make graphData parcable
        GraphData graphData = new JsonGraphReader().getGraphDataFromJson(loadGraphJSONFromAsset());
        LayoutInflater inflater = LayoutInflater.from(this);
        mContainer = findViewById(R.id.container);
        for (int i = 0; i < graphData.getChartData().size(); i++) {
            ChartLayout chartView = (ChartLayout) inflater.inflate(R.layout.item_chart, mContainer, false);
            // Need to set ID to restore state (without ID will be the same state for all views)
            chartView.setId(i);
            chartView.setListener(new ChartLayout.Listener() {
                @Override
                public void onInnerViewTouched() {
                    mScrollView.requestDisallowInterceptTouchEvent(true);
                }

                @Override
                public void onInnerViewReleased() {
                    mScrollView.requestDisallowInterceptTouchEvent(false);
                }
            });
            chartView.setData(graphData.getChartData().get(i));
            mContainer.addView(chartView);
        }

        setSystemBarTheme(false);
    }

    public final void setSystemBarTheme(final boolean pIsDark) {
        // Fetch the current flags.
        final int lFlags = getWindow().getDecorView().getSystemUiVisibility();
        // Update the SystemUiVisibility dependening on whether we want a Light or Dark theme.
        getWindow().getDecorView().setSystemUiVisibility(pIsDark ? (lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (lFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
        if (pIsDark) {
            mToolbar.setTitleTextColor(Color.WHITE);
        } else {
            mToolbar.setTitleTextColor(Color.BLACK);
        }
        if (mToggleMenu != null) {
            if (pIsDark) {
                mToggleMenu.setIcon(R.drawable.ic_moon_white);
            } else {
                mToggleMenu.setIcon(R.drawable.ic_moon_gray);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_theme_toggle, menu);
        mToggleMenu = menu.findItem(R.id.action_toggle);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle:
                if (mIsLightTheme) {
                    mIsLightTheme = false;
                    setTheme(R.style.AppThemeDark);
                    setSystemBarTheme(true);
                } else {
                    mIsLightTheme = true;
                    setTheme(R.style.AppThemeLight);
                    setSystemBarTheme(false);
                }
                break;
        }
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            View child = mContainer.getChildAt(i);
            if (child instanceof ChartLayout) {
                ((ChartLayout) child).reInit();
            }
        }
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        getWindow().getDecorView().setBackgroundColor(typedValue.data);
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(typedValue.data));
        theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(typedValue.data);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(THEME, mIsLightTheme);
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
