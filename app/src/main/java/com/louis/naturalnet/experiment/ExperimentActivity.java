package com.louis.naturalnet.experiment;

import com.louis.naturalnet.R;
import com.louis.naturalnet.utils.Constants;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class ExperimentActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener,
LocationListener, OnMapReadyCallback {

	Experiment exp;
	GoogleMap mMap = null;
	Context mContext;
	GoogleApiClient mGoogleApiClient = null;
	public static Location myLocation = null;

	private final String TAG = "ExperimentActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_experiment);
		mContext = this;
		Intent intent = getIntent();
		final int index = intent.getIntExtra(Constants.INTENT_KEY_POSITION, -1);

		exp = ExperimentList.experimentList.get(index);

//		TextView expNameTv = (TextView) findViewById(R.id.experiment_name);
//		expNameTv.setText(exp.name);

//		TextView expDescTv = (TextView) findViewById(R.id.experiment_desc);
//		expDescTv.setText(exp.description);

//		TextView expTimeTv = (TextView) findViewById(R.id.experiment_time);
//		expTimeTv.setText(exp.endTime);
		
//		Button buttonJoin = (Button) findViewById(R.id.btn_join);
//		buttonJoin.setOnClickListener(new OnClickListener(){
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				for(Experiment exp : ExperimentList.experimentList){
//					exp.joined = 0;
//				}
//				ExperimentList.experimentList.get(index).joined = 1;
//				ExperimentList.joinedExp = new Experiment(ExperimentList.experimentList.get(index));
//				Constants.EXPERIMENT_ID = ExperimentList.joinedExp.id;
//				Log.d(TAG, ExperimentList.joinedExp.id);
//				Toast.makeText(mContext, "Joined the experiment", Toast.LENGTH_LONG).show();
////				WebServerConnector.getInstance(mContext).getAllExperiments();
//				ExperimentList.experimentListAdapter.notifyDataSetChanged();
//				ExperimentList.experimentListAdapter.sortList();
//				Constants.EXPERIMENT_ID = ExperimentList.joinedExp.id;
//			}
//		});

		//		TextView contactTv = (TextView) findViewById(R.id.network_contact);
		//		contactTv.setText(String.valueOf(QueueManager.getInstance(mContext).contacts));
		//
		//		TextView queueLenTv = (TextView) findViewById(R.id.network_queue_len);
		//		queueLenTv.setText(String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));
		//
		//		TextView receivedTv = (TextView) findViewById(R.id.network_received);
		//		receivedTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsReceived));
		//
		//		TextView sentTv = (TextView) findViewById(R.id.network_sent);
		//		sentTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsSent));
		//
		//		TextView creditTv = (TextView) findViewById(R.id.network_credit);
		//		creditTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsSent + QueueManager.getInstance(mContext).packetsReceived));	

//		((SupportMapFragment)  getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(LocationServices.API)
			.build();
		}
	}

	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	protected void onStop() {
		if (mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
		}
		super.onStop();
	}

	Marker experimentMarker = null;

	@Override
	public void onMapReady(GoogleMap map) {
		// TODO Auto-generated method stub
		mMap = map;
		mMap.setMyLocationEnabled(true);

		if(exp.area.size() > 0){
			double lat = 0;
			double lng = 0;

			PolygonOptions polygonOptions = new PolygonOptions();

			for(LatLng latLng : exp.area){
				polygonOptions.add(latLng);
				lat += latLng.latitude;
				lng += latLng.longitude;
			}

			lat /= exp.area.size();
			lng /= exp.area.size();

			polygonOptions.strokeColor(Color.RED).fillColor(Color.BLUE);
			Polygon polygon = map.addPolygon(polygonOptions);

			CameraUpdate experimentLoc = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 8);
			map.animateCamera(experimentLoc);
		}
	}

	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(@Nullable Bundle arg0) {
		// TODO Auto-generated method stub
		Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
				mGoogleApiClient);
		if (mLastLocation != null) {
			myLocation = new Location(mLastLocation);
		}
		mLocationRequest = new LocationRequest();
		if (mLocationRequest != null) {
			startLocationUpdates();
		}
	}

	LocationRequest mLocationRequest = null;

	protected void startLocationUpdates() {
		Log.d(TAG, "start location update");
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(10000);
		mLocationRequest.setFastestInterval(5000);
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub

	}

	public LatLng locationFromPostCode(String postCode){

		LatLng loc = null;

		Geocoder geocoder1 = new Geocoder(this);
		try {
			List<Address> addresses1 = geocoder1.getFromLocationName(postCode, 1);
			if (addresses1 != null && !addresses1.isEmpty()) {
				Address address1 = addresses1.get(0);
				// Use the address as needed
				loc = new LatLng(address1.getLatitude(), address1.getLongitude());
			} else {
				// Display appropriate message when Geocoder services are not available
				Toast.makeText(this, "Unable to geocode zipcode", Toast.LENGTH_LONG).show(); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return loc;
	}
}
