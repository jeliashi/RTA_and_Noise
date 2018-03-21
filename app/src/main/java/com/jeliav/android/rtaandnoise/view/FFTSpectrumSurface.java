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
 The dynamic view of the sound
 */

public class FFTSpectrumSurface extends SpectrumSurface implements DrawingInterface {

    // DONE Need to overlay gridlines to main display
    // DONE Need to overlay labels to gridlines in main display
    // TODO Need to make legend clear and legible
    // TODO need to convert power into dB

    public static final String LOG_TAG = FFTSpectrumSurface.class.getSimpleName();

    private static final float PAINT_TEXT_SIZE = 12f;
    private static final float PAINT_LABEL_SIZE = 8f;
    public static int background;
    public static int resolution =  512;
    public static int minResolution = 64;
    private int fps = 80;

    public static Paint paintSpectogram = new Paint();
    public static Paint textPaint = new Paint();
    public static Paint errPaint = new Paint();
    public static Paint labelPaint = new Paint();

    private AudioCollectTest mAudio;

    private SpectogramColors spectogramColors = new SpectogramColors();

    public static final SparseIntArray hotThreshold = new SparseIntArray();
    static{
        hotThreshold.put(512,200);
        hotThreshold.put(256,2500);
        hotThreshold.put(128,3000);
        hotThreshold.put(64,4000);
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
        setLabelPaint(context, labelPaint);
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
        float clip = (float) Math.log((float) hotThreshold.get(resolution));
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
                    int colorIndex = ((int) (((float) spectogramColors.range ) *   Math.min(Math.max(magnitude/clip, 0.0f), 1.0f)))% spectogramColors.range;
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

    private void drawGridandAxes(Context context, Canvas canvas){
        float[] freqLines;
        switch (resolution){
            case 512:
                freqLines = new float[]{25f, 31f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 310f, 400f,
                500f, 630f, 800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3100f, 4000f, 5000f, 6300f,8000f, 10000f, 12500f, 16000f};
                break;
            case 256:
                freqLines = new float[]{31f, 63f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f};
                break;
            case 128:
                freqLines = new float[]{63f, 250f, 1000f, 4000f, 16000f};
                break;
            default:
                freqLines = new float[]{63f, 250f, 1000f, 4000f, 16000f};
                break;
        }
        int height = getHeight();
        int width = getWidth();
        float x,y;
//        canvas.drawRect(0, height - getTextPxSize(PAINT_LABEL_SIZE), width, height, textPaint);
        for (float freq : freqLines){
            x = AudioCollectTest.findFirstInterpGreater(freq) * (float) width;
            canvas.drawLine(x, 0, x,(float) height, errPaint);
            canvas.drawText(niceAxesDisplay(freq), x - labelPaint.measureText(niceAxesDisplay(freq))/2, height, labelPaint);
            canvas.drawText(niceAxesDisplay(freq), x - labelPaint.measureText(niceAxesDisplay(freq))/2, getTextPxSize(PAINT_LABEL_SIZE), labelPaint);
        }

    }

    private String niceAxesDisplay(float freq){
        if (freq < 1000){
            return String.valueOf((int) freq);
        } else if ((freq%1000f == 0f)){
            return (String.valueOf( (int) freq /1000) + "k");
        } else return (String.valueOf((int) freq/1000) + "." + String.valueOf((freq%1000f)/100).substring(0,1) + "k");
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

    private void setLabelPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.textPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_LABEL_SIZE));
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
        if (null == canvas) return canvas;
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
        drawGridandAxes(this.getContext(), canvas);
        long time5 = System.currentTimeMillis();
        whenToDraw.add(time1 - time0);
        whenToDraw.add(time2 - time1);
        whenToDraw.add(time3 - time2);
        whenToDraw.add(time4 - time3);
        whenToDraw.add(time5- time4);

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
