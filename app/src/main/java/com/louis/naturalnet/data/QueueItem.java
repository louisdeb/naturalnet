package com.louis.naturalnet.data;

import java.util.ArrayList;

public class QueueItem {

	public String packetId;
	public ArrayList<String> path =  new ArrayList<>();
	public ArrayList<Long> delay =  new ArrayList<>();
	public String data = null;
	public long timestamp;
	
}
