package com.louis.naturalnet.signal;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.louis.naturalnet.utils.Constants;

/*
    https://developer.android.com/training/location/receive-location-updates.html
    https://blog.lemberg.co.uk/fused-location-provider

    This class uses the Google Fused Location Provider API which manages Android's location providing capabilities
    through both Network and GPS utilities. It will probably provide the most comprehensive location in any given
    situation.

    Genymotion is not able to alter the location of the phone using this API. Can either implement a GPS only
    system, mock the location provider, or run on a physical device.

    This works well on a physical device.
 */

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LocationService";

    private GoogleApiClient googleApiClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");

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
        locationRequest.setInterval(Constants.LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(Constants.LOCATION_REQUEST_INTERVAL_FASTEST);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG, "Connected");

        try {
            Intent intent = new Intent("com.louis.naturalnet.signal.LocationReceiver");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, pendingIntent);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
