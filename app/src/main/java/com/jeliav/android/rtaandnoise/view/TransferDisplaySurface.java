package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioCollectTest;
import com.jeliav.android.rtaandnoise.AudioUtilities.AudioTools;
import com.jeliav.android.rtaandnoise.AudioUtilities.Generator;
import com.jeliav.android.rtaandnoise.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jeliashiv on 3/21/18.
 */

public class TransferDisplaySurface extends SpectrumSurface implements DrawingInterface {

    public static final String LOG_TAG = TransferDisplaySurface.class.getSimpleName();

    private static final float PAINT_TEXT_SIZE = 12f;
    private static final float PAINT_LABEL_SIZE = 8f;
    public static int background;
    private int fps = 80;

    public static Paint linePaint = new Paint();
    public static Paint textPaint = new Paint();
    public static Paint labelPaint = new Paint();

    private AudioCollectTest mInput;
    private Generator mOutput;

    private List<Long> whenToDraw = new ArrayList<>();
    public Pair<Long, Integer> lastPastTimeAvg = new Pair<>(System.currentTimeMillis(), 0);

    public TransferDisplaySurface(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        background = context.getResources().getColor(R.color.background);
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
                    inFFT[i] += 0.;
                    outFFT[i] += 0.;
                    npe.printStackTrace();
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

    public void setInputSource(AudioCollectTest input){
        mInput = input;
    }
    public void setOutputSource(Generator output){
        mOutput = output;
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

    private int avgDrawTime(){
        if (System.currentTimeMillis() - lastPastTimeAvg.first > 1000){
            lastPastTimeAvg = new Pair<>(System.currentTimeMillis(),
                    (whenToDraw.size() > 0) ? DrawAverage(whenToDraw) : 0);
        }
        return lastPastTimeAvg.second;
    }

    private int DrawAverage(List<Long> mArray){
        int sum = 0;
        for (long l : mArray){
            sum+= (int) l;
        }
        return sum / mArray.size();
    }

    @Override
    public Canvas ifActive(Canvas canvas) {
        if (null == canvas) return canvas;
        canvas.drawColor(background);
        if (whenToDraw.size() >= AudioTools.displaySamples &&
                avgDrawTime() > fps) {
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
