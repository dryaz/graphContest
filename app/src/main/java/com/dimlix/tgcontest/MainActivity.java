package com.dimlix.tgcontest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    private int curProgress = 1;
    private final int MAXP = 1000;

    private int mLeftBoarder = 0;
    private int mRightBoarder = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SeekBar seekBarMax = findViewById(R.id.seekXMax);
        SeekBar seekBarMin = findViewById(R.id.seekXMin);
        final GraphView graphView = findViewById(R.id.graph);
        graphView.setMaxVisibleRegionPercent(0, mRightBoarder);
        seekBarMax.setProgress(mRightBoarder);
        seekBarMin.setProgress(mLeftBoarder);

        seekBarMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRightBoarder = progress;
                graphView.setMaxVisibleRegionPercent(mLeftBoarder, mRightBoarder);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLeftBoarder = progress;
                graphView.setMaxVisibleRegionPercent(mLeftBoarder, mRightBoarder);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
