package com.louis.naturalnet.data;

import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.sensor.TemperatureSensorListener;

import java.util.ArrayList;
import java.util.Random;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

public class QueueManager {

	private final static String TAG = "QueueManager";

	public static String ID;

	private static volatile ArrayList<QueueItem> queue = new ArrayList<>();

	public long sensorTimestamp = 0;
	public long sinkTimestamp = 0;

	public int packetsReceived = 0;
	public int packetsSent = 0;

	public int contacts = 0;
	public int peers = 0;

	// queue is a json with format {sequence of ids : content}

	private static QueueManager qManager = null;

	private Context mContext;

	private QueueManager(Context context){
		mContext = context;
		// for test purpose, init queue content
		ID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		
		Log.d(TAG, "init queue");
		updateName();
		
		start(context);
	}

	public static QueueManager getInstance(Context context){
		if(qManager == null){
			qManager = new QueueManager(context);
		}
		return qManager;
	}

	public void stop(Context context){
		QueueGenerationAlarm.stopGenerating(context);
	}
	
	public void start(Context context){
		QueueGenerationAlarm.stopGenerating(context);
		new QueueGenerationAlarm(context);
	}
	
//	private void initQueue(){
//		Random rand = new Random();
//		for(int i=0; i<rand.nextInt(200); i++){
//			StringBuffer sb = new StringBuffer();
//			sb.append(20 + Math.random() - 0.5);
//			QueueItem qItem = new QueueItem();
//			qItem.packetId = ID + ":" + String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
//			qItem.path.add(ID);
//			qItem.data = sb.toString();
//			qItem.timestamp = System.currentTimeMillis();
//			queue.add(qItem);
//		}
//		updateName();
//	}
	
	public void generateData(){
		QueueItem qItem = new QueueItem();
		qItem.packetId = ID + ":" + String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));
		qItem.path.add(ID);
		qItem.data = String.valueOf(TemperatureSensorListener.getInstance(mContext).getSensorValue());
		qItem.timestamp = System.currentTimeMillis();
		queue.add(qItem);
		updateName();
	}

	public void appendToQueue(String packetId, String path, String data, String delay){

		QueueItem qItem = new QueueItem();

		String[] IDs = path.split(",");
		String[] delays = delay.split(",");
		boolean hasLoop = false;
		for(int i=0; i<IDs.length; i++){
			if(IDs[i].equalsIgnoreCase(ID)){
				hasLoop = true;
				break;
			}
			qItem.path.add(IDs[i]);
			qItem.delay.add(Long.parseLong(delays[i]));
		}
		if(!hasLoop){
			qItem.path.add(ID);
			qItem.packetId = packetId;
			qItem.data = data;
			qItem.timestamp = System.currentTimeMillis();
			queue.add(qItem);
		}
	}

	// format: ID1,ID2,ID3 && data

	//	public String[] getFromQueue(int length, String MAC){
	//		int peerID = Devices.PARTICIPATING_DEVICES_ID.get(MAC);
	//		String[] data = new String[3];
	//		ArrayList<QueueItem> newQueue = new ArrayList<QueueItem>();
	//		for(QueueItem qItem: queue){
	//			if(length > 0){
	//				StringBuffer sb = new StringBuffer();
	//				boolean hasLoop = false;
	//				for(int i=0; i< qItem.path.size(); i++){
	//					if(peerID == qItem.path.get(i)){ // loop detection
	//						hasLoop = true;
	//					}
	//					sb.append(qItem.path.get(i));
	//					if(i != qItem.path.size() - 1){
	//						sb.append(",");
	//					}
	//				}
	//				if(hasLoop){
	//					newQueue.add(qItem);
	//				}
	//				else{
	//					data[0] = sb.toString();
	//					data[1] = qItem.data;
	//					data[2] = qItem.packetId;
	//					length -= 1;
	//				}
	//			}else{
	//				newQueue.add(qItem);
	//			}
	//		}
	//		queue = newQueue;
	//		updateBluetoothDeviceName();
	//		return data;
	//	}

	public synchronized String[] getFromQueue(){ // get the first one from the queue
		String[] data = new String[4];
		if(queue.size() > 0){
			StringBuffer sb = new StringBuffer();

			QueueItem qItem = queue.get(0);

			for(int i=0; i< qItem.path.size(); i++){
				sb.append(qItem.path.get(i));
				if(i != qItem.path.size() - 1){
					sb.append(",");
				}
			}

			data[0] = sb.toString(); // path

			sb = new StringBuffer();

			for(int i=0; i< qItem.delay.size(); i++){
				sb.append(qItem.delay.get(i));
				sb.append(",");
			}

			sb.append(System.currentTimeMillis() - qItem.timestamp);

			data[3] = sb.toString(); // delay

			data[1] = qItem.data; // data
			data[2] = qItem.packetId; // id
			queue.remove(0);
		}
		return data;
	}
	
	public synchronized String[] getFromQueue(String id){ // get one packet from the queue without loop
		String[] data = new String[4];
		if(queue.size() > 0){
			int q;
			for(q=0; q<queue.size(); q++){
				QueueItem qItem = queue.get(q);

				boolean hasLoop = false;
				
				StringBuffer sb = new StringBuffer();
				for(int i=0; i< qItem.path.size(); i++){
					if(qItem.path.get(i).equalsIgnoreCase(id)){ // has loop
						hasLoop = true;
						break;
					}
					sb.append(qItem.path.get(i));
					if(i != qItem.path.size() - 1){
						sb.append(",");
					}
				}
				
				if(hasLoop){
					Log.d(TAG, "found loop continue");
					continue;
				}

				data[0] = sb.toString(); // path

				sb = new StringBuffer();

				for(int i=0; i< qItem.delay.size(); i++){
					sb.append(qItem.delay.get(i));
					sb.append(",");
				}

				sb.append(System.currentTimeMillis() - qItem.timestamp);

				data[3] = sb.toString(); // delay

				data[1] = qItem.data; // data
				data[2] = qItem.packetId; // id
				break;
			}
			if(q<queue.size()){
				queue.remove(q);
			}
		}
		return data;
	}

	public synchronized int getQueueLength(){
		Log.d(TAG, "queue len is " + String.valueOf(queue.size()));
		
		int queueSize = queue.size();
		//		for(QueueItem qItem: queue){
		//			if(qItem.path.size() > 1){
		//				Log.d(TAG, qItem.packetId + "@" + qItem.path.toString() + "@" + qItem.delay.toString() + ":" + qItem.data);
		//			}
		//		}
		if(queueSize == 0){
//			initQueue();
		}

		return queueSize;
	}
	
	public void updateName() {
		StringBuilder newName = new StringBuilder();
		newName.append("OppNet");
		newName.append(":");
		newName.append("R");
		newName.append(":");
		newName.append(ID);
		newName.append(":");
		newName.append(queue.size());
		newName.append(":");
		newName.append(BatteryMonitor.getInstance(mContext).getBatteryLevel());

		BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
		if (BTAdapter != null)
			BTAdapter.setName(newName.toString());

		Log.d(TAG, "update name to " + newName.toString());
	}
}
