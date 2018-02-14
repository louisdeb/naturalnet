package com.louis.naturalnet.utils;

public abstract class Constants {

	public static long SCAN_INTERVAL = 15000;
	public static long SCAN_DURATION = 10000;
	public static long DATA_RATE = 60000;

	public static final long SCAN_DURATION_SAVING = 5000;
	public static final long SCAN_DURATION_NORMAL = 10000;
	public static final long SCAN_INTERVAL_NORMAL = 15000;
	public static final long SCAN_INTERVAL_SAVING = 300000;
	
	public static double ENERGY_PENALTY_COEFF = 0.1;
	public static final double ENERGY_PENALTY_COEFF_ON = 0.1;

	public static String TAG_ACT_TEST = "test";
	
	public static final String DEFAULT_DEVICE_NAME = "OppNet:0:0";
	public static final int QUEUE_DIFF = 5;
	
	public static long BT_CLIENT_TIMEOUT = 5000;
	public static long BT_CLIENT_TIMEOUT_SAVING = 3000;

	public static long SENSOR_CONTACT_INTERVAL = 60000;
	public static long SINK_CONTACT_INTERVAL = 240000;

	public static String SP_KEY_BATTERY = "sp_battery_saving_mode";
	public static String SP_KEY_ENERGY = "sp_energy_awareness_mode";
	
	public static String INTENT_KEY_POSITION = "intent_key_position";
	
	// configure one's experiment id and application id
	public static String EXPERIMENT_ID = "58dd1015aa3208fc2229311f";//"58beba76d0010cb62fa01679";"58a19f2ac2f43c7c37d174cd";
	public static String APPLICATION_ID = "58dd128eaa3208fc22293741";//"58cfc1580922ba0a1a7d4546";
	public static String EXPERIMENTER_ID = "f3e1db7b-5f08-4cee-b715-939123fec017";//"86d7edce-5092-44c0-bed8-da4beaa3fbc6";
}
