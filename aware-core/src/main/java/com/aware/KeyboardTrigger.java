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
                filter.addAction("ACTION_AWARE_KEYBOARD");
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

    private void setupKeyboardDetector() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        if (keyboardDetectionView == null) {
            keyboardDetectionView = new FrameLayout(this);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    0, 0,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    android.graphics.PixelFormat.TRANSLUCENT
            );

            try {
                windowManager.addView(keyboardDetectionView, params);

                keyboardDetectionView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect r = new Rect();
                        keyboardDetectionView.getWindowVisibleDisplayFrame(r);
                        int screenHeight = keyboardDetectionView.getRootView().getHeight();
                        int keypadHeight = screenHeight - r.bottom;

                        boolean isKeyboardOpen = keypadHeight > screenHeight * 0.15; // 15% of screen

                        if (isKeyboardOpen && !wasKeyboardOpen) {
                            wasKeyboardOpen = true;
                            if (Aware.DEBUG) Log.d(TAG, "Keyboard detected as opened.");

                            // Broadcast ACTION_AWARE_KEYBOARD
                            Intent keyboardIntent = new Intent("ACTION_AWARE_KEYBOARD");
                            sendBroadcast(keyboardIntent);

                        } else if (!isKeyboardOpen && wasKeyboardOpen) {
                            wasKeyboardOpen = false;
                            if (Aware.DEBUG) Log.d(TAG, "Keyboard detected as closed.");
                            // Optional: can do something on keyboard close
                        }
                    }
                });

                if (Aware.DEBUG) Log.d(TAG, "Keyboard detection view set up.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to add keyboard detection view: " + e.getMessage());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
