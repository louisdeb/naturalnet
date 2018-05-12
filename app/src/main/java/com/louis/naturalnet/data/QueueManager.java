package com.louis.naturalnet.data;

import java.util.ArrayList;
import java.util.Random;

public class QueueManager {

	private static String DEVICE_ID = "TEST_DEVICE_ID";

	private static volatile ArrayList<QueueItem> queue = new ArrayList<>();

	public int packetsReceived = 0;
	public int packetsSent = 0;

	private static QueueManager _this = null;

	private QueueManager() {}

	public static QueueManager getInstance() {
		if (_this == null)
            _this = new QueueManager();

		return _this;
	}

	// Create a warning and add it to the queue. Creates a test warning so we can study propagation of the packet
    // through the network.
	public Warning generateWarning() {
        QueueItem item = new QueueItem();
        Warning warning = new Warning();

        item.packetId = DEVICE_ID + String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
        item.path.add(DEVICE_ID);
        item.data = warning.toString();
        item.dataType = "warning";
        item.timestamp = System.currentTimeMillis();

        queue.add(item);

        return warning;
    }

    public QueueItem getFirstFromQueue() {
	    return queue.get(0);
    }

	// Called by BTMessageHandler when it receives a packet (of data). This is so that we can send the data on to another
    // device.
	public void appendToQueue(String packetId, String path, String data, String delay) {
        QueueItem qItem = new QueueItem();

        String[] IDs = path.split(",");
        String[] delays = delay.split(",");
        boolean hasLoop = false;

        for (int i = 0; i < IDs.length; i++) {
            if (IDs[i].equalsIgnoreCase(DEVICE_ID)) {
                hasLoop = true;
                break;
            }
            qItem.path.add(IDs[i]);
            qItem.delay.add(Long.parseLong(delays[i]));
        }

        if (!hasLoop) {
            qItem.path.add(DEVICE_ID);
            qItem.packetId = packetId;
            qItem.data = data;
            qItem.timestamp = System.currentTimeMillis();
            queue.add(qItem);
        }
    }

	public synchronized int getQueueLength() {
		return queue.size();
	}
}
