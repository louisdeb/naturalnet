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

    public boolean isAtDestination(JSONObject destination) {
        try {
            if (location == null)
                return false;

            double lat = location.getLatitude();
            double lon = location.getLongitude();
            double destLat1 = destination.getDouble(Warning.WARNING_LAT_START);
            double destLat2 = destination.getDouble(Warning.WARNING_LAT_END);
            double destLon1 = destination.getDouble(Warning.WARNING_LON_START);
            double destLon2 = destination.getDouble(Warning.WARNING_LON_END);

            double minLat = Math.min(destLat1, destLat2);
            double maxLat = Math.max(destLat1, destLat2);
            double minLon = Math.min(destLon1, destLon2);
            double maxLon = Math.max(destLon1, destLon2);

            return minLat <= lat && lat <= maxLat && minLon <= lon && lon <= maxLon;
        } catch (JSONException e) {
            return false;
        }
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

    // The default score this function returns is 2 * EARTH_RADIUS.
    // For a distance x between the device and the phone, score += EARTH_RADIUS - x
    // For a time t to reach the zone, score += t
    // If we don't have a estimated time, score += EARTH_RADIUS (i.e. a penalty)
    private double getLocationScore(JSONObject destination) throws JSONException {
        if (destination == null || location == null)
            return 2 * Constants.EARTH_RADIUS;

        double score;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        double destLat1 = destination.getDouble(Warning.WARNING_LAT_START);
        double destLat2 = destination.getDouble(Warning.WARNING_LAT_END);
        double destLon1 = destination.getDouble(Warning.WARNING_LON_START);
        double destLon2 = destination.getDouble(Warning.WARNING_LON_END);
        
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

        // Use the centre location of the zone as our destination point.
        double destLat = (destLat1 + destLat2) / 2;
        double destLon = (destLon1 + destLon2) / 2;

        // Calculate the smallest distance between the device and the location area.
        double minDistance = haversine(lat, lon, destLat, destLon);
        Log.d(TAG, "minDistance: " + minDistance);

        // Pythagorean attempt at minDistance (using Spherical Pythagorean Theorem)
        // https://www.math.hmc.edu/funfacts/ffiles/20006.2.shtml
        // Only calculates distance to nearest corner (does not account for 'x' offset due to bearing).
        double dLat = Math.min(Math.abs(lat - destLat1), Math.abs(lat - destLat2));
        double dLon = Math.min(Math.abs(lon - destLon1), Math.abs(lon - destLon2));
        double pMinDistance = Constants.EARTH_RADIUS * Math.acos(
                Math.cos(dLat * dLat / Constants.EARTH_RADIUS) * Math.cos(dLon * dLon / Constants.EARTH_RADIUS));
        Log.d(TAG, "pMinDistance: " + pMinDistance);

        double azimuthR = Math.acos((Math.cos(dLon) - Math.cos(pMinDistance) * Math.cos(dLat)) / Math.sin(dLat) * Math.sin(pMinDistance));
        Log.d(TAG, "azimuth: " + azimuthR);

        // TODO: Is this already degrees?
        // double bearing = Math.toDegrees(location.getBearing());

        score = Constants.EARTH_RADIUS - minDistance;
        Log.d(TAG, "Distance score: " + score);

        // If we have movement information, calculate how soon this device will reach the target location.
        if (location.hasBearing() && location.hasSpeed()) {
            // Calculate time till arrival and add it on to the score
            // If the device is travelling away from the destination we'd want to take away some amount from the score.
            // double bearing = location.getBearing();
            // double speed = location.getSpeed();

            // double azimuth = Math.toDegrees(azimuth(lat, lon, destLat, destLon));

            // We want to calculate whether travelling along the bearing from lat,lon will put us in the destination zone.
        } else {
            Log.d(TAG, "No movement information");
            score += Constants.EARTH_RADIUS;
        }

        return score;
    }

    // An implementation of the Haversine formula, which computes the great-circle distance between two points on a
    // sphere. https://www.movable-type.co.uk/scripts/latlong.html
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);

        double diffLat = Math.toRadians(Math.abs(lat2 - lat1));
        double diffLon = Math.toRadians(Math.abs(lon2 - lon1));

        double a = Math.pow(Math.sin(diffLat / 2), 2) +
                   Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(diffLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Constants.EARTH_RADIUS * c;
    }

    // Returns the initial bearing (in radians) of the line from lat1,lon1 to lat2,lon2.
    // https://www.movable-type.co.uk/scripts/latlong.html
    private double azimuth(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double diffLon = Math.toRadians(Math.abs(lon2 - lon1));

        return Math.atan2(Math.sin(diffLon) * Math.cos(phi2),
                          Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(diffLon));
    }

}
