package com.louis.naturalnet.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

public class BTManager {

    private Context context;

    private static BTController mBTController;
    private BTServiceBroadcastReceiver mBroadcastReceiver;

    public static final int REQUEST_BT_ENABLE = 1;
    public static final int REQUEST_BT_DISCOVERABLE = 11;
    public static int RESULT_BT_DISCOVERABLE_DURATION = 0;

    public BTManager(Context _context) {
        context = _context;
        mBroadcastReceiver = new BTServiceBroadcastReceiver(this);
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
            // We may want to inform the activity of this and perform some UI actions based on it

            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // Request the activity to start our BT adapter
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
        } else {
            // Start BT utils once we have an enabled BT adapter
            BTServiceHandler handler = new BTServiceHandler(mBTController, context);
            mBTController = new BTController(handler);
            mBTController.startBTServer();

            // Recreate our BT scanning alarm
            BTScanningAlarm.stopScanning(context);
            new BTScanningAlarm(context, mBTController);
        }
    }

    void connectBTServer(BluetoothDevice device, long timeout) {
        mBTController.connectBTServer(device, timeout);
    }
}
