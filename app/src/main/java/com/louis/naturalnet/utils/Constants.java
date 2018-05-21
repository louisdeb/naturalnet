package com.louis.naturalnet.utils;

import java.util.UUID;

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

	public static final int QUEUE_DIFF = 5;

	public static final int QUEUE_MONITOR_INTERVAL = 10000;

	public static final double EARTH_RADIUS = 6371.01 * 1000;

	/* Bluetooth Communication Constants */

    public static String MAC;

    // What is the merit for a UUID such as this as opposed to something like 'NaturalNet'?
    public static UUID APP_UUID = UUID.fromString("8113ac40-438f-11e1-b86c-0800200c9a60");

    // Service name for the socket we create.
    public static final String BTSocketServiceName = "NaturalNet";

    // Connection timeouts.
    public static long BT_HANDSHAKE_TIMEOUT = 5000;
    public static long BT_CLIENT_TIMEOUT = 5000;
    public static long BT_CLIENT_TIMEOUT_SAVING = 3000;

    // Status codes.
    public final static int BT_CLIENT_CONNECTED = 10401;
    public final static int BT_SERVER_CONNECTED = 10402;
    public final static int BT_DATA_RECEIVED = 10403;
    public final static int BT_CLIENT_CONNECT_FAILED = 10404;
    public final static int BT_CLIENT_ALREADY_CONNECTED = 10405;
    public final static int BT_DISCONNECTED = 10406;
    public final static int BT_SUCCESS = 10407;

    // Data labels used when communicating with the local Messenger (BTMessageHandler).
    public final static String BT_DATA_CONTENT = "bt_data"; // Data received from another device.
    public final static String BT_DEVICE_MAC = "bt_device_mac"; // MAC address of the communicating device (us or them).
}
