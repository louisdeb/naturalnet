package com.louis.naturalnet.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import com.louis.naturalnet.data.Packet;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.utils.Constants;
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
        Message message = intent.getParcelableExtra("message");
        Bundle bundle = message.getData();
        String MAC = bundle.getString(Constants.BT_DEVICE_MAC);

        try {
            JSONObject packet = new JSONObject(bundle.getString(Constants.BT_DATA_CONTENT));
            int packetType = packet.getInt(Packet.TYPE);

            switch (packetType) {
                case Packet.TYPE_WARNING:
                    handleWarningPacket(packet, MAC);
                    break;
                case Packet.TYPE_WARNING_ACK:
                    handleWarningACK(packet, MAC);
                    break;
                /* These are legacy OppNet packet types.
                case Packet.PACKET_TYPE_DATA:
                    handleDataPacket(packet, MAC);
                    break;
                case Packet.PACKET_TYPE_DATA_ACK:
                    handleACKPacket(MAC);
                    break;
                */
                default:
                    break;
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to parse received message");
            e.printStackTrace();
        }
    }

    private void handleWarningPacket(JSONObject packet, String MAC) throws JSONException {
        JSONObject warningJSON = new JSONObject(packet.getString(Packet.DATA));
        Warning warning = new Warning(warningJSON);

        Log.d(TAG, "Received warning: " + warning.toString());

        // Announce warning received (check if we are in the zone, add to queue to transmit).
        Intent warningNotification = new Intent("com.louis.naturalnet.data.WarningReceiver");
        warningNotification.putExtra("warning", warning.toString());
        context.sendBroadcast(warningNotification);

        // Add the warning to our queue.
        QueueManager.getInstance().addPacketToQueue(packet);

        // Send ACK.
        JSONObject ack = new JSONObject();
        ack.put(Packet.TYPE, Packet.TYPE_WARNING_ACK);
        ack.put(Packet.DATA, warning.warningId);
        btController.sendToBTDevice(MAC, ack);
    }

    private void handleWarningACK(JSONObject packet, String MAC) throws JSONException {
        int warningId = packet.getInt(Packet.DATA);
        QueueManager.getInstance().removeFromQueue(warningId);

        // We could also increase the score of the destination device as we have successfully transferred a warning
        // to it.
    }

    /*
    // Possibly remove the dataArray. Instead just expect one warning as the DATA entry.
    private void handleDataPacket(JSONObject dataContent, String MAC) throws JSONException {
        // Handling a data packet
        // Parse, add to queue, and send ACK

        QueueManager queueManager = QueueManager.getInstance();
        JSONArray dataArray = dataContent.getJSONArray(Packet.DATA);
        int receivedDataLength = 0;

        // Parse the data
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataItem = dataArray.getJSONObject(i);
            String id = dataItem.getString(Packet.ID);
            String path = dataItem.getString(Packet.PATH);
            String data = dataItem.getString(Packet.DATA);
            String delay = dataItem.getString(Packet.DELAY);

            Log.d(TAG, "Received Packet id: " + id);
            Log.d(TAG, "Path: " + path);

            receivedDataLength += data.length();
            queueManager.packetsReceived++;
            queueManager.appendToQueue(id, path, data, delay);
        }

        Log.d(TAG, "Received " + receivedDataLength + " bytes data from " + MAC);
        Log.d(TAG, "New queue size: " + QueueManager.getInstance().getQueueLength());

        // Create and send an ACK
        JSONObject ackPacket = new JSONObject();
        try {
            ackPacket.put(Packet.TYPE, Packet.PACKET_TYPE_DATA_ACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        btController.sendToBTDevice(MAC, ackPacket);
        Log.d(TAG, "Sent ACK to " + MAC);
    }

    private void handleACKPacket(String MAC) {
        // Handle an ACK packet
        Log.d(TAG, "Receive ACK");
        btController.stopConnection(MAC);
        QueueManager.getInstance().packetsSent++;
    }
    */

    // This task creates a packet from the queue item and sends it to a peer.
    /*
    private class ClientConnectionTask extends AsyncTask<String, Void, Result> {

        // Parses packets in the queue & returns a result of their data
        protected Result doInBackground(String... strings) {
            QueueManager queueManager = QueueManager.getInstance();

            String MAC = strings[0];
            String name = strings[1];

            Result result = new Result();
            result.MAC = MAC;
            result.data = "";

            String[] packet;

            // Create a basic data packet
            JSONObject dataPacket = new JSONObject();

            try {
                dataPacket.put(Packet.TYPE, Packet.PACKET_TYPE_DATA);
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
                            data.put(Packet.PATH, packet[0]);
                            data.put(Packet.DATA, packet[1]);
                            data.put(Packet.ID, packet[2]);
                            data.put(Packet.DELAY, packet[3]);

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
                dataPacket.put(Packet.DATA, dataArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            btController.sendToBTDevice(MAC, dataPacket);

            return result;
        }
    }
    */

}
