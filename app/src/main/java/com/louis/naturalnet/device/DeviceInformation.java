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
                Location newLocation = SignalUtils.getLocation(intent);
                location = (newLocation == null) ? location : newLocation;
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

    public static Location getLocation() {
        return location;
    }

    private static int getQueueLength() {
        return QueueManager.getInstance().getQueueLength();
    }

    // Determines whether our device is in the destination zone.
    public static boolean isAtDestination(JSONObject destination) {
        return NaturalNetDevice.locationInZone(location, destination);
    }

    /*--- Handshake parsing functions ---*/

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
            String locString = metadata.get("location").toString();
            Location location = new Location("fused");

            // We might also want to parse the 'et' value (elapsed time).

            if (locString.contains("fused")) {
                int latStart = locString.indexOf("fused") + "fused".length() + 1;
                int latEnd = locString.indexOf(',');
                double lat = Double.valueOf(locString.substring(latStart, latEnd));

                int lonStart = latEnd + 1;
                int lonEnd = locString.indexOf(' ', lonStart);
                double lon = Double.valueOf(locString.substring(lonStart, lonEnd));

                location.setLatitude(lat);
                location.setLongitude(lon);
            }

            if (locString.contains("acc")) {
                int accStart = locString.indexOf("acc") + "acc".length() + 1;
                int accEnd = locString.indexOf(' ', accStart);
                float acc = Float.valueOf(locString.substring(accStart, accEnd));

                location.setAccuracy(acc);
            }

            if (locString.contains("alt")) {
                int altStart = locString.indexOf("alt") + "alt".length() + 1;
                int altEnd = locString.indexOf(' ', altStart);
                double alt = Double.valueOf(locString.substring(altStart, altEnd));

                location.setAltitude(alt);
            }

            if (locString.contains("vel")) {
                int velStart = locString.indexOf("vel") + "vel".length() + 1;
                int velEnd = locString.indexOf(' ', velStart);
                float vel = Float.valueOf(locString.substring(velStart, velEnd));

                location.setSpeed(vel);
            }

            if (locString.contains("bear")) {
                int bearStart = locString.indexOf("bear") + "bear".length() + 1;
                int bearEnd = locString.indexOf(' ', bearStart);
                float bear = Float.valueOf(locString.substring(bearStart, bearEnd));

                location.setBearing(bear);
            }

            return location;
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse location");
            e.printStackTrace();
            return null;
        }
    }
}
