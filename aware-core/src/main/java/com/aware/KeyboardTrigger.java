package com.aware;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.aware.plugin.typingtrigger.TypingTriggerReceiver;
import com.aware.utils.Aware_Sensor;

public class KeyboardTrigger extends Aware_Sensor {

    private TypingTriggerReceiver typingTriggerReceiver;
    private FrameLayout keyboardDetectionView;
    private WindowManager windowManager;
    private boolean wasKeyboardOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        TAG = "AWARE::KeyboardTrigger";

        if (Aware.DEBUG) Log.d(TAG, "KeyboardTrigger service created!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(PERMISSIONS_OK){
            if (Aware.DEBUG) Log.d(TAG, "KeyboardTrigger starting...");
            Aware.setSetting(this, Aware_Preferences.STATUS_KEYBOARD_TRIGGER, true);

            if (typingTriggerReceiver == null) {
                typingTriggerReceiver = new TypingTriggerReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                filter.addAction(Keyboard.ACTION_AWARE_KEYBOARD);
                registerReceiver(typingTriggerReceiver, filter);


                if (Aware.DEBUG) Log.d(TAG, "TypingTriggerReceiver registered.");
            }

            // Setup lightweight keyboard detection
            //setupKeyboardDetector();

        }



        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (typingTriggerReceiver != null) {
            // Clear any pending callbacks to avoid delayed stop after destroy
            typingTriggerReceiver.clearCallbacks();
            unregisterReceiver(typingTriggerReceiver);
            typingTriggerReceiver = null;
            if (Aware.DEBUG) Log.d(TAG, "TypingTriggerReceiver unregistered.");
        }

        if (windowManager != null && keyboardDetectionView != null) {
            windowManager.removeView(keyboardDetectionView);
            keyboardDetectionView = null;
            if (Aware.DEBUG) Log.d(TAG, "Keyboard detection view removed.");
        }

        if (Aware.DEBUG) Log.d(TAG, "KeyboardTrigger destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
