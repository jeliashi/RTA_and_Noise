package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioTools;
import com.jeliav.android.rtaandnoise.R;

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;

/**
simple display of the coherence between input and output
 */

public class CoherenceDisplaySurface extends SpectrumSurface implements DrawingInterface {

    public static final String LOG_TAG = CoherenceDisplaySurface.class.getSimpleName();

    private static final float PAINT_LABEL_SIZE = 8f;
    public static int resolution = 512;
    public static int minResolution = 64;

    public static Paint coherencePaint = new Paint();
    public static Paint labelPaint = new Paint();
    public static Paint errPaint = new Paint();

    private SpectrogramColors spectrogramColors = new SpectrogramColors();
    private ArrayDeque<float[]> coherenceHistory = new ArrayDeque<>();

    public static final SparseIntArray hotThreshold = new SparseIntArray();
    static{
        hotThreshold.put(512,2000);
        hotThreshold.put(256,2500);
        hotThreshold.put(128,3000);
        hotThreshold.put(64,4000);
    }

    public CoherenceDisplaySurface(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        setCoherencePaint(context, coherencePaint);
        setLabelPaint(context, labelPaint);
        setErrPaint(context, errPaint);
    }

    private void setCoherencePaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.specColor));
        paint.setStyle(Paint.Style.FILL);
    }

    private void setLabelPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_LABEL_SIZE));
        paint.setTypeface(Typeface.MONOSPACE);
    }

    private void setErrPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.errPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(12f));
    }

    private void drawTitle(Canvas canvas){
        canvas.drawText("Input/Output Coherence",
                getTextPxSize(16f),
                getTextPxSize(24f),
                labelPaint);
    }

    private void drawCoherence(Canvas canvas){
        float width = (float) this.getWidth();
        float height = (float) this.getHeight();
        if (coherenceHistory.size() > AudioTools.displaySamples) coherenceHistory.removeLast();

        float[] coherenceToPush;

        try {
            coherenceToPush =AudioTools.calculateCoherence(
                    mInput.getFFTStream().clone(),
                    mOutput.getFFTStream().clone());
        } catch (ConcurrentModificationException cme){
            cme.printStackTrace();
            return;
        }

        Log.d(LOG_TAG, String.valueOf(coherenceToPush[50]));

        coherenceHistory.push(coherenceToPush);
        float coherenceHeight = height / ((float) AudioTools.displaySamples);
        float sampleWidth = width / ((float) resolution);

        float x,y;
        // range should be from -1 to 1;
        int i = 0;
        if (coherenceHistory.size() <1) return;
        for (float[] coherenceMeasurement : coherenceHistory.clone()){
            y = height - (coherenceHeight * i);
            int freqPoints = coherenceMeasurement.length;

            for (int j = 0; j < resolution; j++){
                x = (sampleWidth*j);
                float magnitude = (freqPoints > 0) ? coherenceMeasurement[j*freqPoints/resolution]: 0f;
                int colorIndex = (int) (magnitude * spectrogramColors.range) % spectrogramColors.range;
                int[] RGB = spectrogramColors.color_map[colorIndex];
                coherencePaint.setColor(Color.rgb(RGB[0], RGB[1], RGB[2]));
                canvas.drawRect(x, y -coherenceHeight, x- sampleWidth, y, coherencePaint);
            }
            i++;
        }

    }


    public long averageDrawingTime(){
        long sum = 0;
        for (long l : whenToDraw) sum += l;
        return sum / whenToDraw.size();
    }

    private boolean canDrawCoherence(){ return (resolution >= minResolution);}

    @Override
    public Canvas ifActive(Canvas canvas) {
        int minDrawTime = 80;
        if (null == canvas) return null;
        canvas.drawColor(background);
        if (!canDrawCoherence()){
            //do stuff
            return canvas;
        }

        if (canDrawCoherence() &&
                whenToDraw.size() >= AudioTools.displaySamples &&
                avgDrawTime() > minDrawTime){
            whenToDraw = null;
            resolution /= 2;
            Log.w(LOG_TAG, "drawing too quickly, resolution now: "+String.valueOf(resolution));
        }

        long time0 = System.currentTimeMillis();
        drawCoherence(canvas);
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
