package com.louis.naturalnet.data;

import java.util.ArrayList;

public class QueueItem {

	public String packetId;
	public String path; // Keeps track of devices this packet has been passed through.
	public ArrayList<Long> delay =  new ArrayList<>();
	public String data;
	public int dataType;
	public long timestamp;

	// We could add the following constructor
    // public QueueItem(JSONObject packet) for received packets

    // And also
    // public QueueItem() for testing

    void addToPath(String MAC) {
        path += ',' + MAC;
    }

}
