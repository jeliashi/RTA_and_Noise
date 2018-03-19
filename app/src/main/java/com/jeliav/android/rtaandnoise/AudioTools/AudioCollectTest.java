package com.jeliav.android.rtaandnoise.AudioTools;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayDeque;

/**
 This Audio Collect Test will


 */

public class AudioCollectTest{

    private static final String LOG_TAG = AudioCollectTest.class.getSimpleName();

    private Handler parentHandler;

    private int AUDIO_INPUT_SAMP_RATE = 44100;
    private int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public boolean mShouldContinue;

    private AudioRecord mAudioRecord;
    public Thread collectThread;

    private int mBufferSize;
    private int displaySamples = 40;

    public ArrayDeque<short[]> mAudioStream;
    short[] audioBuffer;

    public AudioCollectTest(){

    }



    public void startInputStream(int audio_source) {
        Log.d(LOG_TAG, "Finding minimum buffer size");
        mBufferSize = AudioRecord.getMinBufferSize(AUDIO_INPUT_SAMP_RATE,
                AUDIO_CHANNEL_CONFIG,
                AUDIO_ENCODING);

        if (mBufferSize == AudioRecord.ERROR_BAD_VALUE || mBufferSize == AudioRecord.ERROR){
            Log.d(LOG_TAG, "buffer size invalid, setting buffer size manually");
            mBufferSize = AUDIO_INPUT_SAMP_RATE*2;
        }

        audioBuffer = new short[mBufferSize/2];

        Log.d(LOG_TAG, "creating array deque for RAW data");

        mAudioStream = new ArrayDeque<short[]>();
        for (int i =0; i < displaySamples; i++){
            mAudioStream.add(audioBuffer);
        }

        Log.d(LOG_TAG, "Creating new Audio record");

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
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
            Log.d(LOG_TAG, "Should Continue: " + String.valueOf(mShouldContinue));

            mAudioRecord.read(audioBuffer, 0, audioBuffer.length);
            mAudioStream.removeLast();
            mAudioStream.push(audioBuffer);
            Log.d(LOG_TAG, audioBuffer.toString());
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
