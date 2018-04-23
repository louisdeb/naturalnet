package com.louis.naturalnet.bluetooth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import com.louis.naturalnet.utils.Constants;
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
/*
    This class contains code for both the Server and Client BT models, as well as
    code to handle a connection (the ConnectionThread) which transfers data over an established connection.

    Messenger mMessenger is used to handle data. The Messenger has functionality from BTServiceHandler, which parses
    received data.
 */
class BTCom {

	private static final String TAG = "BTCom";

    private static BTCom _this = null;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler timeoutHandler = new Handler();
    private final static StringBuffer sbLock = new StringBuffer();

	// All received messages are sent through this messenger to its parent.
	private Messenger mMessenger = null;

	// Current connection state, only one server thread and one client thread.
	private ServerThread mServerThread;

	// List of connections (ConnectedThreads) so that we can maintain each or send over a specific already established
    // connection.
    private ArrayList<ConnectedThread> connections = new ArrayList<>();

	private BTCom() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	static BTCom getInstance() {
		if(_this == null)
			_this = new BTCom();

		return _this;
	}

	// Sets the communication callback Messenger if not already set.
    // We might want to override any existing mMessenger to avoid unexpected loss of callbacks?
	void setCallback(Messenger callback) {
		if (mMessenger == null)
			mMessenger = callback;
	}

    // Start a BT scan for devices which lasts for _duration_.
    void startScan(long duration) {
        // We don't want to start a scan if we have any active connections, as this will slow down our connections.
        if (getActiveConnectionsCount() == 0) {

            // If scan is already started, do nothing.
            if (mBluetoothAdapter.isDiscovering())
                return;

            // Start discovery.
            mBluetoothAdapter.startDiscovery();

            // Cancel the discovery process after duration.
            final Handler discoveryHandler = new Handler();
            discoveryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothAdapter.isDiscovering())
                        mBluetoothAdapter.cancelDiscovery();
                }
            }, duration);

        }
    }

    // Start the server, allowing for other devices to contact us.
    synchronized void startServer() {
        stopServer();

        mServerThread = new ServerThread(Constants.APP_UUID);
        mServerThread.start();
    }

    // Stop the server.
    void stopServer() {
        if (mServerThread != null)
            mServerThread.cancel();
    }

    // Connect to a BT device.
    synchronized void connect(BluetoothDevice btDevice, long timeout) {
        // Start the thread to connect with the given device.
        // The ClientThread creates an insecure RF Comm connection to the device.

        ClientThread clientThread = new ClientThread(btDevice, sbLock, Constants.APP_UUID, timeout);
        clientThread.start();
    }

	// Stop a connection with device with specific MAC address.
	synchronized void stopConnection(String MAC) {
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC)) {
				connection.cancel();
				connections.remove(connection);
				break;
			}
		}
	}

    // Send JSON data to a destination device with the MAC address.
	synchronized void send(String MAC, JSONObject data) {
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC))
				connection.write(data);
		}
	}

	// Send String data to a destination device with the MAC address.
	synchronized void send(String MAC, String data) {
		for (ConnectedThread connection : connections) {
			if (connection.getMac().equals(MAC))
				connection.write(data);
		}
	}

    // Get the number of active connections.
	private int getActiveConnectionsCount() {
		return connections.size();
	}


	/**
	 * BT Server thread
	 * @author fshi
	 *
	 */
	// The ServerThread (or just Server) maintains a socket & listens to any connections on that socket.
	private class ServerThread extends Thread {
		private final BluetoothServerSocket mServerSocket;
		private UUID uuid;

		ServerThread(UUID uuid) {
			this.uuid = uuid;

			// Use a temporary object that is later assigned to mmServerSocket because mmServerSocket is final.
			BluetoothServerSocket tmp = null;

			try {
				tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Constants.BTSocketServiceName, uuid);
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
					Log.d(TAG, "Server break down. UUID: " + uuid.toString());
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

					// Maybe perform a handshake here, and on return from the handshake, call connected

					// Start a new thread to handling data exchange.
					connected(socket, socket.getRemoteDevice(), false);
				}
			}

			try {
				mServerSocket.close();
			} catch (IOException e) {
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
	    // The thread is responsible for connecting to a single device.

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
			} catch (IOException e) {
			    e.printStackTrace();
            }

			mClientSocket = tmp;
		}

		// Note: This is called when we connectToBTServer. This is our connection transferring data. The connection
        // is created in the ClientThread constructor.
        // If we are trying to send a message when we discover each device (to see if it is a NaturalNet relay),
        // then we will stop discovering at every device found.
        // So we would want to wait till the scan is completed, and then attempt a communication with each of those
        // devices.

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
					    // Can we can use this timeout to decide the device wasn't a NaturalNet device?
						timeoutHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								if (!clientConnected) {
									cancel();
									if (mMessenger != null)
									    announceFailure(Constants.BT_CLIENT_CONNECT_FAILED);
								}
							}

						}, timeout);

						mClientSocket.connect();
					}

					clientConnected = true;

					Log.d(TAG, "Connected as a client");

                    // It may be that the response from the server will be enough to know it's a NaturalNet relay.
                    // Probably not though. It will just mean it's a BT device allowing insecure rf comm.

					// Start a new thread to handling data exchange.
					connected(mClientSocket, mClientSocket.getRemoteDevice(), true);
				} else {
					// Already connected.
					if (mMessenger != null)
					    announceFailure(Constants.BT_CLIENT_ALREADY_CONNECTED);
				}
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out.
				try {
					mClientSocket.close();
				} catch (IOException closeException) { 
					closeException.printStackTrace();
				}
			}

			// Do work to manage the connection (in a separate thread).
		}

		// Call this from the main activity to shutdown the connection.
		void cancel() {
			try {
				mClientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void announceFailure(int what) {
            try {
                Message msg=Message.obtain();
                msg.what = what;

                Bundle b = new Bundle();
                b.putString(Constants.BT_DEVICE_MAC, mBTDevice.getAddress());

                msg.setData(b);
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, boolean asClient) {
	    // Start the connection thread.
		ConnectedThread newConn = new ConnectedThread(socket);
		newConn.start();

		// Add this connection to our list of connections.
		connections.add(newConn);

		// Send the info of the connected device back to the UI Activity.
		Message msg = Message.obtain();
		msg.what = asClient ? Constants.BT_CLIENT_CONNECTED : Constants.BT_SERVER_CONNECTED;

		// Send necessary info to the handler.
		Bundle b = new Bundle();
		b.putString(Constants.BT_DEVICE_MAC, device.getAddress());
		b.putString(Constants.BT_DEVICE_NAME, device.getName());
		msg.setData(b);

		try {
			if (mMessenger != null)
				mMessenger.send(msg);
		} catch (RemoteException e) {
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

			// Get the input and output streams.
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

			// Keep listening to the InputStream until an exception occurs.
			while (true) {
				try {
					// Read from the InputStream.
					buffer = in.readLine();

					// Send the obtained bytes to the UI activity.
					if (mMessenger != null) {
						try {
							// Send the obtained bytes to the UI Activity.
							Message msg=Message.obtain();
                            msg.what = Constants.BT_DATA;

							Bundle b = new Bundle();
							b.putString(Constants.BT_DATA_CONTENT, buffer.toString());
							b.putString(Constants.BT_DEVICE_MAC, this.getMac());
							msg.setData(b);

							mMessenger.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();

					// Send message to update UI.
					try {
                        Message msg = Message.obtain();
                        msg.what = isCancelled ? Constants.BT_SUCCESS : Constants.BT_DISCONNECTED;

                        Bundle b = new Bundle();
						b.putString(Constants.BT_DEVICE_MAC, this.getMac());
						msg.setData(b);

						mMessenger.send(msg);
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}

					// Remove the connection if we got some exception.
					stopConnection(getMac());

                    // Stop the connected thread.
					break;
				}
			}
		}

		// Write a JSON object to the output stream.
		void write(JSONObject json) {
			try {
				DataOutputStream out = new DataOutputStream(mmOutStream);
				out.writeBytes(json.toString() + "\n");
				out.flush();
			} catch (IOException e) {
				Log.e(TAG, "JSON Object output stream error");
				e.printStackTrace();
				stopConnection(getMac());
			}
		}

		// Write a String to the output stream.
		void write(String string) {
			try {
				DataOutputStream out = new DataOutputStream(mmOutStream);
				out.writeBytes(string);
				out.flush();
			} catch (IOException e) {
				Log.e(TAG, "String output stream error");
				e.printStackTrace();
				stopConnection(getMac());
			}
		}

		// Call this from the main activity to shutdown the connection.
		void cancel() {
			isCancelled = true;

			try {
				mmInStream.close();
				mmOutStream.close();
				mConnectedSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
