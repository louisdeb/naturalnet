package com.louis.naturalnet.signal;

import android.telephony.PhoneStateListener;

public abstract class CellularSignalListener extends PhoneStateListener {
    public abstract void notifyDataEnabled(boolean enabled);
}
