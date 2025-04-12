package com.aware.plugin.typingtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("ACTION_AWARE_KEYBOARD_KEY");

        context.getApplicationContext()
                .registerReceiver(new TypingTriggerReceiver(), filter);
    }
}
