package com.louis.naturalnet.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.packet.BasicPacket;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
    Handles received message and responds with ACKs and data from the queue.
 */
class BTMessageHandler {

    private static final String TAG = "BTMessageHandler";

    // The BTController allows us to send data to a device through BTCom.
    private BTController btController;
    private Context context;

    // Wrapper
    class Result
    {
        int length;
        String MAC;
        String data;
    }

    BTMessageHandler(Context context) {
        this.context = context;

        IntentFilter messageFilter = new IntentFilter("com.louis.naturalnet.bluetooth.MessageReceiver");
        context.registerReceiver(new MessageReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleMessage(intent);
            }
        }, messageFilter);
    }

    void setBTController(BTController btController) {
        this.btController = btController;
    }

    private void handleMessage(Intent intent) {
        // TODO: Implement intent parsing and message handling.

        // Old message handler had this:
        // Bundle bundle = msg.getData();
        // String MAC = bundle.getString(Constants.BT_DEVICE_MAC);

        // For msg.what = BT_CLIENT_CONNECTED
        // it called new ClientConnectionTask().execute(MAC, name);

        // For msg.what = BT_DATA_RECEIVED
        // it called handlePacket
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

                    Log.d(TAG, "Received " + receivedDataLength + " bytes data from " + MAC);
                    Log.d(TAG, "New queue size: " + QueueManager.getInstance(context).getQueueLength());

                    // Create and send an ACK
                    JSONObject ackPacket = new JSONObject();
                    try {
                        ackPacket.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA_ACK);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    btController.sendToBTDevice(MAC, ackPacket);
                    Log.d(TAG, "Sent ACK to " + MAC);
                    break;
                case BasicPacket.PACKET_TYPE_DATA_ACK:
                    // Handle an ACK packet
                    Log.d(TAG, "Receive ACK");
                    btController.stopConnection(MAC);
                    QueueManager.getInstance(context).packetsSent++;
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
                        btController.stopConnection(MAC);
                    }
                } else {
                    btController.stopConnection(MAC);
                }
            }

            // Send (to device we received the data from?) the array of data parsed from packets
            try {
                dataPacket.put(BasicPacket.PACKET_DATA, dataArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            btController.sendToBTDevice(MAC, dataPacket);

            return result;
        }

    }
}
