package com.louis.naturalnet.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class MessageReceiver extends BroadcastReceiver {

    @Override
    public abstract void onReceive(Context context, Intent intent);

}
