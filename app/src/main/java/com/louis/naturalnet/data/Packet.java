package com.louis.naturalnet.data;

// BT Message Header
public abstract class Packet {

	// Format:  type|data
	public static final String PACKET_TYPE = "type";
	public static final String PACKET_ID = "id";
	public static final String PACKET_PATH = "path";
	public static final String PACKET_DATA = "data";
	public static final String PACKET_DELAY = "delay";
	
	// Data Type Identifier
	public static final int PACKET_TYPE_DATA = 102;
	public static final int PACKET_TYPE_DATA_ACK = 103;

	public static final int PACKET_TYPE_WARNING = 104;
	public static final int PACKET_TYPE_WARNING_ACK = 105;
}
