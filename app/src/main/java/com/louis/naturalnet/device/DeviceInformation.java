package com.louis.naturalnet.device;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SignalStrength;
import android.util.Log;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.energy.BatteryMonitor;
import com.louis.naturalnet.signal.*;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceInformation {

    private static final String TAG = "DeviceInformation";

    private static Activity activity;

    private static SignalQuality signalQuality;
    private static SignalQuality gpsQuality;

    // Keep track of
    // - cellular signal
    // - gps location
    // - number of peers
    public static void listenToSignals(Activity _activity) {
        activity = _activity;

        CellularSignalReceiver.addListener(activity, new CellularSignalListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                signalQuality = SignalUtils.getSignalQuality(signalStrength);
            }
        });

        IntentFilter locationFilter = new IntentFilter("com.louis.naturalnet.signal.LocationReceiver");
        activity.registerReceiver(new LocationReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                gpsQuality = SignalUtils.getGpsQuality(intent);
            }
        }, locationFilter);
    }

    public static JSONObject getHandshake() {
        JSONObject metadata = new JSONObject();

        try {
            metadata.put("handshake", true);
            metadata.put("signalQuality", signalQuality);
            metadata.put("battery", BatteryMonitor.getInstance(activity).getBatteryLevel());
            metadata.put("queueLength", QueueManager.getInstance(activity).getQueueLength());

            // Adds the gps quality but not actually location information.
            metadata.put("gpsQuality", gpsQuality);
        } catch (JSONException e) {
            Log.d(TAG, "Failed to build handshake JSON object");
            e.printStackTrace();
        }

        return metadata;
    }

}
