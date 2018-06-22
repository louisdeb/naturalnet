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
import com.louis.naturalnet.device.DeviceInformation;
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
        try {
            boolean deviceAtDestination = DeviceInformation.isAtDestination(warning.getZone());
            warningNotification.putExtra("inZone", deviceAtDestination);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

}
