package com.aware.plugin.typingtrigger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aware.Aware;

public class TypingController {
    private static boolean isAwareRunning = false;
    public static void startAware(Context context) {
        if (!isAwareRunning) {
            Intent awareIntent = new Intent(context, Aware.class);
            context.startService(awareIntent);
            Aware.startAWARE(context);
            isAwareRunning = true;
            Log.d("TypingController","startAware(): Aware service started");
        }
    }
    public static void stopAware(Context context) {
        if (isAwareRunning) {
            Log.d("TypingController","TypingController:stopAware() called");
            context.stopService(new Intent(context, Aware.class));
            Aware.stopAWARE(context);
            isAwareRunning = false;
        }
    }
    public static boolean isRunning() {
        return isAwareRunning;
    }
}
