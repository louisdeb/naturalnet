package com.louis.naturalnet.data;

import android.util.Log;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class QueueManager {

	private static volatile ArrayList<QueueItem> queue = new ArrayList<>();

	private static QueueManager _this = null;

	private QueueManager() {}

	public static QueueManager getInstance() {
		if (_this == null)
            _this = new QueueManager();

		return _this;
	}

    public QueueItem getFirstFromQueue() {
        return queue.get(0);
    }

    public synchronized int getQueueLength() {
        return queue.size();
    }

	// Create a warning and add it to the queue. Creates a test warning so we can study propagation of the packet
    // through the network.
	public Warning generateWarning() {
        QueueItem item = new QueueItem();
        Warning warning = new Warning();

        item.packetId = Constants.MAC + String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
        item.path = Constants.MAC;
        item.data = warning.toString();
        item.dataType = Packet.TYPE_WARNING;
        item.timestamp = System.currentTimeMillis();

        queue.add(item);

        return warning;
    }

    public void addPacketToQueue(JSONObject packet) throws JSONException {
        QueueItem item = new QueueItem();

        Log.d("QueueManager", "Adding packet to queue: " + packet.toString());

        item.packetId = packet.getString(Packet.ID);
        item.path = packet.getString(Packet.PATH);
        item.data = packet.getString(Packet.DATA);
        item.dataType = packet.getInt(Packet.TYPE);
        item.timestamp = packet.getLong(Packet.TIMESTAMP);

        // Add our MAC address to the packet's path.
        item.addToPath(Constants.MAC);

        queue.add(item);
    }

    public void removeFromQueue(int warningId) {
	    Log.d("QueueManager", "Removing packet from queue");
	    for (QueueItem item : queue) {
	        try {
                Warning warning = new Warning(new JSONObject(item.data));
                if (warning.warningId == warningId) {
                    queue.remove(item);
                    Log.d("QueueManager", "Removed");
                }
            } catch (JSONException e) {
	            // Data was not a warning item.
                e.printStackTrace();
            }
        }
    }
}
