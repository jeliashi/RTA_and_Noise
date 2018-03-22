package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 The base surface type for things to be displayed on
 */

public class SpectrumSurface extends SurfaceView {
    AtomicBoolean active = new AtomicBoolean(false);


    public SpectrumSurface(Context context){
        super(context);
        setInit();

    }

    public SpectrumSurface(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setInit();
    }

    private void setInit(){
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                synchronized (this){
                    active.set(true);
                    this.notifyAll();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                synchronized (this){
                    active.set(false);
                    this.notifyAll();
                }
            }


        });
    }

    public void drawSurface(DrawingInterface activeInterface){
        if (active.get()){
            getHolder().unlockCanvasAndPost(activeInterface.ifActive(getHolder().lockCanvas()));
        }
    }

    public static float getTextDpSize(float textSize){
        return (textSize / Resources.getSystem().getDisplayMetrics().density);
    }

    public static float getTextPxSize(float textSize){
        return (textSize * Resources.getSystem().getDisplayMetrics().density);
    }
}
