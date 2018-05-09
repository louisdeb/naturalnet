package com.louis.naturalnet.data;

import java.util.ArrayList;

class QueueItem {

	String packetId;
	ArrayList<String> path =  new ArrayList<>(); // keeps track of devices this packet has been passed through
	ArrayList<Long> delay =  new ArrayList<>();
	String data;
	long timestamp;
	
}
