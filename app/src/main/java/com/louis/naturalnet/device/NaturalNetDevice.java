package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.util.Log;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.signal.SignalQuality;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import static com.louis.naturalnet.device.Quadrant.*;

class Point {
    double x;
    double y;
}

class Journey {
    double distance;
    double bearing;
}

enum Quadrant {
    N, NE, E, SE, S, SW, W, NW, DEST
}

public class NaturalNetDevice {

    private static final String TAG = "NetDevice";

    private static final int DEFAULT_LOCATION_SCORE = 1000;

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
            score += DEFAULT_LOCATION_SCORE;
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
            return 0;

        double score = DEFAULT_LOCATION_SCORE;
        double lat = Math.toRadians(location.getLatitude());
        double lon = Math.toRadians(location.getLongitude());
        double destLat1 = Math.toRadians(destination.getDouble(Warning.WARNING_LAT_START));
        double destLat2 = Math.toRadians(destination.getDouble(Warning.WARNING_LAT_END));
        double destLon1 = Math.toRadians(destination.getDouble(Warning.WARNING_LON_START));
        double destLon2 = Math.toRadians(destination.getDouble(Warning.WARNING_LON_END));

        /* Test locations - use these on three devices to test decision making. */

        /*
        if (device.getName().equals("net0")) {
            // Closest to the test destination
            lat = Math.toRadians(51.538331);
            lon = Math.toRadians(-0.154256);
        } else if (device.getName().equals("net1")) {
            // In between location
            lat = Math.toRadians(51.538106);
            lon = Math.toRadians(-0.151417);
        } else if (device.getName().equals("net2")) {
            // Farthest from test destination
            lat = Math.toRadians(51.537782);
            lon = Math.toRadians(-0.148846);
        }
        */

        // Convert coordinates to Cartesian coordinates (this source doesn't use Earth radius)
        // https://stackoverflow.com/questions/5983099/converting-longitude-latitude-to-x-y-coordinate
        Point p = new Point();
        p.x = lon;
        p.y = Math.log(Math.tan(Math.PI / 4 + lat / 2));

        Point a = new Point();
        a.x = destLon1;
        a.y = Math.log(Math.tan(Math.PI / 4 + destLat1 / 2));

        Point b = new Point();
        b.x = destLon2;
        b.y = Math.log(Math.tan(Math.PI / 4 + destLat2 / 2));

        double maxX = Math.max(a.x, b.x);
        double maxY = Math.max(a.y, b.y);
        double minX = Math.min(a.x, b.x);
        double minY = Math.min(a.y, b.y);

        Quadrant quadrant = getQuadrant(p, minX, maxX, minY, maxY);

        Journey journey = getMinJourney(p, quadrant, minX, maxX, minY, maxY);

        score -= journey.distance;

        if (location.hasBearing() && location.hasSpeed()) {
            double bearing = Math.toRadians(location.getBearing());
            double speed = location.getSpeed();

            // Find out whether the device will collide with the area
            double maxBearing = getMaxBearing(p, quadrant, minX, maxX, minY, maxY);
            double minBearing = getMinBearing(p, quadrant, minX, maxX, minY, maxY);

            if (bearing >= minBearing && bearing <= maxBearing) {
                double distance = getDistanceToArrival();
                score -= distance / speed;
            }

            /*
            double idealBearing = Math.atan(dX / dY);
            // tan(bearing) = dX + x / dY
            if (bearing == idealBearing) {
                // min distance
            } else if (bearing >= 0 && bearing < Math.PI / 4) {
                // offsetY
            } else if (bearing >= Math.PI / 4 && bearing < Math.PI / 2) {
                double offsetX = Math.tan(bearing) * dY - dX;
                distance = Math.sqrt(dY * dY + (dX + offsetX) * (dX + offsetX));
            }
            */
        }

        return score;
    }

    private Quadrant getQuadrant(Point p, double minX, double maxX, double minY, double maxY) {
        boolean withinX = p.x >= minX && p.x <= maxX;
        boolean withinY = p.y >= minY && p.y <= maxY;
        boolean aboveX = p.x > maxX;
        boolean belowX = p.x < minX;
        boolean aboveY = p.y > maxY;

        if (aboveY) {
            if (belowX)
                return NW;
            if (withinX)
                return N;
            if (aboveX)
                return NE;
        } else if (withinY) {
            if (belowX)
                return W;
            if (withinX)
                return DEST;
            if (aboveX)
                return E;
        } else {
            if (belowX)
                return SW;
            if (withinX)
                return S;
        }

        return SE;
    }

    private Journey getMinJourney(Point p, Quadrant q, double minX, double maxX, double minY, double maxY) {
        Journey journey = new Journey();
        journey.bearing = 0;
        journey.distance = 0;

        if (q == DEST)
            return journey;

        double dX = 0;
        double dY = 0;

        switch (q) {
            case N:
                journey.distance = p.y - maxY;
                journey.bearing = Math.PI;
                return journey;
            case E:
                journey.distance = p.x - maxX;
                journey.bearing = 3 * Math.PI / 2;
                return journey;
            case S:
                journey.distance = minY - p.y;
                journey.bearing = 0;
                return journey;
            case W:
                journey.distance = minX - p.x;
                journey.bearing = Math.PI / 2;
                return journey;
            case NE:
                dX = p.x - maxX;
                dY = p.y - maxY;
                journey.bearing = 3 * Math.PI / 2 - Math.atan(dY / dX);
                break;
            case SE:
                dX = p.x - maxX;
                dY = minY - p.y;
                journey.bearing = Math.PI + Math.atan(dY / dX);
                break;
            case SW:
                dX = minX - p.x;
                dY = minY - p.y;
                journey.bearing = Math.atan(dX / dY);
                break;
            case NW:
                dX = minX - p.x;
                dY = p.y - maxY;
                journey.bearing = Math.PI - Math.atan(dX / dY);
                break;
        }

        journey.distance = Math.sqrt(dX * dX + dY * dY);
        return journey;
    }

    private double getMaxBearing(Point p, Quadrant q, double minX, double maxX, double minY, double maxY) {
        double x;
        double y;
        double dX;
        double dY;

        switch (q) {
            // TODO: With this syntax, will case N execute the code inside case NE?
            // Bearing points to top left point.
            case N:
            case NE:
                x = minX;
                y = maxY;

                dX = p.x - x;
                dY = p.y - y;

                return Math.PI + Math.atan(dX / dY);
            // Bearing points to top right point.
            case E:
            case SE:
                x = maxX;
                y = maxY;

                dX = p.x - x;
                dY = y - p.y;

                return 3 * Math.PI / 2 + Math.atan(dY / dX);
            // Bearing points to top left point, except we are now underneath the point (dY differs to N, NE).
            case S:
                x = minX;
                y = maxY;

                dX = p.x - x;
                dY = y - p.y;

                return 3 * Math.PI / 2 + Math.atan(dY / dX);
            // Bearing points to bottom right point.
            case SW:
                x = maxX;
                y = minY;

                dX = x - p.x;
                dY = y - p.y;

                return Math.atan(dX / dY);
            // Bearing points to bottom left point.
            case W:
            case NW:
                x = minX;
                y = minY;

                dX = x - p.x;
                dY = p.y - y;

                return Math.PI / 2 + Math.atan(dY / dX);
        }

        // We don't really want to arrive here - we could add a default case for the switch. We shouldn't receive
        // the DEST case either.
        return -1;
    }

    private double getMinBearing(Point p, Quadrant q, double minX, double maxX, double minY, double maxY) {
        double x;
        double y;
        double dX;
        double dY;

        switch (q) {
            // Bearing points to top right point.
            case N:
            case NW:
                x = maxX;
                y = maxY;

                dX = x - p.x;
                dY = p.y - y;

                return Math.PI / 2 + Math.atan(dY / dX);
            // Bearing points to bottom right point.
            case NE:
            case E:
                x = maxX;
                y = minY;

                dX = p.x - x;
                dY = p.y - y;

                return Math.PI + Math.atan(dX / dY);
            // Bearing points to bottom left point.
            case SE:
                x = minX;
                y = minY;

                dX = p.x - x;
                dY = y - p.y;

                return 3 * Math.PI / 2 + Math.atan(dY / dX);
            // Bearing points to top right point.
            case S:
                x = maxX;
                y = maxY;

                dX = x - p.x;
                dY = y - p.y;

                return Math.atan(dX / dY);
            // Bearing points to top left point.
            case SW:
            case W:
                x = minX;
                y = maxY;

                dX = x - p.x;
                dY = y - p.y;

                return Math.atan(dX / dY);
        }

        return -1;
    }

    private double getDistanceToArrival() {
        return -1;
    }

}
