package com.louis.naturalnet.bluetooth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import com.louis.naturalnet.utils.Constants;
import com.louis.naturalnet.device.DeviceInformation;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * a multi thread BT utility
 * @author fshi
 *
 */
/*
    This class contains code for both the Server and Client BT models, as well as
    code to handle a connection (the ConnectionThread) which transfers data over an established connection.
 */
class BTCom {

	private static final String TAG = "BTCom";

    private static BTCom _this = null;
    private Context context;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler timeoutHandler = new Handler();

    private final static StringBuffer clientLock = new StringBuffer();

	// Current connection state, only one server thread and one client thread.
	private ServerThread mServerThread;

	// List of connections (ConnectedThreads) so that we can maintain each or send over a specific already established
    // connection.
    private ArrayList<ConnectedThread> connections = new ArrayList<>();

	private BTCom(Context context) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.context = context;
	}

	static BTCom getInstance(Context context) {
		if(_this == null)
			_this = new BTCom(context);

		return _this;
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

        ClientThread clientThread = new ClientThread(btDevice, clientLock, timeout);
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
			if (connection.getMac().equals(MAC)) {
                connection.write(data);
            }
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
	// The ServerThread (or just Server) maintains a serverSocket & listens to any connections on that serverSocket.
	private class ServerThread extends Thread {
		private final BluetoothServerSocket serverSocket;
		private BluetoothSocket socket;
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

			serverSocket = tmp;
		}

		public void run() {
			// Keep listening until exception occurs or a socket is returned.
			while (true) {
				try {
					socket = this.serverSocket.accept();
				} catch (IOException e) {
					Log.d(TAG, "Server break down. UUID: " + uuid.toString());
					e.printStackTrace();
					break;
				}

				// If a connection was accepted
				if (socket != null && socket.isConnected()) {
					if (mBluetoothAdapter.isDiscovering())
						mBluetoothAdapter.cancelDiscovery();

					Log.d(TAG, "Connected as a server to device: " + socket.getRemoteDevice().getName());

					// We want to exchange data with the connected NaturalNet device.
					sendHandshake(socket, socket.getRemoteDevice(), false);
				}
			}

			// If our server broke down and we broke from the while loop.
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Will cancel the listening serverSocket, and cause the thread to finish.
		void cancel() {
			try {
				serverSocket.close();
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
		private final BluetoothDevice device;
		private long timeout;
		private boolean clientConnected = false;
		private final StringBuffer lock;

		ClientThread(BluetoothDevice device, StringBuffer sb, long timeout) {
            this.device = device;
			this.timeout = timeout;
            this.lock = sb;

            // Use a temporary object that is later assigned to mmSocket because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice.
			try {
				// MY_UUID is the app's UUID string, also used by the server code.
				tmp = this.device.createInsecureRfcommSocketToServiceRecord(Constants.APP_UUID);
			} catch (IOException e) {
			    e.printStackTrace();
            }

			mClientSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection.
			if (mBluetoothAdapter.isDiscovering())
				mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the serverSocket. This will block until it succeeds or throws an exception.
				// Stop the connection after _timeout_ seconds.
                // (Timeout is Constants.BT_CLIENT_TIMEOUT which is 5 seconds).

				boolean connExisted = false;
				for (ConnectedThread connection : connections) {
					if (connection.getMac().equals(device.getAddress()))
						connExisted = true;
				}

				if (!connExisted) {
					synchronized (lock) {
					    // This timeout is triggered if we receive no response from the server.
						timeoutHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								if (!clientConnected) {
									cancel();

                                    Log.d(TAG, "Timeout for connection: " + device.getName() + " - " + device.getAddress());

                                    // Tell the BTDeviceManager that we failed to connect to the device.
                                    Intent connectionIntent = new Intent("com.louis.naturalnet.bluetooth.HandshakeReceiver");
                                    connectionIntent.putExtra("connected", false);
                                    connectionIntent.putExtra("device", device);
                                    context.sendBroadcast(connectionIntent);
								}
							}

						}, timeout);

						mClientSocket.connect();
					}

					clientConnected = true;

					Log.d(TAG, "Connected as a client to device: " + device.getName());

					// Perform a handshake with the device.
                    sendHandshake(mClientSocket, mClientSocket.getRemoteDevice(), true);
				}
			} catch (IOException connectException) {
				// Unable to connect; close the serverSocket and get out.
				try {
					mClientSocket.close();
				} catch (IOException closeException) { 
					closeException.printStackTrace();
				}
			}
		}

		// Call this from the main activity to shutdown the connection.
		void cancel() {
			try {
				mClientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

    private synchronized void sendHandshake(BluetoothSocket socket, BluetoothDevice device, boolean asClient) {
        // Want to send a bit of data announcing we are a NaturalNet device.
        OutputStream outputStream;

        // Get the input and output streams.
        try {
            Log.d(TAG, "Sending handshake: " + DeviceInformation.getHandshake().toString());

            outputStream = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            out.writeBytes(DeviceInformation.getHandshake().toString() + "\n");
            out.flush();

            if (!asClient) {
                try {
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    Object buffer = in.readLine();

                    JSONObject metadata = new JSONObject(buffer.toString());
                    if (!(boolean) metadata.get("handshake")) {
                        // We didn't get a handshake and might want to cancel communication with this device.
                        // Should test to see if we get this case. Will probably occur if a device offers insecure
                        // rf comm connection but isn't a NaturalNet device.
                        Log.d(TAG, "Received non-handshake message from client: " + metadata.toString());
                    }

                    Log.d(TAG, "Received handshake message from client: " + metadata.toString());

                    Intent connectionIntent = new Intent("com.louis.naturalnet.bluetooth.HandshakeReceiver");
                    connectionIntent.putExtra("connected", true);
                    connectionIntent.putExtra("device", device);
                    connectionIntent.putExtra("metadata", metadata.toString());
                    context.sendBroadcast(connectionIntent);
                } catch (Exception e) {
                    // This might just be a delay in the client sending the handshake. It might also be a breaking
                    // case for our connection with the device and would require a failure broadcast.
                    Log.d(TAG, "Failed to read handshake input stream");
                    e.printStackTrace();
                }
            }

            connected(socket, device);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
	    Log.d(TAG, "Called 'connected' with device: " + device.getName());

	    // Start the connection thread.
		ConnectedThread newConn = new ConnectedThread(socket);
		newConn.start();

        // Add this connection to our list of connections.
        connections.add(newConn);
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

		ConnectedThread(BluetoothSocket socket) {
			mConnectedSocket = socket;

			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams.
			try {
				tmpIn = mConnectedSocket.getInputStream();
				tmpOut = mConnectedSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		String getMac() {
		    return getDevice().getAddress();
        }

		BluetoothDevice getDevice(){
			return mConnectedSocket.getRemoteDevice();
		}

		public void run() {
			Object buffer;
			BufferedReader in = new BufferedReader(new InputStreamReader(mmInStream));

			// Keep listening to the InputStream until an exception occurs.
			while (true) {
				try {
                    // Read from the InputStream.
                    buffer = in.readLine();

                    Log.d("ConnectedThread", "Got message: " + buffer.toString());

                    // We might want to put the handshake handling into handleMessage or the BTMessageHandler.
                    try {
                        JSONObject metadata = new JSONObject(buffer.toString());
                        if ((boolean) metadata.get("handshake")) {
                            Log.d(TAG, "Received handshake message from client: " + metadata.toString());
                            // Announce that the client has received a handshake from the server.
                            Intent handshakeIntent = new Intent("com.louis.naturalnet.bluetooth.HandshakeReceiver");
                            handshakeIntent.putExtra("connected", true);
                            handshakeIntent.putExtra("device", getDevice());
                            handshakeIntent.putExtra("metadata", metadata.toString());
                            context.sendBroadcast(handshakeIntent);
                        } else {
                            throw new JSONException("Received a message without a handshake");
                        }
                    } catch (JSONException e) {
                        // This is fine. It just means that we received a message that wasn't a handshake.
                        // metadata.get("handshake") will throw a JSONException. The if statement might not even be
                        // necessary.
                        handleMessage(buffer);
                    }
				} catch (IOException e) {
				    // We no longer announce the socket failure to the BTMessageHandler. We may want to broadcast
                    // something to the BTDeviceManager to say that our connection has dropped (it could then store
                    // the device as a known NaturalNet device but with the knowledge that we are not connected).

				    Log.d(TAG, "Got exception while trying to read from the input stream.");
					e.printStackTrace();

					// Remove the connection if we got some exception.
					stopConnection(getMac());

                    // Stop the connected thread.
					break;
				}
			}
		}

		// Handle a non-handshake, received message.
		private void handleMessage(Object buffer) {
            // Send the obtained bytes to the UI Activity.
            Message msg=Message.obtain();
            msg.what = Constants.BT_DATA_RECEIVED;

            Bundle b = new Bundle();
            b.putString(Constants.BT_DATA_CONTENT, buffer.toString());
            b.putString(Constants.BT_DEVICE_MAC, getMac());
            msg.setData(b);

            Intent messageFilter = new Intent("com.louis.naturalnet.bluetooth.MessageReceiver");
            messageFilter.putExtra("message", msg);
            context.sendBroadcast(messageFilter);
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

		// Shutdown the connection.
		void cancel() {
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
