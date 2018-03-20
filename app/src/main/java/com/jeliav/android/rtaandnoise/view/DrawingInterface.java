package com.jeliav.android.rtaandnoise.view;

import android.graphics.Canvas;
import android.view.ViewPropertyAnimator;

/**
 * Created by jeliashiv on 3/9/18.
 */

public interface DrawingInterface {
    Canvas ifActive(Canvas canvas);
    void drawAll();
}
