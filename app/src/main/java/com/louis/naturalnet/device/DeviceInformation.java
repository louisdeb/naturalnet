package com.louis.naturalnet.device;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
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
    private static Location location;

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
                location = SignalUtils.getLocation(intent);
                Log.d(TAG, intent.toString());
            }
        }, locationFilter);
    }

    public static JSONObject getHandshake() {
        JSONObject metadata = new JSONObject();

        try {
            metadata.put("handshake", true);
            metadata.put("signalQuality", signalQuality.getVal());
            metadata.put("battery", getBatteryLevel());
            metadata.put("queueLength", getQueueLength());
            metadata.put("location", location);
        } catch (JSONException e) {
            Log.d(TAG, "Failed to build handshake JSON object");
            e.printStackTrace();
        }

        return metadata;
    }
    
    static int getBatteryLevel() {
        return BatteryMonitor.getInstance(activity).getBatteryLevel();
    }

    static int getQueueLength() {
        return QueueManager.getInstance(activity).getQueueLength();
    }

    /* Handshake parsing functions */
    static SignalQuality parseSignalQuality(JSONObject metadata) {
        try {
            int val = (int) metadata.get("signalQuality");
            return SignalQuality.values()[val];
        } catch (JSONException e) {
            return SignalQuality.NONE_OR_NOT_KNOWN;
        }
    }

    static int parseBatteryLevel(JSONObject metadata) {
        try {
            return (int) metadata.get("battery");
        } catch (JSONException e) {
            return -1;
        }
    }

    static int parseQueueLength(JSONObject metadata) {
        try {
            return (int) metadata.get("queueLength");
        } catch (JSONException e) {
            return -1;
        }
    }

    static Location parseLocation(JSONObject metadata) {
        try {
            JSONObject locationJSON = new JSONObject((String) metadata.get("location"));
            // We have to manually parse this JSON into a Location object.
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
}
