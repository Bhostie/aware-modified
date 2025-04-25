package com.aware.plugin.typingtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aware.Aware;

public class TypingTriggerReceiver extends BroadcastReceiver {

    private static final long INACTIVITY_TIMEOUT_MS = 30000; // 30 seconds
    private static final long SCREEN_OFF_TIMEOUT_MS = 5000; // 5 seconds
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable stopAwareRunnable = new Runnable() {
        @Override
        public void run() {
            TypingController.stopAware(contextRef);
        }
    };

    private static Context contextRef;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.d("TypingTriggerReceiver", "onReceive(): " + intent.getAction());
        }
        contextRef = context; // keep a reference for handler use
        String action = intent.getAction();

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            handler.removeCallbacks(stopAwareRunnable);
            handler.postDelayed(stopAwareRunnable, SCREEN_OFF_TIMEOUT_MS);

        } else if (Intent.ACTION_SCREEN_ON.equals(action) ||
                "ACTION_AWARE_KEYBOARD".equals(action)) {

            TypingController.startAware(context);

            handler.removeCallbacks(stopAwareRunnable);
            handler.postDelayed(stopAwareRunnable, INACTIVITY_TIMEOUT_MS);
        }
    }
}

