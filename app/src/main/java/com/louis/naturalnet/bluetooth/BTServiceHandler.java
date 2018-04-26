package com.louis.naturalnet.bluetooth;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.packet.BasicPacket;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
    Allows message-based communication across BT services
 */

public class BTServiceHandler extends Handler {

    private static final String TAG = "BTServiceHandler";

    private BTController mBTController;
    private Context context;

    // Wrapper
    class Result
    {
        int length;
        String MAC;
        String data;
    }

    BTServiceHandler(BTController controller, Context _context) {
        mBTController = controller;
        context = _context;
    }

    // Same comment as the ExchangeData task
    private class ClientConnectionTask extends AsyncTask<String, Void, Result> {

        // Parses packets in the queue & returns a result of their data
        protected Result doInBackground(String... strings) {
            QueueManager queueManager = QueueManager.getInstance(context);

            String MAC = strings[0];
            String name = strings[1];

            Result result = new Result();
            result.MAC = MAC;
            result.data = "";

            String[] packet;

            // Create a basic data packet
            JSONObject dataPacket = new JSONObject();

            try {
                dataPacket.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONArray dataArray = new JSONArray();
            int numItems = Math.min(Constants.QUEUE_DIFF, queueManager.getQueueLength()); // max 5

            for (int i = 0; i < numItems; i++) {
                packet = queueManager.getFromQueue(Utils.getDeviceID(name));

                if (packet != null) {
                    if (packet[0] != null) {
                        JSONObject data = new JSONObject();

                        // Parse the packet into our own
                        try {
                            data.put(BasicPacket.PACKET_PATH, packet[0]);
                            data.put(BasicPacket.PACKET_DATA, packet[1]);
                            data.put(BasicPacket.PACKET_ID, packet[2]);
                            data.put(BasicPacket.PACKET_DELAY, packet[3]);

                            // Add this packet to our array of packets
                            dataArray.put(data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Add the packet's data to our result
                        result.data = result.data + packet[1];
                        result.length++;
                    } else {
                        mBTController.stopConnection(MAC);
                    }
                } else {
                    mBTController.stopConnection(MAC);
                }
            }

            // Send (to device we received the data from?) the array of data parsed from packets
            try {
                dataPacket.put(BasicPacket.PACKET_DATA, dataArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mBTController.sendToBTDevice(MAC, dataPacket);

            return result;
        }

        // Do we want this?
        @Override
        protected void onPostExecute(Result re) {
            if (re.length > 0)
                QueueManager.getInstance(context).updateName();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        Bundle bundle = msg.getData();
        String MAC = bundle.getString(Constants.BT_DEVICE_MAC);
        String name = bundle.getString(Constants.BT_DEVICE_NAME);

        switch (msg.what) {
            case Constants.BT_CLIENT_ALREADY_CONNECTED:
            case Constants.BT_CLIENT_CONNECTED:
                // don't continue
                new ClientConnectionTask().execute(MAC, name);
                break;
            case Constants.BT_CLIENT_CONNECT_FAILED:
                // Log.d(Constants.TAG_ACT_TEST, "Client Failed");
                // new ExchangeData().execute();
                break;
            case Constants.BT_SUCCESS:
                // Triggered by receiver
                Log.d(Constants.TAG_ACT_TEST, "Success");
                break;
            case Constants.BT_DISCONNECTED:
                Log.d(Constants.TAG_ACT_TEST, "Disconnected");
                break;
            case Constants.BT_SERVER_CONNECTED:
                // Do nothing, wait for data
                Log.d(TAG, "Server Connected");
                QueueManager.getInstance(context).contacts += 1;
                break;
            case Constants.BT_DATA:
                handlePacket(bundle, MAC);
                break;
            default:
                break;
        }
    }

    private void handlePacket(Bundle bundle, String MAC) {
        JSONObject dataContent;
        int packetType;

        try {
            dataContent = new JSONObject(bundle.getString(Constants.BT_DATA_CONTENT));
            packetType = dataContent.getInt(BasicPacket.PACKET_TYPE);

            switch (packetType) {
                case BasicPacket.PACKET_TYPE_DATA:
                    // Handling a data packet
                    // Parse, add to queue, and send ACK

                    QueueManager queueManager = QueueManager.getInstance(context);
                    JSONArray dataArray = dataContent.getJSONArray(BasicPacket.PACKET_DATA);
                    int receivedDataLength = 0;

                    // Parse the data
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject dataItem = dataArray.getJSONObject(i);
                        String id = dataItem.getString(BasicPacket.PACKET_ID);
                        String path = dataItem.getString(BasicPacket.PACKET_PATH);
                        String data = dataItem.getString(BasicPacket.PACKET_DATA);
                        String delay = dataItem.getString(BasicPacket.PACKET_DELAY);

                        Log.d(TAG, "Received Packet id: " + id);
                        Log.d(TAG, "Path: " + path);

                        receivedDataLength += data.length();
                        queueManager.packetsReceived++;
                        queueManager.appendToQueue(id, path, data, delay);
                    }

                    queueManager.updateName();

                    Log.d(TAG, "Received " + receivedDataLength + " bytes data from " + MAC);
                    Log.d(TAG, "New queue size: " + QueueManager.getInstance(context).getQueueLength());

                    // Create and send an ACK
                    JSONObject ackPacket = new JSONObject();
                    try {
                        ackPacket.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA_ACK);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mBTController.sendToBTDevice(MAC, ackPacket);
                    Log.d(TAG, "Sent ACK to " + MAC);
                    break;
                case BasicPacket.PACKET_TYPE_DATA_ACK:
                    // Handle an ACK packet
                    Log.d(TAG, "Receive ACK");
                    mBTController.stopConnection(MAC);
                    QueueManager.getInstance(context).packetsSent++;
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
