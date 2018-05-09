package com.louis.naturalnet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.device.NaturalNetDevice;
import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
    This class handles discovering and saving bluetooth discoveredDevices.
    When a scan has finished, we perform handshakes with each discovered device to see if it is a NaturalNet device.
    If we find a NaturalNet device we get the metadata in the handshake.
 */

public class BTDeviceManager extends BroadcastReceiver {

    private static final String TAG = "BTBroadcastReceiver";

    private BTManager manager;

    // Timestamp to control if it is a new scan
    private long scanStartTimestamp = System.currentTimeMillis() - 100000;
    private long scanStopTimestamp = System.currentTimeMillis() - 100000;

    // Categories of BT device
    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayList<NaturalNetDevice> naturalNetDevices = new ArrayList<>();

    private ArrayList<String> discoveredMACs = new ArrayList<>();
    private ArrayList<String> naturalNetMACs = new ArrayList<>();

    private ArrayList<String> failedMACs = new ArrayList<>();

    BTDeviceManager(BTManager manager, Context context) {
        this.manager = manager;

        IntentFilter handshakeFilter = new IntentFilter("com.louis.naturalnet.bluetooth.HandshakeReceiver");
        context.registerReceiver(new HandshakeReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleHandshakeResponse(intent);
            }
        }, handshakeFilter);
    }

    // Receives discovered devices from BluetoothAdapter scan.
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceMac = device.getAddress();

            if (!discoveredMACs.contains(deviceMac) && !naturalNetMACs.contains(deviceMac) && !failedMACs.contains(deviceMac)) {
                Log.d(TAG, "Got Device : " + device.getName() + ", " + deviceMac);

                // Add the device to our list of discovered devices.
                discoveredMACs.add(deviceMac);
                discoveredDevices.add(device);
            }

        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            // Start a new scan

            if (System.currentTimeMillis() - scanStartTimestamp > Constants.SCAN_DURATION) {
                discoveredMACs = new ArrayList<>();
                discoveredDevices = new ArrayList<>();
                scanStartTimestamp = System.currentTimeMillis();

                Log.d(TAG, "Discovery process has been started: " + String.valueOf(System.currentTimeMillis()));
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            // Finish BT scan. Now try perform a handshake with each of the devices.

            // Do we want this if statement? It checks that we have waited the scan duration, but are also receiving
            // the action ACTION_DISCOVERY_FINISHED? Probably just worth checking if we ever receive this action
            // without having waited the whole duration.
            if (System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION) {

                // If we have data to send and we have some NaturalNet devices, we may want to prioritise this than
                // checking for new NaturalNet devices.

                // Dispatch a task to create a communication with each of the devices
                new ExchangeHandshake(manager, discoveredDevices).execute();
            }

            /*
            if (System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION) {
                new ExchangeData(manager, QueueManager.getInstance(context), BatteryMonitor.getInstance(context),
                        naturalNetDevices).execute();

                scanStopTimestamp = System.currentTimeMillis();

                Log.d(TAG, "Discovery process has been stopped: " + String.valueOf(System.currentTimeMillis()));

                // Create a broadcast to notify all BTDeviceListeners of the new device
                // Actually here we're just notifying of all surrounding BT discoveredDevices, instead of NaturalNet devices.
                Intent deviceBroadcastIntent = new Intent("com.louis.naturalnet.bluetooth.BTDeviceListener");
                deviceBroadcastIntent.putExtra("discoveredDevices", discoveredDevices);
                context.sendBroadcast(deviceBroadcastIntent);
            }
            */
        }
    }

    private void handleHandshakeResponse(Intent intent) {
        boolean connected = intent.getBooleanExtra("connected", false);
        if (connected) {
            try {
                BluetoothDevice device = intent.getParcelableExtra("device");
                String metadataString = intent.getStringExtra("metadata");
                JSONObject metadata = new JSONObject(metadataString);
                NaturalNetDevice netDevice = new NaturalNetDevice(device, metadata);
                handshakeReceivedMetadata(netDevice);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            BluetoothDevice device = intent.getParcelableExtra("device");
            handshakeFailed(device);
        }
    }

    private void handshakeFailed(BluetoothDevice device) {
        // This is for testing, in course our net devices have a failed connection we don't have to rebuild to
        // retry a connection.
        if (device.getName() != null && device.getName().toLowerCase().contains("net"))
            return;

        failedMACs.add(device.getAddress());
        discoveredDevices.remove(device);
        discoveredMACs.remove(device.getAddress());
    }

    private void handshakeReceivedMetadata(NaturalNetDevice device) {
        naturalNetDevices.add(device);
        naturalNetMACs.add(device.getAddress());
    }

    private static class ExchangeHandshake extends AsyncTask<Void, Void, Void> {
        BTManager btManager;

        ArrayList<BluetoothDevice> discoveredDevices;

        ExchangeHandshake(BTManager btManager, ArrayList<BluetoothDevice> discoveredDevices) {
            this.btManager = btManager;
            this.discoveredDevices = discoveredDevices;
        }

        // For every discovered device, perform some handshake with a timeout. We want to check that the device
        // responds in the way a NaturalNet device would. If we reach the timeout, we can assume the device
        // was not a NaturalNet device and then add it to a list of devices that we won't communicate with again.
        @Override
        protected Void doInBackground(Void... voids) {
            for (BluetoothDevice device : discoveredDevices) {
                btManager.connectToBTServer(device, Constants.BT_HANDSHAKE_TIMEOUT);
            }
            return null;
        }
    }

    private static class ExchangeData extends AsyncTask<Void, Void, Void> {
        BTManager btManager;
        QueueManager queueManager;
        BatteryMonitor batteryMonitor;

        ArrayList<NaturalNetDevice> devices;

        ExchangeData(BTManager btManager, QueueManager queueManager, BatteryMonitor batteryMonitor,
                     ArrayList<NaturalNetDevice> devices) {
            this.btManager = btManager;
            this.queueManager = queueManager;
            this.batteryMonitor = batteryMonitor;
            this.devices = devices;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "# of relays " + String.valueOf(devices.size()));

            double maxScore = 0;
            NaturalNetDevice deviceToSendTo = null;

            // Find relay with best objective function score to send to
            for (NaturalNetDevice device : devices) {

                // The traditional OppNet objective function. We also want to include location decision making.
                double score = device.getScore();

                if (score > maxScore) {
                    deviceToSendTo = device;
                    maxScore = score;
                }
            }

            // If we found a suitable relay device and we have data to send.
            if (deviceToSendTo != null && queueManager.getQueueLength() > 0) {
                // btManager.connectToBTServer(deviceToSendTo, Constants.BT_CLIENT_TIMEOUT);
                // We don't need to actually connect to the BT server, we should already have a connection. Instead
                // we want to cause some send to the device.

                // I think this remove means that we won't send the device information twice. Does doInBackground
                // run indefinitely? Does it send the information to all devices, but in order of descending score?
                devices.remove(deviceToSendTo);
            }

            return null;
        }
    }
}
