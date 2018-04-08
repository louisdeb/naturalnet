package com.louis.naturalnet.signal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
    To receive location updates, register an instance of this class using Activity.registerReceiver
    with the IntentFilter = new IntentFilter("com.louis.naturalnet.signal.LocationReceiver")
 */

public abstract class LocationReceiver extends BroadcastReceiver {

    @Override
    public abstract void onReceive(Context context, Intent intent);

}
