package com.louis.naturalnet.device;

import android.bluetooth.BluetoothDevice;
import org.json.JSONObject;

// Can't extend BluetoothDevice because we aren't in the android.bluetooth package.
public class NaturalNetDevice {

    private BluetoothDevice device;

    public NaturalNetDevice(BluetoothDevice device, JSONObject metadata) {
        this.device = device;
        // Parse metadata.
    }

    public String getAddress() {
        return device.getAddress();
    }

}
