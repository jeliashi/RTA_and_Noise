package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Created by jeliashiv on 3/9/18.
 */

abstract class BaseSurface extends SurfaceView implements IfActiveInterface {
    public AtomicBoolean active = new AtomicBoolean(false);

    public BaseSurface(Context context){
        super(context);
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

    public BaseSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
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


    public void drawSurface(IfActiveInterface iaf){
        if (this.active.get()){
            SurfaceHolder  holder = this.getHolder();
            Canvas oldCanvas = holder.lockCanvas();
            holder.unlockCanvasAndPost(iaf.ifActive(oldCanvas));
        }
    }
}
