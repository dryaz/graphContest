package com.dimlix.tgcontest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    private int curProgress = 1;
    private final int MAXP = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View seekBar = findViewById(R.id.seekXMax);
        final GraphView graphView = findViewById(R.id.graph);
        graphView.setMaxVisibleRegion(curProgress);

        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int xValue = (int) (event.getX() * ((float) MAXP / graphView.getWidth()));
                graphView.setMaxVisibleRegion(xValue);
                Log.e("!@#", "xVal " + xValue);
                return true;
            }
        });
    }
}
