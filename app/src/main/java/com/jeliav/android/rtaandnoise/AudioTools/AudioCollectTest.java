package com.jeliav.android.rtaandnoise.AudioTools;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

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

    private int AUDIO_INPUT_SAMP_RATE = 44100;
    private int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public boolean mShouldContinue;

    private AudioRecord mAudioRecord;
    public Thread collectThread;
    private static final int outputFFTLength = 1025;

    private int mBufferSize = 4096;//2*AUDIO_INPUT_SAMP_RATE / MIN_FREQ_CALCULATED;
    public static int displaySamples = 100;

    public ArrayDeque<short[]> mAudioStream;
    public final ArrayDeque<float[]> fftStream = new ArrayDeque<float[]>();
    public float[] phase;
    private short[] audioBuffer;

    private float[] initialDist = new float[mBufferSize/4 + 1];
    public static final float[] finalDist = new float[outputFFTLength];
    static{
        float intialFreq = 20f;
        float finalFreq = 20000f;
        float init_log = (float) Math.log10(intialFreq);
        float final_log = (float) Math.log10(finalFreq);
        float diff_log = (final_log - init_log) / (((float) outputFFTLength ) - 1);
        for (int i = 0; i < outputFFTLength; i++){
            finalDist[i] = (float) Math.pow(10, init_log + (((float) i) * diff_log));
        }
    }

    private NoiseOptimized noise;

    public AudioCollectTest(){
        noise = Noise.real().optimized().init(mBufferSize, true);
        for (int i=0; i < displaySamples; i++){
            fftStream.add(new float[]{});
        }
        for (int i =  0; i < (mBufferSize+1)/4 ; i++){
            initialDist[i] = (2f * (float) i * (float) AUDIO_INPUT_SAMP_RATE / (float) mBufferSize);
        }
        Log.d(LOG_TAG, initialDist.toString());
        Log.d(LOG_TAG, finalDist.toString());
    }

    public ArrayDeque<float[]> getFFTStream(){
        return fftStream;
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
        for (int i =0; i < displaySamples; i++){
            mAudioStream.add(audioBuffer);
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

    private int findFirstGreater(float input){
        for (int i = 0; i < initialDist.length; i++ ) {
            float compVal = initialDist[i];
            if (input < compVal) return i;
        }
        return initialDist.length - 1;
    }

    public static float findFirstInterpGreater(float input){
        for (int i=0; i < finalDist.length; i++){
            float compval = finalDist[i];
            if (input < compval) return ((float) i/ (float) finalDist.length);
        }
        return 1f;
    }

    private float[] linearInterpolation(float[] input){
        float[] outMag = new float[outputFFTLength];
        int i = 0;
        for (float freq : finalDist){
            int upperIndex = findFirstGreater(freq);
            float upper = initialDist[upperIndex];
            float lower = initialDist[upperIndex - 1];
            float frac = (freq - lower) / (upper - lower);
            outMag[i] = (1f- frac) * input[upperIndex - 1] + frac * input[upperIndex];
            i++;
        }
        return outMag;
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

            fft_power[i] = (float) Math.log(Math.sqrt((real*real + imag*imag)));
            fft_phase[i] = (float) Math.atan2(imag,real);
        }

        float[] test = linearInterpolation(fft_power);
        updateArrays(test, fft_phase);

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
