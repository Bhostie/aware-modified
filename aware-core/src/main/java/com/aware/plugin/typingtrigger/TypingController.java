package com.aware.plugin.typingtrigger;

import android.content.Context;
import android.content.Intent;
import com.aware.Aware;

public class TypingController {

    private static boolean isAwareRunning = false;

    public static void startAware(Context context) {
        if (!isAwareRunning) {
            Intent awareIntent = new Intent(context, Aware.class);
            context.startService(awareIntent);
            isAwareRunning = true;
        }
    }

    public static void stopAware(Context context) {
        if (isAwareRunning) {
            context.stopService(new Intent(context, Aware.class));
            isAwareRunning = false;
        }
    }

    public static boolean isRunning() {
        return isAwareRunning;
    }
}
