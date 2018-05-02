package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import com.louis.naturalnet.signal.SignalQuality;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONObject;

public class NaturalNetDevice {

    private BluetoothDevice device;

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

    public double getScore() {
        return (DeviceInformation.getQueueLength() - this.queueLength) +
                Constants.ENERGY_PENALTY_COEFF * (batteryLevel - DeviceInformation.getBatteryLevel());
    }

    public Location getLocation() {
        return location;
    }

    // Could add scoring functions in here.

}
