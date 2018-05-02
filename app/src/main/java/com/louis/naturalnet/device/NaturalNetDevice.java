package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import com.louis.naturalnet.signal.SignalQuality;
import com.louis.naturalnet.utils.Constants;
import org.json.JSONObject;

// Can't extend BluetoothDevice because we aren't in the android.bluetooth package.
public class NaturalNetDevice {

    private BluetoothDevice device;

    private SignalQuality signalQuality;
    private SignalQuality gpsQuality;
    private int batteryLevel;
    private int queueLength;

    public NaturalNetDevice(BluetoothDevice device, JSONObject metadata) {
        this.device = device;
        parseMetadata(metadata);
    }

    private void parseMetadata(JSONObject metadata) {
        signalQuality = DeviceInformation.parseSignalQuality(metadata);
        gpsQuality = DeviceInformation.parseGpsQuality(metadata);
        batteryLevel = DeviceInformation.parseBatteryLevel(metadata);
        queueLength = DeviceInformation.parseQueueLength(metadata);
    }

    public String getAddress() {
        return device.getAddress();
    }

    public double getScore() {
        return (DeviceInformation.getQueueLength() - this.queueLength) +
                Constants.ENERGY_PENALTY_COEFF * (batteryLevel - DeviceInformation.getBatteryLevel());
    }

    // Could add scoring functions in here.

}
