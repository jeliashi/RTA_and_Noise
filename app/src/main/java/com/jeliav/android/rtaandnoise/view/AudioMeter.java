package com.jeliav.android.rtaandnoise.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioTools;
import com.jeliav.android.rtaandnoise.AudioUtilities.AudioWrapper;
import com.jeliav.android.rtaandnoise.R;

/**
 * Created by jeliashiv on 3/22/18.
 */

public class AudioMeter extends SpectrumSurface implements DrawingInterface {

    public static final String LOG_TAG = AudioMeter.class.getSimpleName();
    private static final float MIN_POWER_DB = -60;
    private static final int METER_BINS = 41;

    public short max_amplitude = Short.MAX_VALUE;

    private float max_amp_memory = MIN_POWER_DB;
    private AudioWrapper audioSource;

    public static Paint meterPaint = new Paint();

    public AudioMeter(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        setMeterPaint(context, meterPaint);
    }

    private void setMeterPaint(Context context, Paint paint){
        paint.setStyle(Paint.Style.FILL);
    }

    public void setAudioSource(AudioWrapper source) { audioSource = source; }

    private void drawMeter(Canvas canvas){
        short[] audioSample = audioSource.getAudioStream();
        double sum = 0;
        for (short s : audioSample){
//            Log.d(LOG_TAG,String.valueOf(sum));
            if (s < 0) sum -= (double) s;
            else sum += (double) s;
        }
        sum /= (double) audioSample.length;


        float inDB = 10f * (float)Math.min(Math.log(sum) - Math.log((double) max_amplitude), 1.0);

        if (inDB > max_amp_memory) max_amp_memory = inDB;
        else max_amp_memory -= 3f;
        float refDB = MIN_POWER_DB;
        float dDB = -1 * refDB / (METER_BINS - 1);
        float width = this.getWidth();
        float height = this.getHeight();

        float dY = height / ((float) (METER_BINS + 1)*4f);
        // gonna have 2 red 5 yellow and 14 green
        for (int i=0; i < METER_BINS - 7; i++){
            if (max_amp_memory > refDB) meterPaint.setColor(this.getResources().getColor(R.color.greenOnMeter));
            else meterPaint.setColor(this.getResources().getColor(R.color.greenOffMeter));
            canvas.drawRect(width*0.2f, height - dY * (float) ((i*4) +4), width*0.8f, height - dY * (float) ((i*4)+1), meterPaint );
            refDB += dDB;
        }
        for (int i = (METER_BINS - 7); i < METER_BINS -2; i++){
            if (max_amp_memory > refDB) meterPaint.setColor(this.getResources().getColor(R.color.yellowOnMeter));
            else meterPaint.setColor(this.getResources().getColor(R.color.yellowOffMeter));
            canvas.drawRect(width*0.2f, height - dY * (float) ((i*4)+4), width*0.8f, height - dY * (float) ((i*4) +1), meterPaint );
            refDB += dDB;
        }
        for (int i = (METER_BINS - 2); i < METER_BINS; i++){
            if (max_amp_memory > refDB) meterPaint.setColor(this.getResources().getColor(R.color.redOnMeter));
            else meterPaint.setColor(this.getResources().getColor(R.color.redOffMeter));
            canvas.drawRect(width*0.2f, height - dY * (float) ((i*4)+4), width*0.8f, height - dY * (float) ((i*4) +1), meterPaint );
            refDB += dDB;
        }

    }

    public void changeClipValue(short clip){ max_amplitude = clip;}

    @Override
    public Canvas ifActive(Canvas canvas) {
        int minDrawTime = 80;
        if (null == canvas) return null;
        canvas.drawColor(background);

        if (whenToDraw.size() >= AudioTools.displaySamples &&
                avgDrawTime() > minDrawTime){
            whenToDraw = null;
            Log.w(LOG_TAG, "drawing too quickly");
            return canvas;
        }

        long time0 = System.currentTimeMillis();
        drawMeter(canvas);
        long time1 = System.currentTimeMillis();
        whenToDraw.add(time1 - time0);

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
