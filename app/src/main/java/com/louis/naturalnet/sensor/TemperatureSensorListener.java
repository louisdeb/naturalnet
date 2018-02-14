package com.louis.naturalnet.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class TemperatureSensorListener implements SensorEventListener {

	private final SensorManager mSensorManager;
	private final Sensor mTempSensor;
	private static TemperatureSensorListener _obj = null;

	private final String TAG = "TempSensorListener";

	private double sensorValue; // lux light
	
	private TemperatureSensorListener(Context context){
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	}

	public void start(){
		boolean re = mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
		Log.d(TAG, String.valueOf(re));
	}
	
	public void stop(){
		mSensorManager.unregisterListener(this);
	}
	
	public static TemperatureSensorListener getInstance(Context context){
		if(_obj == null){
			_obj = new TemperatureSensorListener(context);
		}
		return _obj;
	}
	
	public double getSensorValue(){
		return sensorValue;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		sensorValue = event.values[0];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
