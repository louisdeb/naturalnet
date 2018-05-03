package com.louis.naturalnet.bluetooth;

import android.content.Context;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Messenger;

/**
 * controller to manage interface of bluetooth
 * @author fshi
 *
 */
public class BTController {

	private BTCom btComms;

	BTController(Context context) {
		btComms = BTCom.getInstance(context);
	}

	void startBTScan(long duration) {
		btComms.startScan(duration);
	}

	void startBTServer() {
		btComms.startServer();
	}

	public void stopBTServer() {
		btComms.stopServer();
	}

	void connectToBTServer(BluetoothDevice device, long timeout) {
		btComms.connect(device, timeout);
	}

	void sendToBTDevice(String mac, JSONObject data) {
		btComms.send(mac, data);
	}

	void sendToBTDevice(String mac, String data) {
		btComms.send(mac, data);
	}

	void stopConnection(String mac) {
		btComms.stopConnection(mac);
	}	
}
