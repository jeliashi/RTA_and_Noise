package com.jeliav.android.rtaandnoise.view;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
This will be the manager for all the views so that they can run on
 their own and as some views consume too many resources, they can be isolated
 */

public class SurfaceManager {
    public void handleState(){

    }

    Handler mHandler;
    private  static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private ThreadPoolExecutor mSurfaceThreadPool;


    public static SurfaceManager sInstance;
    static {
        sInstance = new SurfaceManager();
    }

    private SurfaceManager(){
        mHandler = new Handler(Looper.getMainLooper()){

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };

        final BlockingQueue<Runnable> mSurfaceToPaintQueue;

        mSurfaceToPaintQueue = new LinkedBlockingQueue<Runnable>();

        mSurfaceThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mSurfaceToPaintQueue
        );


    }

    static public SpectrumSurface startRTAView(FFTSpectrumSurface surface){
//        sInstance.mSurfaceThreadPool.execute(surface.getRunnable);
        return null;
    }


    public static void cancelAll(){

    }
}
