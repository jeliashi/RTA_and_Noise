package com.jeliav.android.rtaandnoise.AudioUtilities;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import java.util.ArrayDeque;

/**
 This Audio Collect Test will
Collects at sample sizes of 4096 at 44100 which correlates to a min freq of 21.53 Hz
 and a max freq of 22050 Hz
 Let's regrid this to
 25 31 40 50 63 80 100 125 160 200 250 310 400 500 630 800 1000
 1250 1600 2000 2500 3100 4000 5000 6300 8000 10k 12.5k 15k 20k
Hz
 */


public class AudioCollectTest{

    private static final String LOG_TAG = AudioCollectTest.class.getSimpleName();

    public static int AUDIO_INPUT_SAMP_RATE = AudioTools.AUDIO_SAMPLE_RATE;
    private int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static int AUDIO_ENCODING = AudioTools.AUDIO_ENCODING;

    public boolean mShouldContinue;

    private AudioRecord mAudioRecord;
    public Thread collectThread;

    public static int mBufferSize = AudioTools.BufferSize;//2*AUDIO_INPUT_SAMP_RATE / MIN_FREQ_CALCULATED;

    public short[] mAudioStream;
    public final ArrayDeque<float[]> fftStream = new ArrayDeque<float[]>();
    public final ArrayDeque<float[]> fftPhaseStream = new ArrayDeque<float[]>();
    private short[] audioBuffer;

    public AudioCollectTest(){
        for (int i=0; i < AudioTools.displaySamples; i++){
            fftStream.add(new float[AudioTools.outputFFTLength]);
            fftPhaseStream.add(new float[AudioTools.outputFFTLength]);
        }
    }

    public ArrayDeque<float[]> getFFTStream(){
        return fftStream;
    }
    public short[] getAudioStream() { return mAudioStream; }

    public void startInputStream(int audio_source) {

        if (mBufferSize == AudioRecord.ERROR_BAD_VALUE || mBufferSize == AudioRecord.ERROR){
            mBufferSize = AUDIO_INPUT_SAMP_RATE*2;
        }

        audioBuffer = new short[mBufferSize];
        mAudioStream = new short[mBufferSize];

        Log.d(LOG_TAG, "Creating new Audio record");

        mAudioRecord = new AudioRecord(audio_source,
                AUDIO_INPUT_SAMP_RATE,
                AUDIO_CHANNEL_CONFIG,
                AUDIO_ENCODING,
                mBufferSize
                );

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e(LOG_TAG, "Failed to initialize audio record!");
        }

    }


    public void startRecording(){
        if (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e(LOG_TAG, "Can't begin recording, no Audio Record to record");
            return;
        }
        mAudioRecord.startRecording();

        collectThread =  new Thread(new Runnable() {
            @Override
            public void run() {
                readMic();
            }
        }, "Audio Record Thread");
        collectThread.start();
    }


    private void readMic(){
        while (mShouldContinue) {
            int bufferRead = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);

            mAudioStream = audioBuffer;
            fftStream.removeLast();
            fftPhaseStream.removeLast();

            AudioTools.ComplexRadialArray fft = AudioTools.calculateComplexFFT(audioBuffer);
            fftStream.push(fft.magnitude);
            fftPhaseStream.push(fft.phase);

        }

    }

    public void stopRecording(){
        Log.d(LOG_TAG, "Stop Recording message received");
        mShouldContinue = false;
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
    }

}
