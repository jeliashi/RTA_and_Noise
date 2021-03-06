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


public class AudioCollectTest extends AudioWrapper{

    private static final String LOG_TAG = AudioCollectTest.class.getSimpleName();

    private AudioRecord mAudioRecord;

    public AudioCollectTest(){
        super();
    }

    public void startInputStream(int audio_source) {
        int AUDIO_INPUT_SAMP_RATE = AudioTools.AUDIO_SAMPLE_RATE;
        int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
        int AUDIO_ENCODING = AudioTools.AUDIO_ENCODING;

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

        Thread collectThread =  new Thread(new Runnable() {
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
