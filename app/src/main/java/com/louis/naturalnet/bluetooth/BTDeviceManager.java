package com.louis.naturalnet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import com.louis.naturalnet.data.Packet;
import com.louis.naturalnet.data.QueueItem;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.device.NaturalNetDevice;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/*
    This class handles discovering and saving bluetooth discoveredDevices.
    When a scan has finished, we perform handshakes with each discovered device to see if it is a NaturalNet device.
    If we find a NaturalNet device we get the metadata in the handshake.
 */

public class BTDeviceManager extends BroadcastReceiver {

    private static final String TAG = "BTBroadcastReceiver";

    private BTManager manager;
    private Context context;

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
        this.context = context;

        IntentFilter handshakeFilter = new IntentFilter("com.louis.naturalnet.bluetooth.HandshakeReceiver");
        context.registerReceiver(new HandshakeReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleHandshakeResponse(intent);
            }
        }, handshakeFilter);

        // Start the QueueMonitor with periodic run time.
        new Timer().scheduleAtFixedRate(new QueueMonitor(), 0, Constants.QUEUE_MONITOR_INTERVAL);
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
                Log.d(TAG, "Failed to parse handshake intent");
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

        // Create a broadcast to notify all BTDeviceListeners of the new device.
        // We can't parcel a NaturalNetDevice, so instead transmit the MACs.
        Intent deviceBroadcastIntent = new Intent("com.louis.naturalnet.bluetooth.BTDeviceListener");
        deviceBroadcastIntent.putStringArrayListExtra("devices", naturalNetMACs);
        context.sendBroadcast(deviceBroadcastIntent);
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

    private void sendToBestDevice(JSONObject packet, String path, JSONObject destination) {
        Log.d(TAG, "Finding best device");
        double maxScore = 0;
        NaturalNetDevice bestDevice = null;
        for (NaturalNetDevice device : naturalNetDevices) {
            // If this device has already received the packet, do not try to resend it.
            if (path.contains(device.getAddress()))
                continue;

            // If the device is in the destination zone, send the packet to them regardless of their score.
            if (device.isAtDestination(destination)) {
                Log.d(TAG, "Sending to device at destination: " + device.getName());
                manager.sendToBTDevice(device.device, packet);
            } else {
                double score = device.getScore(destination);
                Log.d(TAG, "Device " + device.getName() + " has score: " + score);

                if (bestDevice == null || score > maxScore) {
                    maxScore = score;
                    bestDevice = device;
                }
            }
        }

        if (bestDevice != null) {
            Log.d(TAG, "Sending to device: " + bestDevice.getName());
            manager.sendToBTDevice(bestDevice.device, packet);
        } else {
            Log.d(TAG, "Got no best device");
        }
    }

    private class QueueMonitor extends TimerTask {

        @Override
        public void run() {
            Log.d(TAG, "QueueMonitor instance running");
            QueueManager queueManager = QueueManager.getInstance();

            if (queueManager.getQueueLength() > 0) {
                QueueItem item = queueManager.getFirstFromQueue();
                JSONObject packet = new JSONObject();

                JSONObject destination = null;

                // Create a packet out of the item.
                if (item.dataType == Packet.TYPE_WARNING) {
                    try {
                        packet.put(Packet.TYPE, Packet.TYPE_WARNING);
                        packet.put(Packet.ID, item.packetId);
                        packet.put(Packet.PATH, item.path);
                        packet.put(Packet.DELAY, item.delay);
                        packet.put(Packet.DATA, item.data);
                        packet.put(Packet.TIMESTAMP, item.timestamp);

                        destination = new JSONObject();
                        JSONObject warning = new JSONObject(item.data);
                        destination.put(Warning.WARNING_LAT_START, warning.getDouble(Warning.WARNING_LAT_START));
                        destination.put(Warning.WARNING_LON_START, warning.getDouble(Warning.WARNING_LON_START));
                        destination.put(Warning.WARNING_LAT_END, warning.getDouble(Warning.WARNING_LAT_END));
                        destination.put(Warning.WARNING_LON_END, warning.getDouble(Warning.WARNING_LON_END));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // TODO: We may want to flood to every known device if we are in the destination zone.
                sendToBestDevice(packet, item.path, destination);
            }
        }

    }

}
