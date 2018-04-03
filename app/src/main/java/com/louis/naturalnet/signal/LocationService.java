package com.louis.naturalnet.signal;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;

/*
    https://developer.android.com/training/location/receive-location-updates.html
    https://blog.lemberg.co.uk/fused-location-provider

    This class uses the Google Fused Location Provider API which manages Android's location providing capabilities
    through both Network and GPS utilities. It will probably provide the most comprehensive location in any given
    situation.

    Genymotion is not able to alter the location of the phone using this API. Can either implement a GPS only
    system, mock the location provider, or run on a physical device.
 */

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationService";

    // Time between location requests (millis)
    private static final long REQUEST_INTERVAL = 1000;
    private static final long FASTEST_REQUEST_INTERVAL = 1000;

    private GoogleApiClient googleApiClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");

//        if (googleApiClient == null || !googleApiClient.isConnected())
        startTrackingLocation();

        return START_STICKY;
    }

    private void startTrackingLocation() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d(TAG, "Built googleApiClient");

        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting())
            googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(REQUEST_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_REQUEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* Create an initial location request

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();

        PendingResult<LocationSettingsResult> location = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, settingsRequest);
        */

        Log.d(TAG, "Connected");

        try {
            /*
                Might be better to use requestLocationUpdates with a PendingIntent parameter instead of a
                LocationListener - depends on how we can send the location updates to the rest of the app.
             */
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } catch (SecurityException e) {
            // TODO: We don't have permission to access the location
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO: Research suspended case
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location Changed");
        Log.d(TAG, location.toString());

        // Announce location
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
