package com.louis.naturalnet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.louis.naturalnet.fragments.NetworkFragment;
import com.louis.naturalnet.fragments.WarningFragment;
import com.louis.naturalnet.signal.LocationService;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "NaturalNet:";

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

//        boolean googleApiAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
//        Log.d(TAG, "Google Api Availability: " + googleApiAvailable);

        startLocationTracking();
    }

    private void startLocationTracking() {
        startService(new Intent(getBaseContext(), LocationService.class));

        /*
        Context context = getBaseContext();
        Intent locationTrackerIntent = new Intent(context, LocationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, locationTrackerIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                60000,
                pendingIntent);
        */
    }
}
