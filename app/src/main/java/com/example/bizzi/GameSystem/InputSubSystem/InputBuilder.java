package com.example.bizzi.GameSystem.InputSubSystem;

import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

import com.example.bizzi.AlbertBirthActivity.MainActivity;
import com.example.bizzi.GameSystem.Utility.Builder;

public final class InputBuilder implements Builder {

    private AccelerometerListener accelerometerListener;

    private TouchListener touchListener;


    private final MainActivity mainActivity;

    public final GameInput gameInput;

    public InputBuilder(MainActivity activity) {
        mainActivity =activity;

        gameInput = new GameInput();
    }

    @Override
    public void build() {
        //create and register Accelerometer Listener
        accelerometerListener = new AccelerometerListener(gameInput);
        mainActivity.setAccelerometerListener(accelerometerListener);


        //create and register Touch Listener
        WindowManager windowManager = (WindowManager) mainActivity.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getRealSize(point);
        }
        touchListener = new TouchListener(gameInput, point);
        mainActivity.setTouchListener(touchListener);
    }
}