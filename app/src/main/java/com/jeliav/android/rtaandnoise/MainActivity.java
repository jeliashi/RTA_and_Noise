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

import com.jeliav.android.rtaandnoise.AudioUtilities.AudioCollectTest;
import com.jeliav.android.rtaandnoise.AudioUtilities.Generator;
import com.jeliav.android.rtaandnoise.view.FFTSpectrumSurface;


public class MainActivity extends AppCompatActivity {

    // TODO need to handle lifetime cycles
    // TODO need to add dB meter with dBC of the past 5 seconds displayed on it
    // TODO to handle shared settings of: audio input, audio gain, level clip

    // TODO need to add audio generator
    // TODO need to add shared settings for audio generator

    // TODO need new activity with intent which measures transfer function, delay, and coherence
    // TODO need to add transfer function display
    // TODO need to add phase plot display
    // TODO need to add coherence measurement display

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_AUDIO_PERMSSION = 200;

    public Button mStart;
    public Button mGenerateButton;
    public AudioCollectTest mCollect;
    public Generator mGenerate;
    public FFTSpectrumSurface mSpectrum;
    public boolean isRecording = false;
    private Thread drawingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "Permissions acquired? " + String.valueOf(requestAudioPermissions()));
        mStart = findViewById(R.id.start_record_button);
        mGenerateButton = findViewById(R.id.pink_noise_button);
        mCollect = new AudioCollectTest();
        mGenerate = new Generator();
        mSpectrum = findViewById(R.id.spectrum_view);
        mSpectrum.setAudioSource(mCollect);


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
                    beginDrawing();
                }
            }
        });
        mGenerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGenerate.mShouldContinue){
                    mGenerate.stop();
                } else{
                    mGenerate.mShouldContinue = true;
                    mGenerate.begin();
                }
            }
        });
    }

    private void beginDrawing(){
        drawSurfaces();
        Thread.State drawState = drawingThread.getState();
        Log.d(LOG_TAG, "drawing state: " + drawState.toString());
        switch (drawState){
            case NEW:
                drawingThread.start();
                break;
            case TERMINATED:
                drawingThread.start();
                break;
            default:
                drawingThread.interrupt();
                drawingThread.start();
                break;
        }
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
        }, "Drawing Thread");
    }
}
