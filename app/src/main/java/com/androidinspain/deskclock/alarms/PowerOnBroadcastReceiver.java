package com.androidinspain.deskclock.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by roberto on 13/04/17.
 */
public class PowerOnBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_POWER_ON = "com.android.deskclock.REQUEST_POWER_ON";
    private String TAG = getClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(ACTION_POWER_ON)) {
            Log.d(TAG, "ACTION_POWER_ON!");
        }
    }
}
