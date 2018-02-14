package com.louis.naturalnet.http;

import com.louis.naturalnet.account.AccountManager;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.experiment.Experiment;
import com.louis.naturalnet.experiment.ExperimentList;
import com.louis.naturalnet.utils.Constants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class WebServerConnector extends BroadcastReceiver{

	private final String ENTITY_URI = "urn:oc:entity:experimenters:";

	private final static String TAG = "web server connector";
	private final static String serverUrl = "https://exp.orion.organicity.eu/v2/entities";

	private final static String EXPERIMENT_URL = "http://experimenters.organicity.eu:8081/";

	private static Context mContext = null;

	private static long updatedTime;

	private static WebServerConnector _obj = null;

	private WebServerConnector(Context context){
		mContext = context;
		updatedTime = System.currentTimeMillis();
	}

	public static WebServerConnector getInstance(Context context){
		if(_obj == null){
			_obj = new WebServerConnector(context);
		}
		return _obj;
	}

	public WebServerConnector(){}

	public void registerDevice(){
		new RegisterDeviceTask().execute();
	}

	public void getAllExperiments(){
		new GetAllExperimentsTask().execute();
	}

	private class GetAllExperimentsTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... data) {
			Log.d(TAG, "Get experiment list from the server");

			URL url;
			HttpURLConnection connection = null;
			try{
				url = new URL(EXPERIMENT_URL + "allexperiments");
				connection = (HttpURLConnection)url.openConnection();
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestMethod("GET");
				connection.connect();

				InputStream in = new BufferedInputStream(connection.getInputStream());
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
				StringBuilder responseStrBuilder = new StringBuilder();

				String inputStr;
				while ((inputStr = streamReader.readLine()) != null)
					responseStrBuilder.append(inputStr);
				JSONObject json = new JSONObject(responseStrBuilder.toString());

				JSONArray expArray = json.getJSONArray("experiments");

				ArrayList<Experiment> updatedExp = new ArrayList<Experiment>();

				for(int i=0; i < expArray.length(); i++){
					Experiment exp = new Experiment();
					JSONObject expJSON = (JSONObject) expArray.get(i);
					exp.id = expJSON.getString("experimentId");
					exp.name = expJSON.getString("name");
					exp.description = expJSON.getString("description");
					exp.startTime = expJSON.getString("startDate");
					exp.endTime = expJSON.getString("endDate");
					JSONArray areaArray = expJSON.getJSONArray("area");
					if(areaArray.length() > 0){
						JSONObject area = (JSONObject) areaArray.get(0);
						JSONArray areaCoord = area.getJSONArray("coordinates");
						for(int j=0; j<areaCoord.length(); j++){

							if(areaCoord.getJSONArray(j).length() > 1){
								try{
									exp.area.add(new LatLng(areaCoord.getJSONArray(j).getDouble(1), areaCoord.getJSONArray(j).getDouble(0)));
								}catch(JSONException e){
									e.printStackTrace();
								}
							}
						}
					}
					if(ExperimentList.joinedExp != null){
						if(exp.id.compareToIgnoreCase(ExperimentList.joinedExp.id) == 0){
							exp.joined = 1;
							ExperimentList.joinedExp = new Experiment(exp);
						}
					}
					updatedExp.add(exp);
				}
				ExperimentList.update(updatedExp);
			} catch (IOException e) {
				// writing exception to log
				e.printStackTrace();
				serverAvailable = false;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			ExperimentList.experimentListAdapter.notifyDataSetChanged();
			ExperimentList.experimentListAdapter.sortList();
			super.onPostExecute(result);
		}
	}

	public void sendSensorData(String data){
		new SendSensorDataTask().execute(data);
	}

	private static boolean serverAvailable = true;

	private class SendSensorDataTask extends AsyncTask<String, Void, Void> {
		protected Void doInBackground(String... data) {

			String entityId = ENTITY_URI + Constants.EXPERIMENTER_ID + ":" + Constants.EXPERIMENT_ID + ":" + AccountManager.getManager().getParsedInfo().getSubject() + ":" + data[2];

			Log.d(TAG, "send data to server");

			JSONObject content = new JSONObject();
			try {
				// experiment id
				// application id
				// user id
				content.put("id", entityId);
				content.put("type", "urn:oc:entityType:OppNetData");
				JSONObject dataAttr = new JSONObject();
				dataAttr.put("type", "urn:oc:attributeType:illuminance");
				dataAttr.put("value", data[1]);
				content.put("illuminance", dataAttr);
				JSONObject pathAttr = new JSONObject();
				pathAttr.put("type", "urn:oc:attributeType:oppnet:path");
				pathAttr.put("value", data[0]);
				content.put("path", pathAttr);
				JSONObject delayAttr = new JSONObject();
				delayAttr.put("type", "urn:oc:attributeType:oppnet:delay");
				delayAttr.put("value", data[3]);
				content.put("delay", delayAttr);
				// data
				JSONObject timeInstant = new JSONObject();
				timeInstant.put("type", "urn:oc:attributeType:ISO8601");
				TimeZone tz = TimeZone.getTimeZone("UTC");
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
				df.setTimeZone(tz);
				String nowAsISO = df.format(new Date());
				timeInstant.put("value", nowAsISO);
				content.put("TimeInstant", timeInstant);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			URL url;
			HttpsURLConnection connection = null;
			try{
				Log.d(TAG, serverUrl + entityId);

				url = new URL(serverUrl);
				connection = (HttpsURLConnection)url.openConnection();
				connection.setSSLSocketFactory(trainCA().getSocketFactory());
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("X-Organicity-Application", Constants.APPLICATION_ID);
				connection.setRequestProperty("X-Organicity-Experiment", Constants.EXPERIMENT_ID);
				connection.setRequestProperty("Authorization", String.format("Bearer %s", AccountManager.getManager().getToken()));
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.connect();

				OutputStream out = new BufferedOutputStream(connection.getOutputStream());
				out.write(content.toString().getBytes());
				out.close();

				Log.d(TAG, String.valueOf(connection.getResponseCode()));
				Log.d(TAG, connection.getResponseMessage());
				Log.d(TAG, connection.getCipherSuite());

				InputStream in = new BufferedInputStream(connection.getInputStream());
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
				StringBuilder responseStrBuilder = new StringBuilder();

				String inputStr;
				while ((inputStr = streamReader.readLine()) != null)
					responseStrBuilder.append(inputStr);
				Log.d(TAG, responseStrBuilder.toString());

			} catch (IOException e) {
				// writing exception to log
				e.printStackTrace();
				serverAvailable = false;
			} finally {
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}
	}

	private class RegisterDeviceTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... params) {
			// check if exists

			String entityId = ENTITY_URI + Constants.EXPERIMENTER_ID + ":" + Constants.EXPERIMENT_ID + ":" + AccountManager.getManager().getParsedInfo().getSubject() + ":" + Secure.getString(mContext.getContentResolver(),
					Secure.ANDROID_ID); // unique identifier of the device

			Log.d(TAG, entityId);

			URL url;
			HttpsURLConnection connection = null;
			// register to orion

			JSONObject content = new JSONObject();
			try {
				content.put("id", entityId);
				content.put("type", "urn:oc:entityType:smartPhone");
				JSONObject timeInstant = new JSONObject();
				timeInstant.put("type", "urn:oc:attributeType:ISO8601");
				TimeZone tz = TimeZone.getTimeZone("UTC");
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
				df.setTimeZone(tz);
				String nowAsISO = df.format(new Date());
				timeInstant.put("value", nowAsISO);
				content.put("TimeInstant", timeInstant);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Log.d(TAG, String.format("Bearer %s", AccountManager.getManager().getToken()));

			try{
				url = new URL(serverUrl);
				connection = (HttpsURLConnection)url.openConnection();
				connection.setSSLSocketFactory(trainCA().getSocketFactory());
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("X-Organicity-Application", Constants.APPLICATION_ID);
				connection.setRequestProperty("X-Organicity-Experiment", Constants.EXPERIMENT_ID);
				connection.setRequestProperty("Authorization", String.format("Bearer %s", AccountManager.getManager().getToken()));
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.connect();

				OutputStream out = new BufferedOutputStream(connection.getOutputStream());
				out.write(content.toString().getBytes());
				out.close();

				Log.d(TAG, String.valueOf(connection.getResponseCode()));
				Log.d(TAG, connection.getResponseMessage());
				Log.d(TAG, connection.getCipherSuite());

				InputStream in = new BufferedInputStream(connection.getInputStream());
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
				StringBuilder responseStrBuilder = new StringBuilder();

				String inputStr;
				while ((inputStr = streamReader.readLine()) != null)
					responseStrBuilder.append(inputStr);
				Log.d(TAG, responseStrBuilder.toString());

			} catch (IOException e) {
				// writing exception to log
				e.printStackTrace();
			} finally {
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
			// Do whatever
			Log.d(TAG, "wifi connected");
			//			DataManager.getInstance(mContext).saveLog("wifi connected");

			serverAvailable = true;

			// send data to sink
			long currentTime = System.currentTimeMillis();

			if(mContext != null){
				if(currentTime - updatedTime > 1000*60*1){
					uploadSensorData(currentTime);
				}
			}
		}
	}

	public void uploadSensorData(long currentTime){
		while(QueueManager.getInstance(mContext).getQueueLength() > 0 & serverAvailable){
			String[] data = QueueManager.getInstance(mContext).getFromQueue();
			new SendSensorDataTask().execute(data);
		}
		QueueManager.getInstance(mContext).updateName();
		Log.d(TAG, "update name to " + String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));
		new GetAllExperimentsTask().execute();
		//		MainActivity.txMyQueueLen.setText(String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));
		updatedTime = currentTime;
	}

	private SSLContext trainCA(){
		// Load CAs from an InputStream
		// (could be from a resource or ByteArrayInputStream or ...)
		CertificateFactory cf = null;
		try {
			cf = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		InputStream caInput = Thread.currentThread().getContextClassLoader().getResourceAsStream("isrgrootx1.pem");

		if(caInput == null)
			caInput = this.getClass().getResourceAsStream("lets-encrypt-x3-cross-signed.pem");
		Certificate ca = null;
		try {
			ca = cf.generateCertificate(caInput);
			Log.d("test", "ca=" + ((X509Certificate) ca).getSubjectDN());
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				caInput.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore;
		SSLContext context = null;
		try {
			keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);

			// Create a TrustManager that trusts the CAs in our KeyStore
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);

			// Create an SSLContext that uses our TrustManager
			context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), null);
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return context;
	}
}