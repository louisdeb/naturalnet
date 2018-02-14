package com.louis.naturalnet.data;

import java.util.ArrayList;

public class QueueItem {

	public String packetId;
	public ArrayList<String> path =  new ArrayList<String>();
	public ArrayList<Long> delay =  new ArrayList<Long>();
	public String data = null;
	public long timestamp;
	
}
