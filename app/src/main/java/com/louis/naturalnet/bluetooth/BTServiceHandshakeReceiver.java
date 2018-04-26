package com.louis.naturalnet.bluetooth;

import android.bluetooth.BluetoothDevice;

interface BTServiceHandshakeReceiver {
    void connectionFailed(BluetoothDevice device);
    void receivedMetadata(BluetoothDevice device, Object buffer);
}
