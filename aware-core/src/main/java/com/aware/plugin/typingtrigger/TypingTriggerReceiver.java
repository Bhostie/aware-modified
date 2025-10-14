package com.aware.plugin.typingtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aware.Keyboard;

public class TypingTriggerReceiver extends BroadcastReceiver {

    private static final long INACTIVITY_TIMEOUT_MS = 30000; // 30 seconds
    private static final long SCREEN_OFF_TIMEOUT_MS = 5000; // 5 seconds

    // Instance-scoped handler to avoid static leaks
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable stopAwareRunnable = new Runnable() {
        @Override
        public void run() {
            // The receiver is registered by a Service; use app context to avoid leaking the receiver's context
            if (lastAppContext != null) {
                TypingController.stopAware(lastAppContext);
            }
        }
    };

    private Context lastAppContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.d("TypingTriggerReceiver", "onReceive(): " + intent.getAction());
        }

        // Keep latest application context reference for the delayed runnable
        lastAppContext = context.getApplicationContext();
        final String action = (intent != null ? intent.getAction() : null);

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            handler.removeCallbacks(stopAwareRunnable);
            handler.postDelayed(stopAwareRunnable, SCREEN_OFF_TIMEOUT_MS);

        } else if (Intent.ACTION_SCREEN_ON.equals(action) ||
                Keyboard.ACTION_AWARE_KEYBOARD.equals(action)) {

            TypingController.startAware(lastAppContext);

            handler.removeCallbacks(stopAwareRunnable);
            handler.postDelayed(stopAwareRunnable, INACTIVITY_TIMEOUT_MS);
        }
    }

    public void clearCallbacks() {
        handler.removeCallbacksAndMessages(null);
    }
}
