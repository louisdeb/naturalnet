package com.louis.naturalnet.signal;

import android.app.Activity;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CellularSignalReceiver {

    private static CellularSignalReceiver _this;
    private static String TAG = "CellularSignalReceiver";
    private static TelephonyManager telephonyManager;

    private CellularSignalReceiver(Activity activity) {
        telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        Log.d(TAG, "Created class & Telephony Manager");
    }

    public static void addListener(Activity activity, CellularSignalListener listener) {
        if (_this == null) {
            _this = new CellularSignalReceiver(activity);
        }

        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

}
