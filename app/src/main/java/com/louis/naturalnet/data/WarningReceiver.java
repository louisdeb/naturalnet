package com.louis.naturalnet.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class WarningReceiver extends BroadcastReceiver {

    @Override
    public abstract void onReceive(Context context, Intent intent);

}