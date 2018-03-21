package com.jeliav.android.rtaandnoise.AudioUtilities;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Random;

/**
 class for generating audio
 */

public class Generator {

    public static final String LOG_TAG = Generator.class.getSimpleName();

    public static int AUDIO_OUTPUT_SAMP_RATE = AudioTools.AUDIO_SAMPLE_RATE;
    public static int AUDIO_ENCODING = AudioTools.AUDIO_ENCODING;
    public static int mBufferSize = AudioTools.BufferSize;

    private AudioTrack audioTrack = null;

    private final short[] mBuffer;
    private float phase;

    public static boolean mShouldContinue = false;

    private short MAX_AMPLITUDE = Short.MAX_VALUE;

    private Random random = new Random();
    public ArrayDeque<short[]> mAudioStream;
    public final ArrayDeque<float[]> fftStream = new ArrayDeque<>();

    public Thread produceThread;

    public Generator(){
        mBuffer = new short[mBufferSize];
        phase = 0;
        initializeOutput();
    }

    private void initializeOutput(){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                AUDIO_OUTPUT_SAMP_RATE, AudioFormat.CHANNEL_OUT_DEFAULT,
                AUDIO_ENCODING, mBufferSize,
                AudioTrack.MODE_STREAM);

        for (int i=0; i < AudioTools.displaySamples; i++){
            fftStream.add(new float[]{});
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            audioTrack.setVolume(AudioTrack.getMaxVolume());
        }
        audioTrack.play();

    }

    public void generateSineTone(float freq){
        float dPhase = freq / (float) AUDIO_OUTPUT_SAMP_RATE;
        for (int i=0 ; i < mBufferSize; i++){
            mBuffer[i] = (short)( Math.sin(2*Math.PI *phase) * MAX_AMPLITUDE);
            phase += dPhase;
            phase %= 1f;
        }
    }

    public void generateWhiteNoise(){
        for (int i=0; i < mBufferSize; i++){
            mBuffer[i] = (short) random.nextGaussian();
            }
    }

    public void generatePinkNoise(){
        float alpha = 1f;
        int poles = 5;
        double[] multipliers = new double[poles];
        double[] values = new double[poles];
        double a = 1;
        for (int i = 0; i < poles ; i++){
            a *= (i - alpha/2 )/(i+1);
            multipliers[i] = a;
        }
        for (int i = 0; i < poles ; i++){
            nextValue(poles, multipliers, values);
        }
        for (int i=0; i < mBufferSize; i++){
            mBuffer[i] = nextValue(poles, multipliers, values);
        }
    }

    private short nextValue(int poles, double[] multipliers, double[] values){
        double x = random.nextGaussian();
        for (int j = 0; j < poles ; j++){
            x-= multipliers[j]*values[j];
        }
        System.arraycopy(values, 0, values, 1, values.length -1);
        values[0] = x;
        return (short) (x*MAX_AMPLITUDE);
    }

    public ArrayDeque<float[]> getFFTStream(){ return fftStream;}

    public void begin(){
        if (audioTrack == null || audioTrack.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e(LOG_TAG, "Can't begin generator, no audio track to play from");
        }
        produceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                play();
            }
        },"Audio Generator Thread");
        produceThread.start();
    }

    public void play(){
        while (mShouldContinue){
//            generatePinkNoise();
            generateSineTone(440f);

            fftStream.removeLast();
            fftStream.push(AudioTools.calculateFFT(mBuffer));
            int bufferWrite = audioTrack.write(mBuffer, 0, mBufferSize);
        }
    }

    public void stop(){
        Log.d(LOG_TAG, "Stop generator");
        mShouldContinue = false;
        if (produceThread.getState() == Thread.State.RUNNABLE) return;
        try{
            produceThread.join();
        } catch (InterruptedException ie){
            ie.printStackTrace();
        }
        audioTrack.stop();
        audioTrack.release();
    }
}
