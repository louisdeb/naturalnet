package com.louis.naturalnet.data;

import java.util.ArrayList;
import java.util.Random;

import android.util.Log;

public class QueueManager {

	private final static String TAG = "QueueManager";

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
	public void generateWarning() {
        QueueItem item = new QueueItem();
        Warning warning = new Warning();

        item.packetId = DEVICE_ID + String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
        item.path.add(DEVICE_ID);
        item.data = warning.toString();
        item.timestamp = System.currentTimeMillis();

        queue.add(item);
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

    // Called by the ClientConnectedTask in BTMessageHandler, which we no longer use.
    // Get one packet from the queue, where the packet doesn't have a loop.
	public synchronized String[] getFromQueue(String id) {
		String[] data = new String[4];

		if (queue.size() > 0) {
			int q;

			for(q=0; q<queue.size(); q++){
				QueueItem qItem = queue.get(q);
				boolean hasLoop = false;
				StringBuffer sb = new StringBuffer();

				for (int i=0; i< qItem.path.size(); i++) {
					if (qItem.path.get(i).equalsIgnoreCase(id)) { // has loop
						hasLoop = true;
						break;
					}

					sb.append(qItem.path.get(i));

					if (i != qItem.path.size() - 1)
						sb.append(",");
				}
				
				if (hasLoop) {
					Log.d(TAG, "found loop continue");
					continue;
				}

				data[0] = sb.toString(); // path

				sb = new StringBuffer();

				for (int i=0; i< qItem.delay.size(); i++) {
					sb.append(qItem.delay.get(i));
					sb.append(",");
				}

				sb.append(System.currentTimeMillis() - qItem.timestamp);

				data[3] = sb.toString(); // delay

				data[1] = qItem.data; // data
				data[2] = qItem.packetId; // id
				break;
			}

			if (q<queue.size())
				queue.remove(q);
		}
		return data;
	}

	public synchronized int getQueueLength() {
		return queue.size();
	}
}
