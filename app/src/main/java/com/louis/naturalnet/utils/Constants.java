package com.louis.naturalnet.utils;

import java.util.UUID;

public abstract class Constants {

	public static long BT_SCAN_INTERVAL = 15000;
	public static long BT_SCAN_DURATION = 10000;

    // Time between location requests (millis)
    public static final long LOCATION_REQUEST_INTERVAL = 30000;
    public static final long LOCATION_REQUEST_INTERVAL_FASTEST = 2000;
	
	public static double ENERGY_PENALTY_COEFF = 0.1;

	public static final int QUEUE_MONITOR_INTERVAL = 10000;

	/* Bluetooth Communication Constants */

    public static String MAC;

    // What is the merit for a UUID such as this as opposed to something like 'NaturalNet'?
    public static UUID APP_UUID = UUID.fromString("8113ac40-438f-11e1-b86c-0800200c9a60");

    // Service name for the socket we create.
    public static final String BTSocketServiceName = "NaturalNet";

    // Connection timeouts.
    public static long BT_HANDSHAKE_TIMEOUT = 5000;

    // Status codes.
    public final static int BT_DATA_RECEIVED = 10403;

    // Data labels used when communicating with the local Messenger (BTMessageHandler).
    public final static String BT_DATA_CONTENT = "bt_data"; // Data received from another device.
    public final static String BT_DEVICE_MAC = "bt_device_mac"; // MAC address of the communicating device (us or them).
}
