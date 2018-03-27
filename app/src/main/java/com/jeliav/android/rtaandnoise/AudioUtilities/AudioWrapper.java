package com.jeliav.android.rtaandnoise.AudioUtilities;

import java.util.ArrayDeque;

/**
 */

public class AudioWrapper {

    public boolean mShouldContinue;

    static int mBufferSize = AudioTools.BufferSize;//2*AUDIO_INPUT_SAMP_RATE / MIN_FREQ_CALCULATED;

    short[] mAudioStream;
    final ArrayDeque<float[]> fftStream = new ArrayDeque<>();
    final ArrayDeque<float[]> fftPhaseStream = new ArrayDeque<>();
    short[] audioBuffer;

    public AudioWrapper(){
        for (int i=0; i < AudioTools.displaySamples; i++){
            fftStream.add(new float[AudioTools.outputFFTLength]);
            fftPhaseStream.add(new float[AudioTools.outputFFTLength]);
        }
    }

    public ArrayDeque<float[]> getFFTStream(){
        return fftStream;
    }
    public ArrayDeque<float[]> getFFTPhaseStream() { return fftPhaseStream; }
    public short[] getAudioStream() { return mAudioStream; }


}
