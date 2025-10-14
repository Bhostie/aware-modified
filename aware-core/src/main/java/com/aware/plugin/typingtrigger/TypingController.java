package com.aware.plugin.typingtrigger;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.aware.Aware;

public class TypingController {
    private static boolean isAwareRunning = false;
    public static synchronized void startAware(Context context) {
        Context appCtx = context.getApplicationContext();
        if (!isAwareRunning) {
            Intent awareIntent = new Intent(appCtx, Aware.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(awareIntent);
                } else {
                    appCtx.startService(awareIntent);
                }
            } catch (IllegalStateException ex) {
                Log.w("TypingController", "startAware(): fallback to startService due to state=" + ex.getMessage());
                appCtx.startService(awareIntent);
            }
            Aware.startAWARE(appCtx);
            isAwareRunning = true;
            Log.d("TypingController","startAware(): Aware service started");
        }
    }
    public static synchronized void stopAware(Context context) {
        Context appCtx = context.getApplicationContext();
        if (isAwareRunning) {
            Log.d("TypingController","TypingController:stopAware() called");
            try {
                appCtx.stopService(new Intent(appCtx, Aware.class));
            } catch (Exception ignore) { }
            Aware.stopAWARE(appCtx);
            isAwareRunning = false;
        }
    }
    public static boolean isRunning() {
        return isAwareRunning;
    }
}
