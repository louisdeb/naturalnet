package com.louis.naturalnet.bluetooth;

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

	private BTCom mBTHelper;

	BTController(Handler btHandler){
		// init bt utility
		mBTHelper = BTCom.getInstance();
        Messenger mMessenger = new Messenger(btHandler);
		mBTHelper.setCallback(mMessenger);
	}

	void startBTScan(long duration){
			mBTHelper.startScan(duration);
	}

	void startBTServer(){
		mBTHelper.startServer();
	}

	public void stopBTServer(){
		mBTHelper.stopServer();
	}

	void connectToBTServer(BluetoothDevice btDevice, long timeout){
		mBTHelper.connect(btDevice, timeout);
	}

	void sendToBTDevice(String mac, JSONObject data){
		mBTHelper.send(mac, data);
	}

	void sendToBTDevice(String mac, String data){
		mBTHelper.send(mac, data);
	}

	void stopConnection(String mac){
		mBTHelper.stopConnection(mac);
	}	
}
