package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.util.Log;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.signal.SignalQuality;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

public class NaturalNetDevice {

    private static final String TAG = "NetDevice";

    public BluetoothDevice device;

    private SignalQuality signalQuality;
    private Location location;
    private int batteryLevel;
    private int queueLength;

    public NaturalNetDevice(BluetoothDevice device, JSONObject metadata) {
        this.device = device;
        parseMetadata(metadata);
    }

    private void parseMetadata(JSONObject metadata) {
        signalQuality = DeviceInformation.parseSignalQuality(metadata);
        batteryLevel = DeviceInformation.parseBatteryLevel(metadata);
        queueLength = DeviceInformation.parseQueueLength(metadata);
        location = DeviceInformation.parseLocation(metadata);
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return device.getName();
    }

    public double getScore(JSONObject destination) {
        double score = (DeviceInformation.getQueueLength() - this.queueLength) +
                Constants.ENERGY_PENALTY_COEFF * (batteryLevel - DeviceInformation.getBatteryLevel());

        Log.d(TAG, "Getting score for device " + device.getName());
        Log.d(TAG, "Traditional score: " + score);

        try {
            score += getLocationScore(destination);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return score;
    }

    private double getLocationScore(JSONObject destination) throws JSONException {
        if (destination == null)
            return Constants.EARTH_DIAMETER;

        double score;
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        /* Test locations - use these on three devices to test decision making. */

        /*
        if (device.getName().equals("net0")) {
            // Closest to the test destination
            lat = 51.538331;
            lon = -0.154256;
        } else if (device.getName().equals("net1")) {
            // In between location
            lat = 51.538106;
            lon = -0.151417;
        } else if (device.getName().equals("net2")) {
            // Farthest from test destination
            lat = 51.537782;
            lon = -0.148846;
        }
        */

        double destLat1 = destination.getDouble(Warning.WARNING_LAT_START);
        double destLon1 = destination.getDouble(Warning.WARNING_LON_START);
        double destLat2 = destination.getDouble(Warning.WARNING_LAT_END);
        double destLon2 = destination.getDouble(Warning.WARNING_LON_END);

        Log.d(TAG, "Calculating distance between location " + lat + ", " + lon + " and zone (" + destLat1 + ", " +
                          destLon1 + ") (" + destLat2 + ", " + destLon2 + ")");

        // Calculate the smallest distance between the device and the location area.
        double diffLon = Math.min(Math.toRadians(Math.abs(destLon1 - lon)),
                                  Math.toRadians(Math.abs(destLon2 - lon)));

        Log.d(TAG, "diffLon: " + diffLon);

        double minDistance = Math.min(haversine(lat, destLat1, diffLon),
                                      haversine(lat, destLat2, diffLon));

        Log.d(TAG, "minDistance: " + minDistance);

        score = Constants.EARTH_DIAMETER - minDistance;

        Log.d(TAG, "score: " + score);

        // If we have movement information, calculate how soon this device will reach the target location.
        if (location.hasBearing() && location.hasSpeed()) {
            // Calculate time till arrival and add it on to the score
            // If the device is travelling away from the destination we'd want to take away some amount from the score.
        }

        return score;
    }

    private double haversine(double lat, double destLat, double diffLon) {
        double slat = Math.toRadians(lat);
        double flat = Math.toRadians(destLat);

        double diffLat = Math.toRadians(Math.abs(destLat - lat));
        double a = Math.pow(Math.sin(diffLat / 2), 2) +
                   Math.cos(slat) * Math.cos(flat) * Math.pow(Math.sin(diffLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Constants.EARTH_DIAMETER * c;
    }

}
