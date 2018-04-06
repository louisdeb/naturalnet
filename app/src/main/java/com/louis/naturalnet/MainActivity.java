package com.louis.naturalnet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.widget.Toast;
import com.louis.naturalnet.bluetooth.BTCom;
import com.louis.naturalnet.bluetooth.BTController;
import com.louis.naturalnet.bluetooth.BTScanningAlarm;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.fragments.NetworkFragment;
import com.louis.naturalnet.fragments.WarningFragment;
import com.louis.naturalnet.packet.BasicPacket;
import com.louis.naturalnet.signal.LocationService;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "NaturalNet:";
    private Context mContext = null;

    // Bluetooth
    private static BTController mBTController;
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_BT_ENABLE = 1;
    private final int REQUEST_BT_DISCOVERABLE = 11;
    private int RESULT_BT_DISCOVERABLE_DURATION = 0;

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"Started main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the default content view
        setContentView(R.layout.main_layout);

        if (findViewById(R.id.warning_fragment_container) != null) {
            WarningFragment warningFragment = new WarningFragment();
            getFragmentManager().beginTransaction().add(R.id.warning_fragment_container, warningFragment).commit();
        }

        if (findViewById(R.id.network_fragment_container) != null) {
            NetworkFragment networkFragment = new NetworkFragment();
            getFragmentManager().beginTransaction().add(R.id.network_fragment_container, networkFragment).commit();
        }

        // May choose to have some warning if we can't access the Google API
        //  boolean googleApiAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
        // Log.d(TAG, "Google Api Availability: " + googleApiAvailable);

        startLocationTracking();

        if (mContext == null) {
            mContext = this;
            registerBroadcastReceivers();
        } // else statement exists in OppNet
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBluetoothUtils();
    }

    // Want to stop services
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        BTScanningAlarm.stopScanning(mContext);
        QueueManager.getInstance(mContext).stop(mContext);
        unregisterBroadcastReceivers();
    }

    private void startLocationTracking() {
        startService(new Intent(getBaseContext(), LocationService.class));
    }

    private void registerBroadcastReceivers(){
        // Register the bluetooth BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(BTFoundReceiver, filter);
    }

    private void unregisterBroadcastReceivers(){
        unregisterReceiver(BTFoundReceiver);
    }

    private void initBluetoothUtils(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(mContext, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
        }
        else{
            // start bluetooth utils
            BTServiceHandler handler = new BTServiceHandler();
            mBTController = new BTController(handler);
            mBTController.startBTServer();
            BTScanningAlarm.stopScanning(mContext);
            new BTScanningAlarm(mContext, mBTController);
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
    private class ExchangeData extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... voids) {
            // init the counter
            Log.d(TAG, "# of sensors " + String.valueOf(deviceSensor.size()));
            Log.d(TAG, "# of sinks " + String.valueOf(deviceSink.size()));
            Log.d(TAG, "# of relays " + String.valueOf(deviceRelay.size()));

            QueueManager.getInstance(mContext).peers += deviceRelay.size();

            // send to sink if queue len > 1
            boolean sendToSink = false;
            boolean sendToSensor = false;

            int indexToRemove = -1;
            for(BluetoothDevice device : deviceSink){
                if(((System.currentTimeMillis() - QueueManager.getInstance(mContext).sinkTimestamp) > Constants.SINK_CONTACT_INTERVAL) && QueueManager.getInstance(mContext).getQueueLength() > 0){
                    indexToRemove = deviceSensor.indexOf(device);
                    mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);
                    sendToSink = true;
                    break;
                }
            }
            if(indexToRemove >= 0){
                deviceSensor.remove(indexToRemove);
            }
            // receive from sensor if queue len = 0
            if(!sendToSink){
                indexToRemove = -1;
                for(BluetoothDevice device : deviceSensor){
                    if((System.currentTimeMillis() - QueueManager.getInstance(mContext).sensorTimestamp) > Constants.SENSOR_CONTACT_INTERVAL){
                        indexToRemove = deviceSensor.indexOf(device);
                        mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);
                        sendToSensor = true;
                        break;
                    }
                }
                if(indexToRemove >= 0){
                    deviceSensor.remove(indexToRemove);
                }
            }

            if(!sendToSensor){
                indexToRemove = -1;
                double maxScore = 0;
                BluetoothDevice deviceToConnect = null;
                for(BluetoothDevice device : deviceRelay){
                    int tmpPeerQueueLen = Utils.getQueueLen(device.getName());
                    int tmpEnergyLevel = Utils.getBatteryLevel(device.getName());
                    double score = (QueueManager.getInstance(mContext).getQueueLength() - tmpPeerQueueLen) + Constants.ENERGY_PENALTY_COEFF * (tmpEnergyLevel - BatteryMonitor.getInstance(mContext).getBatteryLevel());
                    if(score > maxScore){
                        indexToRemove = deviceRelay.indexOf(device);
                        deviceToConnect = device;
                        Log.d(TAG, "smaller score found " + String.valueOf(tmpPeerQueueLen) + ":" + String.valueOf(Utils.getBatteryLevel(device.getName())));
                        maxScore = score;
                    }
                }

                if(indexToRemove >= 0 && QueueManager.getInstance(mContext).getQueueLength() > 0){
                    deviceRelay.remove(indexToRemove);
                    mBTController.connectBTServer(deviceToConnect, Constants.BT_CLIENT_TIMEOUT);
                    QueueManager.getInstance(mContext).contacts += 1;
                }
            }
            return null;
        }
    }

    @SuppressLint("HandlerLeak") private class BTServiceHandler extends Handler {

        private final String TAG = "BTServiceHandler";

        // wrapper class
        class Result
        {
            public int length;
            public String MAC;
            public String data;
        }

        // Same comment as the ExchangeData task
        private class ClientConnectionTask extends AsyncTask<String, Void, Result> {

            protected Result doInBackground(String... strings) {
                // init the counter
                String MAC = strings[0];
                String name = strings[1];
                Result re = new Result();
                re.MAC = MAC;
                re.data = "";

                String[] packet;

                JSONObject dataPacket = new JSONObject();
                try {
                    dataPacket.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA);
                } catch (JSONException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                JSONArray dataArray = new JSONArray();
                for(int i=0; i<Math.min(Constants.QUEUE_DIFF, QueueManager.getInstance(mContext).getQueueLength()); i++){
                    packet = QueueManager.getInstance(mContext).getFromQueue(Utils.getDeviceID(name));
                    if(packet != null){
                        if(packet[0] != null){
                            JSONObject data = new JSONObject();
                            try {
                                data.put(BasicPacket.PACKET_PATH, packet[0]);
                                data.put(BasicPacket.PACKET_DATA, packet[1]);
                                data.put(BasicPacket.PACKET_ID, packet[2]);
                                data.put(BasicPacket.PACKET_DELAY, packet[3]);
                                dataArray.put(data);
                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            re.data = re.data + packet[1];
                            re.length++;
                        }
                        else{
                            mBTController.stopConnection(MAC);
                        }
                    }
                    else{
                        mBTController.stopConnection(MAC);
                    }
                }

                try {
                    dataPacket.put(BasicPacket.PACKET_DATA, dataArray);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mBTController.sendToBTDevice(MAC, dataPacket);

                return re;
            }

            @Override
            protected void onPostExecute(Result re) {
                // TODO Auto-generated method stub
                if(re.length > 0){
                    //					arrowView.setBackgroundResource(R.drawable.arrowright);
                    //					byteSent.setText(re.data.length() + " bytes");
                    //					txPeerQueueLen.setText(String.valueOf(peerQueueLen));
                    QueueManager.getInstance(mContext).updateName();
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String MAC = b.getString(BTCom.BT_DEVICE_MAC);
            String name = b.getString(BTCom.BT_DEVICE_NAME);
            switch(msg.what){
                case BTCom.BT_CLIENT_ALREADY_CONNECTED:
                case BTCom.BT_CLIENT_CONNECTED:
                    // don't continue
                    new ClientConnectionTask().execute(MAC, name);
                    break;
                case BTCom.BT_CLIENT_CONNECT_FAILED:
                    Log.d(Constants.TAG_ACT_TEST, "client failed");
                    //				new ExchangeData().execute();
                    break;
                case BTCom.BT_SUCCESS: // triggered by receiver
                    Log.d(Constants.TAG_ACT_TEST, "success");
                    break;
                case BTCom.BT_DISCONNECTED:
                    Log.d(Constants.TAG_ACT_TEST, "disconnected");
                    break;
                case BTCom.BT_SERVER_CONNECTED:
                    Log.d(TAG, "server connected");
                    // do nothing, wait for data
                    QueueManager.getInstance(mContext).contacts += 1;
                    break;
                case BTCom.BT_DATA:
                    JSONObject json;
                    int type;
                    try{
                        json = new JSONObject(b.getString(BTCom.BT_DATA_CONTENT));
                        type = json.getInt(BasicPacket.PACKET_TYPE);
                        switch(type){
                            case BasicPacket.PACKET_TYPE_DATA:
                                JSONArray dataArray = json.getJSONArray(BasicPacket.PACKET_DATA);
                                int receivedDataLen = 0;
                                for(int i=0; i<dataArray.length(); i++){
                                    JSONObject dataItem = dataArray.getJSONObject(i);
                                    String id = dataItem.getString(BasicPacket.PACKET_ID);
                                    String path = dataItem.getString(BasicPacket.PACKET_PATH);
                                    String data = dataItem.getString(BasicPacket.PACKET_DATA);
                                    String delay = dataItem.getString(BasicPacket.PACKET_DELAY);

                                    Log.d(TAG, "received packet " + id);

                                    Log.d(TAG, "path : " + path);
                                    receivedDataLen += data.length();
                                    QueueManager.getInstance(mContext).packetsReceived ++;
                                    QueueManager.getInstance(mContext).appendToQueue(id, path, data, delay);

                                }

                                QueueManager.getInstance(mContext).updateName();

                                //						txMyQueueLen.setText(String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));

                                //						txPeerQueueLen.setText("R");

                                //						arrowView.setBackgroundResource(R.drawable.arrowleft);
                                //						byteSent.setText(String.valueOf(receivedDataLen) + " bytes");

                                Log.d(TAG, "receive " + receivedDataLen + " bytes data from " + MAC);
                                Log.d(TAG, "new queue size " + QueueManager.getInstance(mContext).getQueueLength());
                                JSONObject ack = new JSONObject();
                                try {
                                    ack.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA_ACK);
                                } catch (JSONException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                mBTController.sendToBTDevice(MAC, ack);
                                Log.d(TAG, "send ack to " + MAC);
                                break;
                            case BasicPacket.PACKET_TYPE_DATA_ACK:
                                Log.d(TAG, "receive ack");

                                mBTController.stopConnection(MAC);

                                QueueManager.getInstance(mContext).packetsSent ++;

                                //						txMyQueueLen.setText(String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));
                                break;
                            default:
                                break;
                        }
                    }catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // timestamp to control if it is a new scan
    private long scanStartTimestamp = System.currentTimeMillis() - 100000;
    private long scanStopTimestamp = System.currentTimeMillis() - 100000;

    private ArrayList<BluetoothDevice> deviceSink = new ArrayList<>();
    private ArrayList<BluetoothDevice> deviceRelay = new ArrayList<>();
    private ArrayList<BluetoothDevice> deviceSensor = new ArrayList<>();

    // Create a BroadcastReceiver for actions
    BroadcastReceiver BTFoundReceiver = new BTServiceBroadcastReceiver();

    public class BTServiceBroadcastReceiver extends BroadcastReceiver {

        ArrayList<String> devicesFoundStringArray = new ArrayList<>();

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) { // check if one device found more than once
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceMac = device.getAddress();
                Log.d(TAG, "get a device : " + device.getName() + ", " + deviceMac);

                if(device.getName() != null){
                    if(Utils.isOppNetRelay(device.getName())){ // name parsing
                        if(!devicesFoundStringArray.contains(deviceMac)){
                            devicesFoundStringArray.add(deviceMac);
                            //deviceSensor.add(device);
                            //deviceSink.add(device);
                            deviceRelay.add(device);
                        }
                    }
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                if(System.currentTimeMillis() - scanStartTimestamp > Constants.SCAN_DURATION){
                    //a new scan has been started
                    Log.d(TAG, "Discovery process has been started: " + String.valueOf(System.currentTimeMillis()));

                    devicesFoundStringArray = new ArrayList<>();
                    deviceSink = new ArrayList<>();
                    deviceRelay = new ArrayList<>();
                    deviceSensor = new ArrayList<>();
                    scanStartTimestamp = System.currentTimeMillis();
                }
                invalidateOptionsMenu();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                if(System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION){
                    Log.d(TAG, "Discovery process has been stopped: " + String.valueOf(System.currentTimeMillis()));

                    new ExchangeData().execute();
                    scanStopTimestamp = System.currentTimeMillis();
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (requestCode){
            case REQUEST_BT_ENABLE:
                if (resultCode == RESULT_OK) {
                    // start bluetooth utils
                    initBluetoothUtils();
                    Intent discoverableIntent = new
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, RESULT_BT_DISCOVERABLE_DURATION);
                    startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABLE);
                }
                else{
                    Log.d(TAG, "Bluetooth is not enabled by the user.");
                }
                break;
            case REQUEST_BT_DISCOVERABLE:
                if (resultCode == RESULT_CANCELED){
                    Log.d(TAG, "Bluetooth is not discoverable.");
                }
                else{
                    Log.d(TAG, "Bluetooth is discoverable by 300 seconds.");
                }
                break;
            default:
                break;
        }
    }
}
