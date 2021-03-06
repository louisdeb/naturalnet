package com.louis.naturalnet.bluetooth;

import android.content.Context;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;

/**
 * controller to manage interface of bluetooth
 * @author fshi
 *
 */
class BTController {

	private BTCom btComm;

	BTController(Context context) {
		btComm = BTCom.getInstance(context);
	}

	void startBTScan(long duration) {
		btComm.startScan(duration);
	}

	void startBTServer() {
		btComm.startServer();
	}

	void connectToBTServer(BluetoothDevice device, long timeout) {
		btComm.connect(device, timeout);
	}

	void sendToBTDevice(String mac, JSONObject data) {
		btComm.send(mac, data);
	}

}
