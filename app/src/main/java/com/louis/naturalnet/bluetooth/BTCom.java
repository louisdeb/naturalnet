package com.louis.naturalnet.bluetooth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * a multi thread BT utility
 * @author fshi
 *
 */
class BTCom {

	private static final String TAG = "BTCom";

	private final String BTRecordName = "com.louis.bluetooth";
	final static int BT_CLIENT_CONNECT_FAILED = 10404;
	final static int BT_CLIENT_CONNECTED = 10401;
	final static int BT_SERVER_CONNECTED = 10402;
	final static int BT_DATA = 10403;
	final static int BT_CLIENT_ALREADY_CONNECTED = 10405;
	final static int BT_DISCONNECTED = 10406;
	final static int BT_SUCCESS = 10407;
	final static String BT_DATA_CONTENT = "bt_data"; // data received from another device
	final static String BT_DEVICE_MAC = "bt_device_mac"; // mac address of the communicating device
	final static String BT_DEVICE_NAME = "bt_device_name"; // name of the peer device

	// Default UUID	list.
	private static UUID MY_UUID = UUID.fromString("8113ac40-438f-11e1-b86c-0800200c9a60");

	private BluetoothAdapter mBluetoothAdapter;

	// All received messages are sent through this messenger to its parent.
	private Messenger mMessenger = null;

	private ArrayList<ConnectedThread> connections = new ArrayList<>();

	// Current connection state, only one server thread and one client thread.
	private ServerThread mServerThread;

	private final static StringBuffer sbLock = new StringBuffer();

	private Handler timeoutHandler = new Handler();

	private static BTCom _this = null;

	// Constructor. Prepares a new BT interface.
	private BTCom() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	static BTCom getInstance(){
		if(_this == null){
			_this = new BTCom();
		}
		return _this;
	}

	void setCallback(Messenger callback) {
		if(mMessenger == null)
			mMessenger = callback;
	}

	// Used to managed connections
	synchronized void stopConnection(String MAC){
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC)) {
				connection.cancel();
				connections.remove(connection);
				break;
			}
		}

		Log.d(TAG, "active connection # " + String.valueOf(connections.size()));
	}

	// Used to send data to a dest
	synchronized void send(String MAC, JSONObject data){
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC))
				connection.writeObject(data);
		}
	}

	// Used to send data to a dest
	synchronized void send(String MAC, String data){
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC))
				connection.writeString(data);
		}
	}

    // Start listening.
	synchronized void startServer(){
		stopServer();
		mServerThread = new ServerThread(MY_UUID);             
		mServerThread.start();

		Log.d(TAG, "Server started");
	}

    // Stop listening.
	void stopServer(){
		if (mServerThread != null)
			mServerThread.cancel();
	}

    // Connect a BT device.
	synchronized void connect(BluetoothDevice btDevice, long timeout){
		// Start the thread to connect with the given device
		ClientThread clientThread = new ClientThread(btDevice, sbLock, MY_UUID, timeout);
		clientThread.start();
	}

    // Get the number of active connections.
	private int getActiveConnectionsCount() {
		return connections.size();
	}

    // Start scan service
	void startScan(boolean scanStart, long duration) {
		if (scanStart) {
			if (getActiveConnectionsCount() == 0) {

                // If scan is already started, do nothing.
				if (mBluetoothAdapter.isDiscovering()) {
                    // mBluetoothAdapter.cancelDiscovery();
					return;
				}

				// if (getActiveConnectionsCount() == 0) {
				mBluetoothAdapter.startDiscovery();

				// Cancel the discovery process after SCAN_INTERVAL.
				final Handler discoveryHandler = new Handler();
				discoveryHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mBluetoothAdapter.isDiscovering()){
							mBluetoothAdapter.cancelDiscovery();
						}
					}
				}, duration);
			}
		} else {
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	/**
	 * BT Server thread
	 * @author fshi
	 *
	 */
	private class ServerThread extends Thread {
		private final BluetoothServerSocket mServerSocket;
		private UUID uuid;

		ServerThread(UUID uuid) {
			this.uuid = uuid;

			// Use a temporary object that is later assigned to mmServerSocket because mmServerSocket is final.
			BluetoothServerSocket tmp = null;

			try {
				// MY_UUID is the app's UUID string, also used by the client code
				tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(BTRecordName, uuid);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket;

			// Keep listening until exception occurs or a socket is returned.
			while (true) {
				try {
					socket = mServerSocket.accept();
				} catch (IOException e) {
					Log.d(TAG, "server break down#########################" + uuid.toString());
					e.printStackTrace();
					break;
				}

				// If a connection was accepted
				if (socket != null && socket.isConnected()) {
					if (mBluetoothAdapter.isDiscovering())
						mBluetoothAdapter.cancelDiscovery();

					// Do work to manage the connection (in a separate thread).
                    //manageConnectedSocket(socket);
					Log.d(TAG, "Connected as a server");

					// Start a new thread to handling data exchange.
					connected(socket, socket.getRemoteDevice(), false);
				}
			}
			try {
				mServerSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Will cancel the listening socket, and cause the thread to finish.
		void cancel() {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace(); 
			}
		}
	}


	/**
	 * Client thread to handle issued connection command
	 * @author fshi
	 *
	 */
	private class ClientThread extends Thread {
		private final BluetoothSocket mClientSocket;
		private final BluetoothDevice mBTDevice;
		private long timeout;
		private boolean clientConnected = false;
		private StringBuffer lock;

		ClientThread(BluetoothDevice device, StringBuffer sb, UUID uuid, long timeout) {
			this.timeout = timeout;
            mBTDevice = device;
            this.lock=sb;

            // Use a temporary object that is later assigned to mmSocket because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice.
			try {
				// MY_UUID is the app's UUID string, also used by the server code.
				tmp = mBTDevice.createInsecureRfcommSocketToServiceRecord(uuid);
			} catch (IOException e) { }
			mClientSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection.
			if (mBluetoothAdapter.isDiscovering())
				mBluetoothAdapter.cancelDiscovery();
			// timestamp before connection
			try {
				// Connect the device through the socket. This will block until it succeeds or throws an exception.
				// Stop the connection after 5 seconds.

				boolean connExisted = false;
				for (ConnectedThread connection : connections) {
					if (connection.getMac().equals(mBTDevice.getAddress()))
						connExisted = true;
				}
				if (!connExisted) {
					synchronized (lock) {
						timeoutHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								if (!clientConnected) {
									cancel();
									if (mMessenger != null) {
										try {
											Message msg=Message.obtain();
											msg.what = BT_CLIENT_CONNECT_FAILED;
											// Send necessary info to the handler.
											Bundle b = new Bundle();
											b.putString(BT_DEVICE_MAC, mBTDevice.getAddress());
											msg.setData(b);
											mMessenger.send(msg);
										} catch (RemoteException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}
						}, timeout);
						mClientSocket.connect();
					}
					clientConnected = true;
					Log.d(TAG, "Connected as a client");

                    // Do work to manage the connection (in a separate thread).

					// Start a new thread to handling data exchange.
					connected(mClientSocket, mClientSocket.getRemoteDevice(), true);
				} else {
					// already connected
//					if(mMessenger != null){
//						try {
//							Message msg=Message.obtain();
//							msg.what = BT_CLIENT_ALREADY_CONNECTED;
//							// send necessary info to the handler
//							Bundle b = new Bundle();
//							b.putString(BT_DEVICE_MAC, mBTDevice.getAddress());
//							msg.setData(b);
//							mMessenger.send(msg);
//						} catch (RemoteException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
				}
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mClientSocket.close();
				} catch (IOException closeException) { 
					// TODO Auto-generated catch block
					closeException.printStackTrace();
				}
			}

			// Do work to manage the connection (in a separate thread).
		}

		// Call this from the main activity to shutdown the connection
		void cancel() {
			try {
				mClientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, boolean iAmClient) {
		ConnectedThread newConn = new ConnectedThread(socket);
		newConn.start();
		connections.add(newConn);

		// Send the info of the connected device back to the UI Activity
		Message msg=Message.obtain();
		if (iAmClient) {
			msg.what = BT_CLIENT_CONNECTED;
		} else {
			msg.what = BT_SERVER_CONNECTED;
		}

		// Send necessary info to the handler
		Bundle b = new Bundle();
		b.putString(BT_DEVICE_MAC, device.getAddress());
		b.putString(BT_DEVICE_NAME, device.getName());
		msg.setData(b);

		try {
			if (mMessenger != null)
				mMessenger.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Connected Thread for handling established connections
	 * @author fshi
	 *
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mConnectedSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private boolean isCancelled = false;
		
		ConnectedThread(BluetoothSocket socket) {
			mConnectedSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = mConnectedSocket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		String getMac(){
			return mConnectedSocket.getRemoteDevice().getAddress();
		}

		public void run() {
			Object buffer;

			BufferedReader in = new BufferedReader(new InputStreamReader(mmInStream)); 
			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					buffer = in.readLine();
					// Send the obtained bytes to the UI activity
					if (mMessenger != null) {
						try {
							// Send the obtained bytes to the UI Activity
							Message msg=Message.obtain();
							Bundle b = new Bundle();
							b.putString(BT_DATA_CONTENT, buffer.toString());
							b.putString(BT_DEVICE_MAC, this.getMac());
							msg.what = BT_DATA;
							msg.setData(b);
							mMessenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();

					// Send message to update UI
					try {
                        Message msg = Message.obtain();
                        Bundle b = new Bundle();
                        if (isCancelled) {
                            msg.what = BT_SUCCESS;
                        } else {
                            msg.what = BT_DISCONNECTED;
                        }
						b.putString(BT_DEVICE_MAC, this.getMac());
						msg.setData(b);
						mMessenger.send(msg);
					} catch (RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					// Remove the connection
					stopConnection(getMac());
					break;

					// Stop the connected thread
				}
			}
		}

		void writeObject(JSONObject json) {
			try {
				DataOutputStream out = new DataOutputStream(mmOutStream);
				out.writeBytes(json.toString() + "\n");
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "output stream error");
				e.printStackTrace();
				stopConnection(getMac());
			}
		}

		void writeString(String string) {
			try {
				DataOutputStream out = new DataOutputStream(mmOutStream);
				out.writeBytes(string);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "output stream error");
				e.printStackTrace();
				stopConnection(getMac());
			}
		}

		// Call this from the main activity to shutdown the connection
		void cancel() {
			isCancelled = true;
			try {
				mmInStream.close();
				mmOutStream.close();
				mConnectedSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
