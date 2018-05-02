package com.louis.naturalnet.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BTManager {

    private Context context;

    private static BTController mBTController;
    private BTDeviceManager mBroadcastReceiver;

    public static final int REQUEST_BT_ENABLE = 1;
    public static final int REQUEST_BT_DISCOVERABLE = 11;
    public static int RESULT_BT_DISCOVERABLE_DURATION = 0;

    public BTManager(Context context) {
        this.context = context;
        mBroadcastReceiver = new BTDeviceManager(this, context);
    }

    public void registerBroadcastReceivers() {
        // Register the bluetooth BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mBroadcastReceiver, filter);
    }

    public void unregisterBroadcastReceivers() {
        context.unregisterReceiver(mBroadcastReceiver);
    }

    public void initBluetoothUtils(Activity activity) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            alertBluetoothNotSupported(activity);
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // Request the activity to start our BT adapter
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
        } else {
            // Start BT utils once we have an enabled BT adapter
            BTMessageHandler handler = new BTMessageHandler(context);
            mBTController = new BTController(handler, context);
            handler.setBTController(mBTController);
            mBTController.startBTServer();

            // Recreate our BT scanning alarm
            BTScanningAlarm.stopScanning(context);
            new BTScanningAlarm(context, mBTController);
        }
    }

    void connectToBTServer(BluetoothDevice device, long timeout) {
        mBTController.connectToBTServer(device, timeout);
    }

    private void alertBluetoothNotSupported(Activity activity) {
        Intent deviceBroadcastIntent = new Intent("com.louis.naturalnet.bluetooth.BTDeviceListener");
        deviceBroadcastIntent.putExtra("btSupported", false);
        activity.getBaseContext().sendBroadcast(deviceBroadcastIntent);
    }
}
