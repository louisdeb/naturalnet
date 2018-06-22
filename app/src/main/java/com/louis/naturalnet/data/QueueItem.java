package com.louis.naturalnet.data;

import java.util.ArrayList;

public class QueueItem {

	public String packetId;
	public String path; // Keeps track of devices this packet has been passed through.
	public ArrayList<Long> delay =  new ArrayList<>();
	public String data;
	public int dataType;
	public long timestamp;

    void addToPath(String MAC) {
        path += ',' + MAC;
    }

}
