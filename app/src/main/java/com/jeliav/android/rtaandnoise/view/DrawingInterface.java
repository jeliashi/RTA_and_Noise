package com.jeliav.android.rtaandnoise.view;

import android.graphics.Canvas;

/**
 Interface to allow drawing surfaces to interact with their parent simple surface
 */

public interface DrawingInterface {
    Canvas ifActive(Canvas canvas);
    void drawAll();
}
