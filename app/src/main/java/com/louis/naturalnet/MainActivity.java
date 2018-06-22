package com.louis.naturalnet;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.louis.naturalnet.bluetooth.BTManager;
import com.louis.naturalnet.bluetooth.BTScanningAlarm;
import com.louis.naturalnet.fragments.NetworkFragment;
import com.louis.naturalnet.fragments.WarningFragment;
import com.louis.naturalnet.signal.LocationService;
import com.louis.naturalnet.device.DeviceInformation;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "NaturalNet";

    private BTManager mBTManager;

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"Started main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the default content view
        setContentView(R.layout.main_layout);

        if (findViewById(R.id.warning_fragment_container) != null) {
            WarningFragment warningFragment = new WarningFragment();
            getFragmentManager().beginTransaction().add(R.id.warning_fragment_container, warningFragment).commit();
        }

        if (findViewById(R.id.network_fragment_container) != null) {
            NetworkFragment networkFragment = new NetworkFragment();
            getFragmentManager().beginTransaction().add(R.id.network_fragment_container, networkFragment).commit();
        }

        // May choose to issue some warning if we can't access the Google API
        // boolean googleApiAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
        // Log.d(TAG, "Google Api Availability: " + googleApiAvailable);

        DeviceInformation.listenToSignals(this);

        startLocationTracking();

        if (mBTManager == null) {
            mBTManager = new BTManager(getBaseContext());
            mBTManager.registerBroadcastReceivers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBTManager.initBluetoothUtils(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BTScanningAlarm.stopScanning(this);
        mBTManager.unregisterBroadcastReceivers();

        stopLocationTracking();
    }

    private void startLocationTracking() {
        startService(new Intent(getBaseContext(), LocationService.class));
    }

    private void stopLocationTracking() {
        stopService(new Intent(getBaseContext(), LocationService.class));
    }

    // Initial request comes from BTManager wanting BTAdapter to be enabled
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BTManager.REQUEST_BT_ENABLE:
                // BTManager requests the Activity to enable Bluetooth (the Bluetooth Adapter)

                if (resultCode == RESULT_OK) {
                    // The BTAdapter has become enabled

                    // Reinitialise BT Utils
                    mBTManager.initBluetoothUtils(this);

                    // Request to become discoverable
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                            BTManager.RESULT_BT_DISCOVERABLE_DURATION);
                    startActivityForResult(discoverableIntent, BTManager.REQUEST_BT_DISCOVERABLE);
                } else {
                    Log.d(TAG, "Bluetooth is not enabled by the user.");
                }
                break;
            case BTManager.REQUEST_BT_DISCOVERABLE:
                // Request for BT to become discoverable

                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Bluetooth is not discoverable.");
                } else {
                    Log.d(TAG, "Bluetooth is discoverable by 300 seconds.");
                }
                break;
            default:
                break;
        }
    }
}
