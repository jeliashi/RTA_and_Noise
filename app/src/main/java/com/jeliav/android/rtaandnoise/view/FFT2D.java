package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.ViewPropertyAnimator;

import com.jeliav.android.rtaandnoise.R;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.List;


/**
 * Created by jeliashiv on 3/9/18.
 */

public class FFT2D extends BaseSurface {
    private static final String LOG_TAG = FFT2D.class.getSimpleName();

    private static final float PAINT_TEXT_SIZE = 12f;

    public static float sec = 10f;
    public static float sampleRate = 44100f;
    public static float sampleSize = 4096;
    public static float framesPerSec = 80;
    public static float hertz = sampleRate/sampleSize;
    public static float historySamples = sec * hertz;
    public static float resolution =  512;
    public static float minResolution = 64;
    public static ArrayDeque<List<Float>> ffts = new ArrayDeque<>();

    public static Paint paintSpectogram = new Paint();
    public static Paint textPaint = new Paint();
    public static Paint errPaint = new Paint();

    private Spectogram spectogram = new Spectogram();

    public static int background;

    public static final SparseIntArray hotThreshold = new SparseIntArray();
    static
    {
        hotThreshold.put(512,15000);
        hotThreshold.put(256,15000);
        hotThreshold.put(128,30000);
        hotThreshold.put(64,45000);
    }

    public static ArrayDeque<Long> drawTimes =  new ArrayDeque<Long>();
    public static Pair<Long, String> message;
    public static Pair<Long, String> lastAverage = new Pair<>(System.currentTimeMillis(), "0");


    public FFT2D(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        background = context.getResources().getColor(R.color.background);

        setSpecPaint(context, paintSpectogram);
        setTextPaint(context, textPaint);
        setErrPaint(context, errPaint);
    }

    public void drawTitle(Canvas canvas){
        canvas.drawText("Sound Map", getTextPxSize(16f), getTextPxSize(24f), textPaint);
    }

    public void drawIndicator(Canvas canvas) {
        float height = this.getHeight();
        for (int i = 0 ; i< height ; i++ ){
            double f = i/((double) height);
            int spec_index = (int) (256 * (1-f));
            int[] color_array = spectogram.color_map[spec_index];

            int color = Color.rgb(color_array[0],color_array[1],color_array[2]);

            paintSpectogram.setColor(color);
            canvas.drawRect(0f, (float) i,  10f, (float) i + 1f, paintSpectogram);
        }
    }

    public void drawMessage(Canvas canvas) {
        if ((message != null || message.second != null)  && 0 < System.currentTimeMillis()) {
            canvas.drawText(message.second,
                    (this.getWidth() - errPaint.measureText(message.second)) /2,
                    this.getHeight() - getTextPxSize(16f),
                    errPaint
            );
        }
    }

    public void drawSpectogram(Canvas canvas){
        int fft_width = (int) ((float) this.getWidth() / historySamples);
        int band_height = (int) ((float) this.getHeight() / resolution);

        float x;
        float y;
        float[] band;
        float hot = hotThreshold.get((int) resolution, 0);

        for (int i =0; i < ffts.size(); i++){
            synchronized (ffts) {

            }
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

    private void setErrPaint(Context context, Paint paint){
        paint.setColor(context.getResources().getColor(R.color.errPaint));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getTextPxSize(PAINT_TEXT_SIZE));
        paint.setTypeface(Typeface.MONOSPACE);
    }

    @Override
    public Canvas ifActive(Canvas canvas) {
        return null;
    }

    @Override
    public void drawAll() {

    }

    private static float getTextDpSize(float textSize){
        return (textSize / Resources.getSystem().getDisplayMetrics().density);
    }

    private static float getTextPxSize(float textSize){
        return (textSize * Resources.getSystem().getDisplayMetrics().density);
    }

}
