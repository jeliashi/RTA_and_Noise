package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import com.jeliav.android.rtaandnoise.AudioTools.AudioCollectTest;
import com.jeliav.android.rtaandnoise.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jeliashiv on 3/19/18.
 */

public class FFTSpectrumSurface extends SpectrumSurface implements DrawingInterface {

    public static final String LOG_TAG = FFTSpectrumSurface.class.getSimpleName();

    private static final float PAINT_TEXT_SIZE = 12f;
    public static int background;
    public static int resolution =  512;
    public static int minResolution = 64;
    private int fps = 80;

    public static Paint paintSpectogram = new Paint();
    public static Paint textPaint = new Paint();
    public static Paint errPaint = new Paint();

    private AudioCollectTest mAudio;

    private SpectogramColors spectogramColors = new SpectogramColors();

    public static final SparseIntArray hotThreshold = new SparseIntArray();
    static{
        hotThreshold.put(512,15000);
        hotThreshold.put(256,15000);
        hotThreshold.put(128,30000);
        hotThreshold.put(64,45000);
    }

    private List<Long> whenToDraw =  new ArrayList<Long>();

    public static Pair<Long, String> message;
    public Pair<Long, Integer> lastPaintTimeAvg = new Pair<>(System.currentTimeMillis(), 0);

    public FFTSpectrumSurface(Context context, @Nullable  AttributeSet attributeSet) {
        super(context, attributeSet);
        background = context.getResources().getColor(R.color.background);
        setSpecPaint(context, paintSpectogram);
        setTextPaint(context, textPaint);
        setErrPaint(context, errPaint);
    }

    public void setAudioSource(AudioCollectTest audioCollectTest){
        mAudio = audioCollectTest;
    }

    private void drawTitle(Canvas canvas){
        canvas.drawText("FFT Heat Map",
                getTextPxSize(16f),
                getTextPxSize(24f),
                textPaint);
    }

    private void drawLegend(Canvas canvas) {
        float height = this.getHeight();
        for (int i = 0 ; i < height ; i++ ){
            double heightPosNorm = ((double) i)/((double) height);
            int specLegendIndex = ((int) (256f * (1 - heightPosNorm)))%256;
            int[] legendColors = spectogramColors.color_map[specLegendIndex];
            int color = Color.rgb(legendColors[0],
                    legendColors[1],
                    legendColors[2]);

            paintSpectogram.setColor(color);
            canvas.drawRect(0f,
                    (float) specLegendIndex,
                    10f,
                    (float) specLegendIndex + 1f,
                    paintSpectogram);
        }
    }

    private void drawSpectrum(Canvas canvas){
        float width = (float) this.getWidth();
        float height = (float) this.getHeight();
        ArrayDeque<float[]> fftHistory = mAudio.getFFTStream().clone();
        float spectrumHeight = height  /((float)AudioCollectTest.displaySamples);
        float sampleWidth = width /  ((float) resolution);

        float x,y;
        float[] sampleFFT;
        int clip = 80;
        int i = 0;
        Iterator<float[]> fftIterator = fftHistory.iterator();
        while (fftIterator.hasNext()) {
            try {
                sampleFFT = fftIterator.next();
                y = height -  (spectrumHeight*i);
                int freqPoints = sampleFFT.length;

                for (int j = 0; j < resolution; j++){
                    x = (sampleWidth*j);
                    float magnitude = (freqPoints > 0) ? sampleFFT[j*freqPoints/resolution] : 0f;
                    int colorIndex = ((int) (((float) spectogramColors.range ) *   Math.min((magnitude / clip), 1.0)))% spectogramColors.range;
                    int[] RGB = spectogramColors.color_map[colorIndex];
                    paintSpectogram.setColor(Color.rgb(RGB[0], RGB[1], RGB[2]));
                    canvas.drawRect(x, y- spectrumHeight, x-sampleWidth, y, paintSpectogram);
                }

                i++;
            } catch (ConcurrentModificationException cme){
                cme.printStackTrace();
                break;
            }

        }

    }

    private void drawDisplayMessage(Canvas canvas){
        if (message != null && message.first != null &&
                0 > System.currentTimeMillis()){
            canvas.drawText(message.second,
                    (this.getWidth() - errPaint.measureText(message.second)) /2,
                    this.getHeight() -getTextPxSize(16f),
                    errPaint);
        }
    }

    private void setSpecPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.specColor));
        paint.setStyle(Paint.Style.FILL);
    }

    private void setTextPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_TEXT_SIZE));
        paint.setTypeface(Typeface.MONOSPACE);
    }

    private void setErrPaint(Context context, Paint paint) {
        paint.setColor(context.getResources().getColor(R.color.errPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_TEXT_SIZE));
    }

    private static float getTextDpSize(float textSize){
        return (textSize / Resources.getSystem().getDisplayMetrics().density);
    }

    private static float getTextPxSize(float textSize){
        return (textSize * Resources.getSystem().getDisplayMetrics().density);
    }

    private boolean canDrawSpectogram(){
        return (resolution >= minResolution);
    }

    private int avgDrawTime(){
        if (System.currentTimeMillis() - lastPaintTimeAvg.first > 1000){
            lastPaintTimeAvg = new Pair<>(System.currentTimeMillis(),
                    (whenToDraw.size() > 0) ? DrawAverage(whenToDraw) : 0);
        }
        return lastPaintTimeAvg.second;
    }

    private int DrawAverage(List<Long> mArray){
        int sum = 0;
        for (long l : mArray){
            sum += (int) l;
        }
        return sum / mArray.size();
    }


    @Override
    public Canvas ifActive(Canvas canvas) {
        canvas.drawColor(background);
        if (!(canDrawSpectogram())){
            String gpu_warn_string = "Memory low";
            drawTitle(canvas);
            canvas.drawText(gpu_warn_string,
                    (this.getWidth() - errPaint.measureText(gpu_warn_string))/2,
                    this.getHeight() - getTextPxSize(16f),
                    errPaint);
            return canvas;
        }

        if (canDrawSpectogram() &&
                whenToDraw.size() >= AudioCollectTest.displaySamples / 5 &&
                avgDrawTime() > fps) {
            whenToDraw = null;
            resolution /= 2;
            message = new Pair<>(System.currentTimeMillis() + 10000, "Reducing sample resoution due to low mem");
            Log.w(LOG_TAG, "drawing too quickly. Reduced res to " + String.valueOf(resolution));

            return canvas;
        }


        long time0 = System.currentTimeMillis();
        drawSpectrum(canvas);
        long time1 = System.currentTimeMillis();
        drawLegend(canvas);
        long time2 = System.currentTimeMillis();
        drawTitle(canvas);
        long time3 = System.currentTimeMillis();
        drawDisplayMessage(canvas);
        long time4 = System.currentTimeMillis();
        whenToDraw.add(time1 - time0);
        whenToDraw.add(time2 - time1);
        whenToDraw.add(time3 - time2);
        whenToDraw.add(time4 - time3);


        while (whenToDraw.size() > AudioCollectTest.displaySamples){
            whenToDraw.remove(0);
        }

        return canvas;
    }

    @Override
    public void drawAll() {
        drawSurface(this);
    }
}
