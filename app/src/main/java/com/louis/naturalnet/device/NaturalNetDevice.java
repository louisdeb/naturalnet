package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.signal.SignalQuality;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

public class NaturalNetDevice {

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

    public double getScore(JSONObject destination) {
        double score = (DeviceInformation.getQueueLength() - this.queueLength) +
                Constants.ENERGY_PENALTY_COEFF * (batteryLevel - DeviceInformation.getBatteryLevel());

        if (destination != null) {
            try {
                score += getLocationScore(destination);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return score;
    }

    private double getLocationScore(JSONObject destination) throws JSONException {
        double score = 0;
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        long destLat1 = destination.getLong(Warning.WARNING_LAT_START);
        long destLat2 = destination.getLong(Warning.WARNING_LAT_END);
        long destLon1 = destination.getLong(Warning.WARNING_LON_START);
        long destLon2 = destination.getLong(Warning.WARNING_LON_END);

        // Calculate the smallest distance between the device and the location area.
        double diffLon = Math.min(Math.toRadians(Math.abs(destLon1 - lon)),
                                  Math.toRadians(Math.abs(destLon2 - lon)));

        double minDistance = Math.min(haversine(lat, destLat1, diffLon),
                                      haversine(lat, destLat2, diffLon));

        score = Constants.EARTH_DIAMETER - minDistance;

        // If we have movement information, calculate how soon this device will reach the target location.
        if (location.hasBearing() && location.hasSpeed()) {
            
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

    // Could add scoring functions in here.

}
