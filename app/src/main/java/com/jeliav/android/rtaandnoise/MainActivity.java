package com.jeliav.android.rtaandnoise;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jeliav.android.rtaandnoise.AudioTools.AudioCollectTest;
import com.jeliav.android.rtaandnoise.view.FFTSpectrumSurface;


public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_AUDIO_PERMSSION = 200;

    public Button mStart;
    public AudioCollectTest mCollect;
    public FFTSpectrumSurface mSpectrum;
    public boolean isRecording = false;
    private Thread drawingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "Permissions acquired? " + String.valueOf(requestAudioPermissions()));
        mStart = findViewById(R.id.start_record_button);
        mCollect = new AudioCollectTest();
        mSpectrum = findViewById(R.id.spectrum_view);
        mSpectrum.setAudioSource(mCollect);
        drawSurfaces();


        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording){
                    mCollect.mShouldContinue = false;
                    mCollect.stopRecording();
                    isRecording = false;

                } else {
                    mCollect.mShouldContinue = true;
                    mCollect.startInputStream(MediaRecorder.AudioSource.MIC);
                    mCollect.startRecording();
                    isRecording = true;
                    drawingThread.start();
                }
            }
        });
    }


    private boolean requestAudioPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            Log.d(LOG_TAG, "Audio Permissions needed!");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_PERMSSION);
        }
        return true;
    }

    private void drawSurfaces(){
        drawingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRecording){
                    mSpectrum.drawAll();
                }
            }
        });
    }
}
