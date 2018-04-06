package com.louis.naturalnet.signal;

import android.telephony.PhoneStateListener;

public abstract class CellularSignalListener extends PhoneStateListener {
    // The following function is removed since TelephonyManager.isDataEnabled() requires
    // API level 26. We are currently using API level 21.
    // This class can remain in case we find any further abstract method we would like to implement.
    // public abstract void notifyDataEnabled(boolean enabled);
}
