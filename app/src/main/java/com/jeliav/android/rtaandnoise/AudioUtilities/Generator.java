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

public class Generator extends AudioWrapper{

    private static final String LOG_TAG = Generator.class.getSimpleName();

    private AudioTrack audioTrack = null;

    private float phase;
    private int AUDIO_OUTPUT_SAMPLE_RATE = AudioTools.AUDIO_SAMPLE_RATE;

    private short MAX_AMPLITUDE = Short.MAX_VALUE;

    private Random random = new Random();

    private Thread produceThread;

    public Generator(){
        audioBuffer = new short[mBufferSize];
        phase = 0;
        mAudioStream = new short[mBufferSize];
        for (int i=0; i < AudioTools.displaySamples; i++){
            fftStream.add(new float[AudioTools.outputFFTLength]);
            fftPhaseStream.add(new float[AudioTools.outputFFTLength]);
        }
        initializeOutput();
    }

    private void initializeOutput(){
        int AUDIO_ENCODING = AudioTools.AUDIO_ENCODING;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                AUDIO_OUTPUT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_DEFAULT,
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

    private void generateSineTone(float freq){
        float dPhase = freq / (float) AUDIO_OUTPUT_SAMPLE_RATE;
        for (int i=0 ; i < mBufferSize; i++){
            audioBuffer[i] = (short)( Math.sin(2*Math.PI *phase) * MAX_AMPLITUDE);
            phase += dPhase;
            phase %= 1f;
        }
    }

    private void generateWhiteNoise(){
        for (int i=0; i < mBufferSize; i++){
            audioBuffer[i] = (short) random.nextGaussian();
            }
    }

    private void generatePinkNoise(){
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
            audioBuffer[i] = nextValue(poles, multipliers, values);
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
    public ArrayDeque<float[]> getFftPhaseStream(){ return fftPhaseStream;}
    public short[] getAudioStream(){ return mAudioStream;}

    public void clearAudioBuffer(){ mAudioStream = new short[mBufferSize];}

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

    private void play(){
        while (mShouldContinue){
            generatePinkNoise();
//            generateSineTone(440f);

            fftStream.removeLast();
            fftPhaseStream.removeLast();
            mAudioStream = audioBuffer;
            AudioTools.ComplexRadialArray fft = AudioTools.calculateComplexFFT(audioBuffer);
            fftStream.push(fft.magnitude);
            fftPhaseStream.push(fft.phase);
            //int bufferWrite =
            audioTrack.write(audioBuffer, 0, mBufferSize);
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
        mAudioStream = new short[mBufferSize];
        audioTrack.stop();
        audioTrack.release();
    }
}
