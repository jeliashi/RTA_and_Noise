package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioCollectTest;
import com.jeliav.android.rtaandnoise.AudioUtilities.Generator;
import com.jeliav.android.rtaandnoise.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 The base surface type for things to be displayed on
 */

public class SpectrumSurface extends SurfaceView {
    AtomicBoolean active = new AtomicBoolean(false);

    static int background;
    public List<Long> whenToDraw = new ArrayList<>();
    public Pair<Long, Integer> lastPastTimeAvg = new Pair<>(System.currentTimeMillis(), 0);
    AudioCollectTest mInput;
    Generator mOutput;


    public SpectrumSurface(Context context){
        super(context);
        background = context.getResources().getColor(R.color.background);
        setInit();

    }

    public SpectrumSurface(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        background = context.getResources().getColor(R.color.background);
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

    public void setInputSource(AudioCollectTest input){ mInput = input; }
    public void setOutputSource(Generator output){mOutput = output;}

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

    int avgDrawTime(){
        if (System.currentTimeMillis() - lastPastTimeAvg.first > 1000){
            lastPastTimeAvg = new Pair<>(System.currentTimeMillis(),
                    (whenToDraw.size() > 0) ? DrawAverage(whenToDraw) : 0);
        }
        return lastPastTimeAvg.second;
    }

    int DrawAverage(List<Long> mArray){
        int sum = 0;
        for (long l : mArray){
            sum+= (int) l;
        }
        return sum / mArray.size();
    }

}
