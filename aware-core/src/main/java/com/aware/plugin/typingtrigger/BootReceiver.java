package com.aware.plugin.typingtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Log.d("BootReceiver", "BOOT_COMPLETED received");
        Log.d("AWARE::BootReceiver", "BootReceiver:onReceive() run");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("ACTION_AWARE_KEYBOARD");

        context.getApplicationContext()
                .registerReceiver(new TypingTriggerReceiver(), filter);

         */


    }
}
