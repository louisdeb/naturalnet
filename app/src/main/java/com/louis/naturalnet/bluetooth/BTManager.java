package com.louis.naturalnet.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.utils.Utils;

import java.util.ArrayList;

public class BTManager {

    private static final String TAG = "BTManager";
    private Context context;

    private static BTController mBTController;
    private BroadcastReceiver mBroadcastReceiver = new BTServiceBroadcastReceiver();

    public static final int REQUEST_BT_ENABLE = 1;
    public static final int REQUEST_BT_DISCOVERABLE = 11;
    public static int RESULT_BT_DISCOVERABLE_DURATION = 0;

    // Timestamp to control if it is a new scan
    private long scanStartTimestamp = System.currentTimeMillis() - 100000;
    private long scanStopTimestamp = System.currentTimeMillis() - 100000;

    // Categories of BT device
    private ArrayList<BluetoothDevice> deviceSink = new ArrayList<>();
    private ArrayList<BluetoothDevice> deviceRelay = new ArrayList<>();
    private ArrayList<BluetoothDevice> deviceSensor = new ArrayList<>();

    public BTManager(Context _context) {
        context = _context;
    }

    public void registerBroadcastReceivers(){
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

    /*
        This class handles discovering and saving bluetooth devices.
        When a scan has finished, data in ExchangeData is exchanged.
     */
    public class BTServiceBroadcastReceiver extends BroadcastReceiver {
        ArrayList<String> devicesFoundStringArray = new ArrayList<>();

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceMac = device.getAddress();

                Log.d(TAG, "Got Device : " + device.getName() + ", " + deviceMac);

                if (device.getName() != null) {
                    // Check if the device is an OppNet relay
                    if (Utils.isOppNetRelay(device.getName())) {
                        if (!devicesFoundStringArray.contains(deviceMac)) {

                            // Add the device to our list of relays
                            devicesFoundStringArray.add(deviceMac);
                            deviceRelay.add(device);

                            // This code was commented out in OppNet
                            // We have no cause for sensors in our application
                            // However will have to investigate how OppNet communicates between
                            // sensors, sinks & relays, and work out what we need to do for P2P bidirectional comms.

                            // deviceSensor.add(device);
                            // deviceSink.add(device);
                        }
                    }

                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Start a new scan

                if (System.currentTimeMillis() - scanStartTimestamp > Constants.SCAN_DURATION) {
                    devicesFoundStringArray = new ArrayList<>();
                    deviceSink = new ArrayList<>();
                    deviceRelay = new ArrayList<>();
                    deviceSensor = new ArrayList<>();
                    scanStartTimestamp = System.currentTimeMillis();

                    Log.d(TAG, "Discovery process has been started: " + String.valueOf(System.currentTimeMillis()));
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Finish scan

                if(System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION){
                    // Exchange data?
                    new ExchangeData().execute();
                    scanStopTimestamp = System.currentTimeMillis();

                    Log.d(TAG, "Discovery process has been stopped: " + String.valueOf(System.currentTimeMillis()));
                }
            }
        }
    }

    /**
     * exchange sensor data
     * @author fshi
     *
     */
    // We can't make this static because it is coupled with the device info arrays
    // Otherwise we'd like to use this pattern:
    // https://github.com/commonsguy/cw-android/blob/master/Rotation/RotationAsync/
    /*
        This class looks through Sinks > Sensors > Relays trying to find a suitable device to send data to
     */
    private class ExchangeData extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "# of sensors " + String.valueOf(deviceSensor.size()));
            Log.d(TAG, "# of sinks " + String.valueOf(deviceSink.size()));
            Log.d(TAG, "# of relays " + String.valueOf(deviceRelay.size()));

            boolean sendToSink = false;
            boolean sendToSensor = false;
            QueueManager queueManager = QueueManager.getInstance(context);

            queueManager.peers += deviceRelay.size();

            // Try send to sink
            for (BluetoothDevice device : deviceSink) {
                long timeSinceSinkContact = System.currentTimeMillis() - queueManager.sinkTimestamp;
                if (timeSinceSinkContact > Constants.SINK_CONTACT_INTERVAL && queueManager.getQueueLength() > 0) {
                    mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);

                    sendToSink = true;
                    deviceSensor.remove(device);
                    break;
                }
            }

            // If we couldn't send to sink, try send to sensor
            if (!sendToSink) {
                for (BluetoothDevice device : deviceSensor) {
                    if ((System.currentTimeMillis() - queueManager.sensorTimestamp) > Constants.SENSOR_CONTACT_INTERVAL) {
                        mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);

                        sendToSensor = true;
                        deviceSensor.remove(device);
                        break;
                    }
                }
            }

            // If we couldn't send to sensor, try send to relay
            if (!sendToSensor) {
                double maxScore = 0;
                BluetoothDevice deviceToConnect = null;

                // Find relay with best objective function score to send to
                for (BluetoothDevice device : deviceRelay) {
                    int tmpPeerQueueLen = Utils.getQueueLen(device.getName());
                    int tmpEnergyLevel = Utils.getBatteryLevel(device.getName());

                    // This looks like our objective function
                    double score = (QueueManager.getInstance(context).getQueueLength() - tmpPeerQueueLen) +
                            Constants.ENERGY_PENALTY_COEFF * (tmpEnergyLevel - BatteryMonitor.getInstance(context).getBatteryLevel());

                    if(score > maxScore){
                        deviceToConnect = device;
                        maxScore = score;

                        Log.d(TAG, "Greater obj function score " + String.valueOf(tmpPeerQueueLen) + ":" + String.valueOf(Utils.getBatteryLevel(device.getName())));
                    }
                }

                // If we found a suitable relay device
                if(deviceToConnect != null && QueueManager.getInstance(context).getQueueLength() > 0) {
                    mBTController.connectBTServer(deviceToConnect, Constants.BT_CLIENT_TIMEOUT);

                    deviceRelay.remove(deviceToConnect);
                    QueueManager.getInstance(context).contacts += 1;
                }
                // Could add an else indicating we found no suitable devices to connect to
            }

            return null;
        }
    }
}
