package com.louis.naturalnet.data;

import java.util.ArrayList;

public class QueueItem {

	public String packetId;
	public ArrayList<String> path =  new ArrayList<>(); // keeps track of devices this packet has been passed through
	public ArrayList<Long> delay =  new ArrayList<>();
	public String data;
	public String dataType;
	public long timestamp;
	
}
