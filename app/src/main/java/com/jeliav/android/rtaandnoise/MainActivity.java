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
import com.jeliav.android.rtaandnoise.view.AudioMeter;
import com.jeliav.android.rtaandnoise.view.CoherenceDisplaySurface;
import com.jeliav.android.rtaandnoise.view.FFTSpectrumSurface;
import com.jeliav.android.rtaandnoise.view.PhaseDisplaySurface;
import com.jeliav.android.rtaandnoise.view.TransferDisplaySurface;


public class MainActivity extends AppCompatActivity {

    // TODO need to handle lifetime cycles
    // DONE need to add dB meter with dBA of the past sample displayed on it
    // TODO to handle shared settings of: audio input, audio gain, level clip

    // TODO need to combine audio receive with audio generator thread or maybe not
    // TODO need to add shared settings for audio generator

    // DONE need to add transfer function display
    // DONE need to add phase plot display
    // DONE need to add coherence measurement display
    // TODO need to make UI dynamic

    // TODO need to make a thread pool for the drawing and separate all the surface draws into different threads

    // TODO add P2P functionality to be able to stream signals if running on computer as well.

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_AUDIO_PERMSSION = 200;

    public Button mStart;
    public Button mGenerateButton;
    public AudioCollectTest mCollect;
    public Generator mGenerate;
    public AudioMeter mInMeter;
    public AudioMeter mOutMeter;
    public FFTSpectrumSurface mSpectrum;
    public TransferDisplaySurface mTransfer;
    public PhaseDisplaySurface mPhase;
    public CoherenceDisplaySurface mCoherence;
    public boolean isRecording = false;
    private static Thread drawingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createEverything();


        Log.d(LOG_TAG, String.valueOf(2.4f%1f));

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
                    mGenerate.clearAudioBuffer();
                } else{
                    mGenerate.mShouldContinue = true;
                    mGenerate.begin();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording){
            mCollect.mShouldContinue = true;
            mCollect.startInputStream(MediaRecorder.AudioSource.MIC);
            mCollect.startRecording();
            beginDrawing();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRecording){
            mCollect.mShouldContinue = true;
            mCollect.startInputStream(MediaRecorder.AudioSource.MIC);
            mCollect.startRecording();
            beginDrawing();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCollect.mShouldContinue = false;
        mCollect.stopRecording();
        isRecording = false;
        mGenerate.stop();
        mGenerate.clearAudioBuffer();
    }

    private void createEverything(){
        Log.d(LOG_TAG, "Permissions acquired? " + String.valueOf(requestAudioPermissions()));
        mStart = findViewById(R.id.start_record_button);
        mGenerateButton = findViewById(R.id.pink_noise_button);
        mCollect = new AudioCollectTest();
        mGenerate = new Generator();
        mInMeter = findViewById(R.id.audio_in_meter_view);
        mInMeter.setAudioSource(mCollect);
        mOutMeter = findViewById(R.id.audio_out_meter_view);
        mOutMeter.setAudioSource(mGenerate);
        mSpectrum = findViewById(R.id.spectrum_view);
        mSpectrum.setInputSource(mCollect);
        mTransfer = findViewById(R.id.transfer_view);
        mTransfer.setInputSource(mCollect);
        mTransfer.setOutputSource(mGenerate);
        mPhase = findViewById(R.id.phase_view);
        mPhase.setInputSource(mCollect);
        mPhase.setOutputSource(mGenerate);
        mCoherence = findViewById(R.id.coherence_view);
        mCoherence.setInputSource(mCollect);
        mCoherence.setOutputSource(mGenerate);
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
                    mTransfer.drawAll();
                    mPhase.drawAll();
                    mCoherence.drawAll();
                    mInMeter.drawAll();
                    mOutMeter.drawAll();


//                    if (mSpectrum.averageDrawingTime() < 80 && mSpectrum.whenToDraw.size() > 3){
//                        try{
//                            drawingThread.sleep(200);
//                        }catch (InterruptedException ie){
//                            ie.printStackTrace();
//                        }
//                    }
                }
            }
        }, "Drawing Thread");
    }
}
