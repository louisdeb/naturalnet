package com.louis.naturalnet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.utils.Utils;

import java.util.ArrayList;

/*
    This class handles discovering and saving bluetooth devices.
    When a scan has finished, data in ExchangeData is exchanged.
 */
public class BTServiceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BTBroadcastReceiver";

    private BTManager manager;

    // Timestamp to control if it is a new scan
    private long scanStartTimestamp = System.currentTimeMillis() - 100000;
    private long scanStopTimestamp = System.currentTimeMillis() - 100000;

    // Categories of BT device
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private ArrayList<String> devicesFoundStringArray = new ArrayList<>();

    BTServiceBroadcastReceiver(BTManager _manager) {
        manager = _manager;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceMac = device.getAddress();

            Log.d(TAG, "Got Device : " + device.getName() + ", " + deviceMac);

//            if (device.getName() != null) {
                // Check if the device is an OppNet relay
//                if (Utils.isOppNetRelay(device.getName())) {
                    if (!devicesFoundStringArray.contains(deviceMac)) {

                        // Add the device to our list of relays
                        devicesFoundStringArray.add(deviceMac);
                        devices.add(device);
                    }
//                }
//            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            // Start a new scan

            if (System.currentTimeMillis() - scanStartTimestamp > Constants.SCAN_DURATION) {
                devicesFoundStringArray = new ArrayList<>();
                devices = new ArrayList<>();
                scanStartTimestamp = System.currentTimeMillis();

                Log.d(TAG, "Discovery process has been started: " + String.valueOf(System.currentTimeMillis()));
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            // Finish scan

            if (System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION) {
                new ExchangeData(manager, QueueManager.getInstance(context), BatteryMonitor.getInstance(context),
                        devices).execute();
                scanStopTimestamp = System.currentTimeMillis();

                Log.d(TAG, "Discovery process has been stopped: " + String.valueOf(System.currentTimeMillis()));

                // Create a broadcast to notify all BTDeviceListeners of the new device
                Intent deviceBroadcastIntent = new Intent("com.louis.naturalnet.bluetooth.BTDeviceListener");
                deviceBroadcastIntent.putExtra("devices", devices);
                context.sendBroadcast(deviceBroadcastIntent);
            }
        }
    }

    // We can't make this static because it is coupled with the device info arrays
    // Otherwise we'd like to use this pattern:
    // https://github.com/commonsguy/cw-android/blob/master/Rotation/RotationAsync/
    private static class ExchangeData extends AsyncTask<Void, Void, Void> {
        BTManager btManager;
        QueueManager queueManager;
        BatteryMonitor batteryMonitor;

        ArrayList<BluetoothDevice> devices;

        ExchangeData(BTManager _btManager, QueueManager _queueManager, BatteryMonitor _batteryMonitor,
                     ArrayList<BluetoothDevice> _devices) {
            btManager = _btManager;
            queueManager = _queueManager;
            batteryMonitor = _batteryMonitor;
            devices = _devices;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "# of relays " + String.valueOf(devices.size()));

            queueManager.peers += devices.size();

            double maxScore = 0;
            BluetoothDevice deviceToConnect = null;

            // Find relay with best objective function score to send to
            for (BluetoothDevice device : devices) {

                // Device contextual information is stored in the device name. Instead maybe we should send a message
                // to see if the device is an OppNet relay, and then if so, in the response return this data.
                // A) Are you an OppNet relay?
                // B) Yes and here is my contextual information
                // B') No response

                // The use of Utils.getQueueLen (and possibly getBatteryLevel) results in a memory leak.
                // This exchange data may be quite specific to OppNet's purposes, so we can remove this code
                // for now and reimplement communications later.
                /*
                int tmpPeerQueueLen = Utils.getQueueLen(device.getName());
                int tmpEnergyLevel = Utils.getBatteryLevel(device.getName());

                // This looks like our objective function
                double score = (queueManager.getQueueLength() - tmpPeerQueueLen) +
                        Constants.ENERGY_PENALTY_COEFF *
                                (tmpEnergyLevel - batteryMonitor.getBatteryLevel());

                if (score > maxScore) {
                    deviceToConnect = device;
                    maxScore = score;

                    Log.d(TAG, "Greater obj function score " + String.valueOf(tmpPeerQueueLen) + ":"
                            + String.valueOf(Utils.getBatteryLevel(device.getName())));
                }
                */
            }

            // If we found a suitable relay device
            if (deviceToConnect != null && queueManager.getQueueLength() > 0) {
                btManager.connectBTServer(deviceToConnect, Constants.BT_CLIENT_TIMEOUT);

                devices.remove(deviceToConnect);
                queueManager.contacts += 1;
            }

            return null;
        }
    }
}
