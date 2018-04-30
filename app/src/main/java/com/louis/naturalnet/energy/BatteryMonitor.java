package com.louis.naturalnet.energy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryMonitor {

    private final static String TAG = "BatteryMonitor";
	private static BatteryMonitor _this = null;
    private Context context;

    private BatteryMonitor(Context context) {
        this.context = context;
    }

    public static BatteryMonitor getInstance(Context context) {
		if(_this == null)
			_this = new BatteryMonitor(context);

		return _this;
	}

	public int getBatteryLevel() {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int batteryPct;
		int batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int batteryScale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if(batteryLevel == -1 || batteryScale == -1) {
			Log.d(TAG, "wrong level");
			batteryPct = 50;
		} else {
			batteryPct = (int) (((float)batteryLevel / (float)batteryScale) * 100.0f);
		}

		return batteryPct;
	}

}
