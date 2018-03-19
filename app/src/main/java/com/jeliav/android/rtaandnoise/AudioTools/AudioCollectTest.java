package com.jeliav.android.rtaandnoise.AudioTools;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayDeque;

/**
 This Audio Collect Test will


 */

public class AudioCollectTest{

    private static final String LOG_TAG = AudioCollectTest.class.getSimpleName();

    private int AUDIO_INPUT_SAMP_RATE = 44100;
    private int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public boolean mShouldContinue;

    private AudioRecord mAudioRecord;
    public Thread collectThread;

    private static int MIN_FREQ_CALCULATED = 20;

    private int mBufferSize = 4096;//2*AUDIO_INPUT_SAMP_RATE / MIN_FREQ_CALCULATED;
    private int displaySamples = 40;

    public ArrayDeque<short[]> mAudioStream;
    public ArrayDeque<float[]> fftStream;
    public float[] phase;
    private short[] audioBuffer;

    private NoiseOptimized noise;

    public AudioCollectTest(){
        noise = Noise.real().optimized().init(mBufferSize, true);

    }



    public void startInputStream(int audio_source) {
        Log.d(LOG_TAG, "Finding minimum buffer size");
//        mBufferSize = AudioRecord.getMinBufferSize(AUDIO_INPUT_SAMP_RATE,
//                AUDIO_CHANNEL_CONFIG,
//                AUDIO_ENCODING);

        if (mBufferSize == AudioRecord.ERROR_BAD_VALUE || mBufferSize == AudioRecord.ERROR){
            Log.d(LOG_TAG, "buffer size invalid, setting buffer size manually");
            mBufferSize = AUDIO_INPUT_SAMP_RATE*2;
        }

        audioBuffer = new short[mBufferSize/2];

        Log.d(LOG_TAG, "creating array deque for RAW data");

        mAudioStream = new ArrayDeque<short[]>();
        fftStream = new ArrayDeque<float[]>();
        for (int i =0; i < displaySamples; i++){
            mAudioStream.add(audioBuffer);
            fftStream.add(new float[]{});
        }


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

    private void calculateFFT(short[] inputAudio){
        float[] inputFloat = new float[inputAudio.length];
        for (int i = 0; i < inputAudio.length; i++){
            inputFloat[i] = ((float) inputAudio[i]) / 32767.0f;
        }

        float[] fft = new float[inputFloat.length+2];

        noise.fft( inputFloat, fft);


        float[] fft_power = new float[fft.length/2];
        float[] fft_phase = new float[fft.length/2];
        for (int i =0; i < (fft.length/2); i++){
            float real = fft[i*2];
            float imag = fft[i*2+ 1];

            fft_power[i] = (float) Math.sqrt((real*real + imag*imag));
            fft_phase[i] = (float) Math.atan2(imag,real);
        }
        updateArrays(fft_power, fft_phase);

    }

    private void updateArrays(float[] power, float[] new_phase){
        fftStream.removeLast();
        fftStream.push(power);
        phase = new_phase;
    }

    private void readMic(){
        while (mShouldContinue) {
            int bufferRead = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);

            mAudioStream.removeLast();
            mAudioStream.push(audioBuffer);
            calculateFFT(audioBuffer);

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
