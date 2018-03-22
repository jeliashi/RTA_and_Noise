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

import java.util.ArrayDeque;


/**
Shows the frequency dependent difference in normalized power of input and output
 */

public class TransferDisplaySurface extends SpectrumSurface implements DrawingInterface {

    public static final String LOG_TAG = TransferDisplaySurface.class.getSimpleName();

    private static final float PAINT_TEXT_SIZE = 12f;
    private static final float PAINT_LABEL_SIZE = 8f;

    public static Paint linePaint = new Paint();
    public static Paint textPaint = new Paint();
    public static Paint labelPaint = new Paint();

    public TransferDisplaySurface(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        setLinePaint(context, linePaint);
        setTextPaint(context, textPaint);
        setLabelPaint(context, labelPaint);
    }

    private void drawTransfer(Canvas canvas){
        float width = (float) this.getWidth();
        float height= (float) this.getHeight();
        ArrayDeque<float[]> inFFTDeque = mInput.getFFTStream().clone();
        ArrayDeque<float[]> outFFTDeque = mOutput.getFFTStream().clone();

        int freqPoints = AudioTools.finalDist.length;
        float x =0,y = height/2f;
        float dX = width / (float) freqPoints;

        double[] inFFT = new double[freqPoints];
        double[] outFFT = new double[freqPoints];

//        Log.d(LOG_TAG, "polling");
        for (int j=0; j < AudioTools.displaySamples; j++){
            float[] inElement = inFFTDeque.poll();
            float[] outElement = outFFTDeque.poll();
//            if (inElement == null || outElement == null) Log.e(LOG_TAG, "null element polled");
            for (int i = 0; i < freqPoints ; i++){
//                if (inElement[i] == null || outElement[i] == null) Log.e(LOG_TAG, "nullEleement from deque");
//                if (inFFT[i] == null || outFFT[i] == null) Log.e(LOG_TAG, "nullElement from doubles");
                try {
                    inFFT[i] += inElement[i];
                    outFFT[i] += outElement[i];
                } catch (NullPointerException npe){
                    return;
                }
            }

        }
//        Log.d(LOG_TAG, "integrated");
        double inFFTintegrated = 0;
        double outFFTintegrated = 0;

        for (int i = 0; i < freqPoints ; i++){
            inFFTintegrated += inFFT[i];
            outFFTintegrated += outFFT[i];
        }
//        Log.d(LOG_TAG, String.format("in integral = %f, out integral = %f", (float) inFFTintegrated, (float) outFFTintegrated));
        if (inFFTintegrated < 1){
            inFFTintegrated = 1;
        }
        if (outFFTintegrated < 1){
            outFFTintegrated = 1;
        }

        for (int j =0 ; j < freqPoints; j++){
            float val = (float) (((float) freqPoints)*(outFFT[j] / outFFTintegrated - inFFT[j] / inFFTintegrated))/ 50f;
            float newY = (1 - (val) )*(height/2);
            canvas.drawLine(x,y,x+dX, newY, linePaint );
            x+= dX;
            y = newY;
        }
    }

    private void drawTitle(Canvas canvas){
        canvas.drawText("Transfer wave",
                getTextPxSize(16f),
                getTextPxSize(24f),
                textPaint);
    }

    private void setLinePaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }
    private void setTextPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.colorPrimary));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_TEXT_SIZE));
        paint.setTypeface(Typeface.MONOSPACE);
    }
    private void setLabelPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_LABEL_SIZE));
        paint.setTypeface(Typeface.MONOSPACE);
    }

    @Override
    public Canvas ifActive(Canvas canvas) {
        int minimimDrawTimeThreshold = 80;
        if (null == canvas) return null;
        canvas.drawColor(background);
        if (whenToDraw.size() >= AudioTools.displaySamples &&
                avgDrawTime() > minimimDrawTimeThreshold) {
            Log.w(LOG_TAG, "drawing too quickly");
            whenToDraw = null;

            try {
                wait((long) 200);
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }

//        if (!mOutput.mShouldContinue){
//            mOutput.fftStream.removeLast();
//            mOutput.fftStream.push(new float[AudioTools.finalDist.length]);
//        }

        long time0 = System.currentTimeMillis();
        drawTransfer(canvas);
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
