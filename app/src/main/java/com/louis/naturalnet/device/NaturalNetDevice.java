package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import com.louis.naturalnet.signal.SignalQuality;
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

    public String getAddress() {
        return device.getAddress();
    }

    private void parseMetadata(JSONObject metadata) {
        signalQuality = DeviceInformation.getSignalQuality(metadata);
        gpsQuality = DeviceInformation.getGpsQuality(metadata);
        batteryLevel = DeviceInformation.getBatteryLevel(metadata);
        queueLength = DeviceInformation.getQueueLength(metadata);
    }

    // Could add scoring functions in here.

}
