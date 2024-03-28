package com.gncbrown.scraperecipe;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final String TAG = "SettingsActivity";
    public static Context context;

    private TextView volumeLabelView;
    private TextView delayLabelView;
    private int currentVolume;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        context = this;

        Log.d(TAG, "onCreate.setContentView");
        setContentView(R.layout.settings_activity);
        //audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        currentVolume = MainActivity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int savedVolume = Utils.retrieveVolumeFromPreference();

        volumeLabelView = findViewById(R.id.volumeLabel);
        SeekBar volumeSeekBarView = findViewById(R.id.volumeSeekBar);
        volumeSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                volumeLabelView.setText(String.format("%s(%s) ", context.getResources().getString(R.string.volumeLabel), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int seekValue = seekBar.getProgress();
                Log.d(TAG, "volumeSeekBar.onStopTrackingTouch seekValue=" + seekValue);

                volumeLabelView.setText(String.format("%s(%s) ", context.getResources().getString(R.string.volumeLabel), seekValue));
                Utils.saveVolumeToPreference(seekValue);
                MainActivity.speak(String.format("Volume level %s", seekValue));
            }
        });
        volumeSeekBarView.setMax(MainActivity.audioMax);
        volumeSeekBarView.setProgress(savedVolume);


        delayLabelView = findViewById(R.id.delayLabel);
        SeekBar delaySeekBarView = findViewById(R.id.delaySeekBar);
        delaySeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                delayLabelView.setText(String.format("%s(%s) ", context.getResources().getString(R.string.delayLabel), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int seekValue = seekBar.getProgress();
                Log.d(TAG, "delaySeekBar.onStopTrackingTouch seekValue=" + seekValue);

                delayLabelView.setText(String.format("%s(%s ms) ", context.getResources().getString(R.string.delayLabel), seekValue));
                Utils.saveDelayToPreference(seekValue);
                MainActivity.speak(String.format("Delay %s milliseconds", seekValue));
            }
        });
        delaySeekBarView.setMax(MainActivity.DELAY_MAX);
        delaySeekBarView.setProgress(Utils.retrieveDelayFromPreference());
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy restore volume to " + currentVolume);
        if (MainActivity.audioManager != null)
            MainActivity.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

        super.onDestroy();
    }
}
