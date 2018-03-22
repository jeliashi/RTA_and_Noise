package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioTools;
import com.jeliav.android.rtaandnoise.R;

/**
 Display surface of the phase coherence
 */

public class PhaseDisplaySurface extends SpectrumSurface implements DrawingInterface {

    // TODO fix the phase value display. Go through the entire step from fft to make sure everything is [-pi,pi)


    public static final String LOG_TAG = PhaseDisplaySurface.class.getSimpleName();
    public static int background;

    public static Paint linePaint = new Paint();
    public static Paint labelPaint = new Paint();

    public PhaseDisplaySurface(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        background = context.getResources().getColor(R.color.background);
        setLinePaint(context, linePaint);
        setLabelPaint(context, labelPaint);
    }


    private void setLinePaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.white));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    private void setLabelPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(12f));
        paint.setTypeface(Typeface.MONOSPACE);
    }

    private void drawPhase(Canvas canvas){
        float width = (float) this.getWidth();
        float height = (float) this.getHeight();

        int freqPoints = AudioTools.finalDist.length;

        float[] inMagnitude = mInput.getFFTStream().clone().peekFirst();
        float[] inPhase = mInput.getFFTPhaseStream().clone().peekFirst();
        float[] outMagnitude = mOutput.getFFTStream().clone().peekFirst();
        float[] outPhase = mOutput.getFftPhaseStream().clone().peekFirst();

        float[] displayPhase = AudioTools.caculatePhaseFromFFT(
                inMagnitude,
                inPhase,
                outMagnitude,
                outPhase
        );
        float x=0, y = height/2f;
        float dX = width / (float) freqPoints;

        for (int j=0 ; j < freqPoints; j++){
            float newY = (1- displayPhase[j]) * height/(2*(float) Math.PI);
            if (Math.abs(newY - y) < Math.PI/2) {
                canvas.drawLine(x,y,x+dX, newY, linePaint);
            } else{
                canvas.drawPoint(x+dX, newY, linePaint);
            }
            x+= dX;
            y = newY;
        }
    }

    private void drawTitle(Canvas canvas){
        canvas.drawText("Output phase discrepancy",
                getTextPxSize(16f),
                getTextPxSize(24f),
                labelPaint);
    }


    @Override
    public Canvas ifActive(Canvas canvas) {
        int minimumDrawTimeThreshold = 80;
        if (null == canvas) return null;
        canvas.drawColor(background);
        if (whenToDraw.size() >= AudioTools.displaySamples &&
                avgDrawTime() > minimumDrawTimeThreshold) {
            Log.w(LOG_TAG, "drawing too quickly");
            whenToDraw = null;
            try {
                wait((long) 200);
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }

        long time0 = System.currentTimeMillis();
        drawPhase(canvas);
        long time1 = System.currentTimeMillis();
        drawTitle(canvas);
        long time2 = System.currentTimeMillis();
        whenToDraw.add(time1-time0);
        whenToDraw.add(time2-time1);

        while (whenToDraw.size() > AudioTools.displaySamples){
            whenToDraw.remove(0);
        }
        return canvas;
    }

    @Override
    public void drawAll() {
        drawSurface(this);

    }
}
