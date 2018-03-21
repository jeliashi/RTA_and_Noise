package com.jeliav.android.rtaandnoise.AudioUtilities;

/**
 * Created by jeliashiv on 3/21/18.
 */

public class KissFFTWrapper {
    static {
        System.loadLibrary("kiss-fft-lib");
    }

    public KissFFTWrapper(){

    }

    public double[] fft(float[] input){
        boolean isForward = true;
        return fft(input, isForward);
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
