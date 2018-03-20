package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jeliashiv on 3/19/18.
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
                active.set(true);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                active.set(false);

            }
        });
    }


    public void drawSurface(DrawingInterface activeInterface){
        if (active.get()){
            getHolder().unlockCanvasAndPost(activeInterface.ifActive(getHolder().lockCanvas()));
        }
    }
}
