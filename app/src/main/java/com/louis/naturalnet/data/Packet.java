package com.louis.naturalnet.data;

// BT Message Header
public abstract class Packet {

	// Format:  type|data
	public static final String TYPE = "type";
	public static final String ID = "id";
	public static final String PATH = "path";
	public static final String DATA = "data";
	public static final String DELAY = "delay";
	public static final String TIMESTAMP = "timestamp";

	public static final int TYPE_WARNING = 104;
	public static final int TYPE_WARNING_ACK = 105;
}
