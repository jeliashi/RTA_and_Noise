//
// Created by Jonathan Eliashiv on 3/21/18.
//
#include <jni.h>
#include <android/log.h>
#include "../../../../kiss_fft.h"
#include "../../../../kiss_fftr.h"

#ifndef RTA_AND_NOISE_KISSFFTIMPLEMENTATION_H
#define RTA_AND_NOISE_KISSFFTIMPLEMENTATION_H

extern "C" {
#define LOG_TAG "KissFFTWrapper"

JNIEXPORT jdoubleArray JNICALL
Java_com_jeliav_android_rtaandnoise_AudioUtilities_KissFFTWrapper_doFFT(JNIEnv *env, jobject,
                                                                        jdoubleArray in,
                                                                        jint isInverted){
//    if (in == NULL){
//        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "null input");
//        return NULL;
//    }

    int inputLength = env->GetArrayLength(in);

    int fftLength = inputLength;
//    if (fftLength < 1){
//        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "too short input to computer");
//        return NULL;
//    }

    kiss_fft_cfg cfg = kiss_fft_alloc(fftLength, isInverted, 0, 0);

    jdouble *inRealValues = env->GetDoubleArrayElements(in, 0);
    kiss_fft_cpx *inComplexArray = new kiss_fft_cpx[inputLength];
    kiss_fft_cpx *outComplexArray = new kiss_fft_cpx[inputLength];

    for (int i=0; i < inputLength; i++){
        inComplexArray[i].r = inRealValues[i];
        inComplexArray[i].i = 0;
    }

    env->ReleaseDoubleArrayElements(in, inRealValues, 0);

    kiss_fft(cfg, inComplexArray,outComplexArray);


    jdoubleArray out = env->NewDoubleArray(fftLength*2);
    jdouble *outValues = env->GetDoubleArrayElements(out, 0);

    for (int i=0; i < fftLength; i++){
        outValues[2*i] = outComplexArray[i].r;
        outValues[2*i+1] = outComplexArray[i].i;
    }

    env->ReleaseDoubleArrayElements(out, outValues, 0);
    free(cfg);

    delete[] inComplexArray;
    delete[] outComplexArray;

    return  out;
}

};



#endif //RTA_AND_NOISE_KISSFFTIMPLEMENTATION_H
