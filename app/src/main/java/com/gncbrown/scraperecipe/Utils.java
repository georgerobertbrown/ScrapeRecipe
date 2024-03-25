package com.gncbrown.scraperecipe;

import android.content.SharedPreferences;

public class Utils {
    private static String TAG = Utils.class.getName();
    public static SharedPreferences sharedPreferences;

    private static final String PREF_KEY_VOLUME = "volume";
    private static final String PREF_KEY_DELAY = "delay";

    public static int retrieveVolumeFromPreference() {
        int value = MainActivity.DEFAULT_VOLUME;
        try {
            value = MainActivity.sharedPreferences.getInt(PREF_KEY_VOLUME, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void saveVolumeToPreference(int value) {
        //Log.d(TAG, "saveVolumeToPreference, value=" + value);
        SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
        editor.putInt(PREF_KEY_VOLUME, value).apply();
    }

    public static int retrieveDelayFromPreference() {
        int value = MainActivity.DEFAULT_DELAY;
        try {
            value = MainActivity.sharedPreferences.getInt(PREF_KEY_DELAY, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void saveDelayToPreference(int value) {
        //Log.d(TAG, "saveDelayToPreference, value=" + value);
        SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
        editor.putInt(PREF_KEY_DELAY, value).apply();
    }
}
