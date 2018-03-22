package com.jeliav.android.rtaandnoise.AudioUtilities;

/**
A Wrapper for the C++ FFT functionality that uses C code to calculate complex ffts
 */

public class KissFFTWrapper {
    static {
        System.loadLibrary("kiss-fft-lib");
    }

    KissFFTWrapper(){

    }

    public double[] fft(float[] input){
        return fft(input, true);
    }

    public double[] fft(float[] input, boolean isForward){
        double[] complexInput = new double[input.length];
        int i = 0;
        for (float in : input){
            complexInput[i++] = (double) in;
        }
        int transformType = (isForward) ? 0 : 1;
        return doFFT(complexInput, transformType);

    }

    private native double[] doFFT(double[] input, int is_reverse);

}
