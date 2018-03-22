package com.jeliav.android.rtaandnoise.AudioUtilities;

import android.media.AudioFormat;
import android.util.Log;

import java.util.ArrayDeque;

/**
 Easier to store all the audio tools in here
 */

public class AudioTools {
    static final int AUDIO_SAMPLE_RATE = 44100;
    static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    static int BufferSize = 4096;
    public static final int displaySamples = 50;
    static final int outputFFTLength = BufferSize/2;

    private static KissFFTWrapper fftWrapper;
    static{
        fftWrapper = new KissFFTWrapper();
    }

    private static float[] initialDist = new float[BufferSize/2];
    static{
        float dF = (float) AUDIO_SAMPLE_RATE / (float) BufferSize;
        for (int i =  0; i < BufferSize /2; i++){
            initialDist[i] = ((float) (i+1) * dF);
        }
            Log.d("tools",String.valueOf(initialDist[initialDist.length-1]));
    }
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

    private static int findFirstGreater(float input){
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
    private static float[] linearInterpolation(float[] input){
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

    public static float[] calculateFFT(short[] inputAudio){
        float[] inputFloat = new float[inputAudio.length];
        for (int i = 0; i < inputAudio.length; i++){
            inputFloat[i] = ((float) inputAudio[i]) / 32767.0f;
        }

        double[] fft =  fftWrapper.fft(inputFloat);

        float[] fft_power = new float[initialDist.length];
        for (int i =0; i < (initialDist.length); i++){
            double real = fft[i*2];
            double imag = fft[i*2+ 1];

            fft_power[i] = (float) Math.sqrt((real*real + imag*imag));
        }

        return linearInterpolation(fft_power);
    }

    static ComplexRadialArray calculateComplexFFT(short[] inputAudio){

        float[] inputFloat = new float[inputAudio.length];
        for (int i = 0; i < inputAudio.length; i++){
            inputFloat[i] = ((float) inputAudio[i]) / 32767.0f;
        }

        double[] fft =  fftWrapper.fft(inputFloat);
        float[] power = new float[initialDist.length];
        float[] phase = new float[initialDist.length];

//        Log.d("FFT", String.valueOf(fft.length) + " vs " + String.valueOf(test.length));
        for (int i =0; i < (initialDist.length); i++){
            double real = fft[i*2];
            double imag = fft[i*2+ 1];

            power[i] = (float) Math.sqrt((real*real + imag*imag));
            phase[i] = (float) Math.atan2(imag, real);
        }

        return new ComplexRadialArray(linearInterpolation(power),  linearInterpolation(phase));

    }

    public static float[] caculatePhaseFromFFT(float[] inMag, float[] inPhase, float[] outMag, float[] outPhase){
        if (inMag.length != inPhase.length ||
                inMag.length != outMag.length ||
                inMag.length != outPhase.length){
            throw new IndexOutOfBoundsException(String.format("All arrays must have same length (%d %d %d %d).", inMag.length, inPhase.length, outMag.length, outPhase.length));
        }

        float[] out = new float[inMag.length];
        float workMag, workPhase, workReal, workImag;

        for (int i = 0; i < inMag.length ; i++ ) {
            if (outMag[i] == 0) out[i] = 0;
            else {
                workMag = inMag[i] / outMag[i];
                workPhase = (float) ((inPhase[i] - outPhase[i] + Math.PI) % (2 * Math.PI) - Math.PI);
                workReal = workMag * (float) Math.cos(workPhase) - outMag[i];
                workImag = workMag * (float) Math.cos(workPhase);
                out[i] = (float) Math.atan2(workImag, workReal);
            }
        }
        return out;
    }

    public static float[] calculateCoherence(ArrayDeque<float[]> inMag, ArrayDeque<float[]> outMag){
        float[] out = new float[finalDist.length];
        float[] Cxy = calculateCorr(inMag.clone(), outMag.clone());
        float[] Cxx = calculateCorr(inMag.clone(), inMag.clone());
        float[] Cyy = calculateCorr(outMag.clone(), outMag.clone());

        for (int i = 0 ; i < out.length; i++){
            out[i] = Cxy[i] / (Cxx[i] * Cyy[i]);
        }

        return out;
    }

    private static float[] calculateCorr(ArrayDeque<float[]> inMag, ArrayDeque<float[]> outMag){
        float[] out = new float[finalDist.length];
        for (float[] inArray : inMag){
            float[] outArray = outMag.poll();
            if (inArray.length == 0 || outArray.length ==0) return out;
            for (int j = 0 ; j < finalDist.length ; j++){
                out[j] += inArray[j]*outArray[j] / (float) displaySamples;
            }
        }
        return out;
    }


    static class ComplexRadialArray{
        float[] magnitude;
        float[] phase;
        ComplexRadialArray(float[] mag, float[] phi){
            this.magnitude = mag;
            this.phase = phi;
        }
    }

}
