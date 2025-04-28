package com.aware.plugin.typingtrigger;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.aware.utils.Aware_Plugin;

public class Typing_Plugin extends Aware_Plugin {
    private static final String TAG = "AWARE::TypingTrigger";
    private TypingTriggerReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        // — register exactly once here
        mReceiver = new TypingTriggerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("ACTION_AWARE_KEYBOARD");
        registerReceiver(mReceiver, filter);
        Log.d(TAG, "onCreate(): receiver registered");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand(): " + intent);
        // keeps your plugin alive across kills
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
        Log.d(TAG, "onDestroy(): receiver unregistered");
    }
}
