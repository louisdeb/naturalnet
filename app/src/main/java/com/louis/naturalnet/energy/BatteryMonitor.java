package com.louis.naturalnet.energy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryMonitor {

	private int batteryLevel;
	private int batteryScale;
	private int batteryPct;

	private static BatteryMonitor _obj = null;
	
	private Context mContext;

	public static BatteryMonitor getInstance(Context context){
		if(_obj == null){
			_obj = new BatteryMonitor(context);
		}
		return _obj;
	}

	public int getBatteryLevel(){
		Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		batteryScale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if(batteryLevel == -1 || batteryScale == -1) {
			Log.d(TAG, "wrong level");
			batteryPct = 50;
		}
		else{
			batteryPct = (int)(((float)batteryLevel / (float)batteryScale) * 100.0f);
		}
		return batteryPct;
	}

	private BatteryMonitor(Context context){
		mContext = context;
	} 

	private final static String TAG = "BatteryMonitor";

}
