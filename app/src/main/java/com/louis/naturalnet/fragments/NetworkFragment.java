package com.louis.naturalnet.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SignalStrength;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.louis.naturalnet.R;
import com.louis.naturalnet.bluetooth.BTDeviceListener;
import com.louis.naturalnet.signal.*;

import java.util.ArrayList;

public class NetworkFragment extends Fragment {

    private final static String TAG = "NetworkFragment";

    private boolean expanded = false;

    private SignalQuality cellSignalQuality;
    private SignalQuality gpsSignalQuality;
    private int numPeers;
    private boolean bluetoothSupported;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.network_fragment, container, false);

        LinearLayout title = view.findViewById(R.id.network_title_layout);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View titleView) {
                expanded = !expanded;

                // Toggle chevron direction.
                titleView.findViewById(R.id.chevron_down_icon).setVisibility(expanded ? View.GONE : View.VISIBLE);
                titleView.findViewById(R.id.chevron_up_icon).setVisibility(expanded ? View.VISIBLE : View.GONE);

                // Toggle content.
                view.findViewById(R.id.network_content_layout).setVisibility(expanded ? View.VISIBLE : View.GONE);
            }
        });

        CellularSignalReceiver.addListener(getActivity(), new CellularSignalListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                handleCellularSignalChange(signalStrength, view);
            }
        });

        IntentFilter locationFilter = new IntentFilter("com.louis.naturalnet.signal.LocationReceiver");
        getActivity().registerReceiver(new LocationReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleLocationChange(intent, view);
            }
        }, locationFilter);

        IntentFilter bluetoothFilter = new IntentFilter("com.louis.naturalnet.bluetooth.BTDeviceListener");
        getActivity().registerReceiver(new BTDeviceListener() {
            @Override
            public void onReceive(Context context, Intent intent) {
                bluetoothSupported = intent.getBooleanExtra("btSupported", true);
                if (!bluetoothSupported) {
                    handleBluetoothNotSupported(view);
                    return;
                }

                ArrayList<BluetoothDevice> devices = intent.getParcelableArrayListExtra("devices");
                handlePeersChange(devices, view);
            }
        }, bluetoothFilter);

        return view;
    }

    private void handleCellularSignalChange(SignalStrength signalStrength, View view) {
        SignalQuality _signalQuality = SignalUtils.getSignalQuality(signalStrength);

        if (cellSignalQuality == _signalQuality)
            return;

        cellSignalQuality = _signalQuality;

        ImageView cellularIcon = view.findViewById(R.id.cellular_signal_icon);
        TextView cellularText = view.findViewById(R.id.cellular_signal_text);

        int iconRes = R.drawable.ic_network_cell_great;
        int textRes = R.string.cellular_status_great;

        switch (cellSignalQuality) {
            case NONE_OR_NOT_KNOWN:
                iconRes = R.drawable.ic_network_cell_none;
                textRes = R.string.cellular_status_none;
                break;
            case POOR:
                iconRes = R.drawable.ic_network_cell_moderate;
                textRes = R.string.cellular_status_poor;
                break;
            case MODERATE:
                iconRes = R.drawable.ic_network_cell_moderate;
                textRes = R.string.cellular_status_moderate;
                break;
            case GOOD:
                iconRes = R.drawable.ic_network_cell_great;
                textRes = R.string.cellular_status_good;
                break;
            case GREAT:
                iconRes = R.drawable.ic_network_cell_great;
                textRes = R.string.cellular_status_great;
                break;
        }

        cellularIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), iconRes));
        cellularText.setText(textRes);

        handleNetworkChange();
    }

    private void handleLocationChange(Intent intent, View view) {
        SignalQuality gpsQuality = SignalUtils.getGpsQuality(intent);

        if (gpsQuality == gpsSignalQuality)
            return;

        gpsSignalQuality = gpsQuality;

        ImageView gpsIcon = view.findViewById(R.id.gps_signal_icon);
        TextView gpsText = view.findViewById(R.id.gps_signal_text);

        int iconRes = R.drawable.ic_gps_fixed;
        int textRes = R.string.gps_status_great;

        switch (gpsQuality) {
            case NONE_OR_NOT_KNOWN:
                iconRes = R.drawable.ic_gps_none;
                textRes = R.string.gps_status_none;
                break;
            case POOR:
                iconRes = R.drawable.ic_gps_not_fixed;
                textRes = R.string.gps_status_poor;
                break;
            case MODERATE:
                iconRes = R.drawable.ic_gps_not_fixed;
                textRes = R.string.gps_status_moderate;
                break;
            case GOOD:
                iconRes = R.drawable.ic_gps_fixed;
                textRes = R.string.gps_status_good;
                break;
            case GREAT:
                iconRes = R.drawable.ic_gps_fixed;
                textRes = R.string.gps_status_great;
                break;
        }

        gpsIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), iconRes));
        gpsText.setText(textRes);

        handleNetworkChange();
    }

    private void handleBluetoothNotSupported(View view) {
        ImageView peersIcon = view.findViewById(R.id.net_signal_icon);
        TextView netSignalText = view.findViewById(R.id.net_signal_text);
        LinearLayout peersConnectionsLayout = view.findViewById(R.id.net_number_of_connections_wrapper);

        peersIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_net_signal_none));
        netSignalText.setText(R.string.bt_not_supported);

        peersConnectionsLayout.setVisibility(View.GONE);

        handleNetworkChange();
    }

    private void handlePeersChange(ArrayList<BluetoothDevice> peers, View view) {
        int _numPeers = peers.size();

        if (_numPeers == numPeers)
            return;

        numPeers = _numPeers;

        ImageView peersIcon = view.findViewById(R.id.net_signal_icon);
        TextView netSignalText = view.findViewById(R.id.net_signal_text);
        TextView peersConnectionsText = view.findViewById(R.id.net_number_of_connections);
        LinearLayout peersConnectionsLayout = view.findViewById(R.id.net_number_of_connections_wrapper);

        int iconRes = R.drawable.ic_net_signal_great;
        int netText = R.string.net_status_great;

        // May want a more complex metric than simply how many peers are available?
        if (numPeers == 0) {
            iconRes = R.drawable.ic_net_signal_none;
            netText = R.string.net_status_none;
        } else if (numPeers <= 2) {
            iconRes = R.drawable.ic_net_signal_great;
            netText = R.string.net_status_poor;
        } else if (numPeers <= 5) {
            iconRes = R.drawable.ic_net_signal_great;
            netText = R.string.net_status_good;
        }

        peersConnectionsLayout.setVisibility(View.VISIBLE);

        peersIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), iconRes));
        netSignalText.setText(netText);
        peersConnectionsText.setText(Integer.toString(numPeers));

        handleNetworkChange();
    }

    private void handleNetworkChange() {
        // Compute a overall quality of network and alter the main colour of the layout.
    }
}
