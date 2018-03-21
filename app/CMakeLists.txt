cmake_minimum_required(VERSION 3.4.1)
add_library(kiss-fft-lib
            SHARED
            src/main/cpp/KissFFTImplementation.cpp ../kiss_fft.c ../kiss_fftr.c)
