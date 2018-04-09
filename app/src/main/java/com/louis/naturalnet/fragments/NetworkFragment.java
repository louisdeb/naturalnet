package com.louis.naturalnet.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SignalStrength;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.louis.naturalnet.R;
import com.louis.naturalnet.signal.*;

public class NetworkFragment extends Fragment {

    private final static String TAG = "NetworkFragment";

    private boolean expanded = false;

    private SignalQuality signalQuality;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.network_fragment, container, false);

        LinearLayout title = view.findViewById(R.id.network_title_layout);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View titleView) {
                expanded = !expanded;

                // Toggle chevron direction
                titleView.findViewById(R.id.chevron_down_icon).setVisibility(expanded ? View.GONE : View.VISIBLE);
                titleView.findViewById(R.id.chevron_up_icon).setVisibility(expanded ? View.VISIBLE : View.GONE);

                // Toggle content
                view.findViewById(R.id.network_content_layout).setVisibility(expanded ? View.VISIBLE : View.GONE);
            }
        });

        CellularSignalReceiver.addListener(getActivity(), new CellularSignalListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                handleCellularSignalChange(signalStrength, view);
            }
        });

        IntentFilter filter = new IntentFilter("com.louis.naturalnet.signal.LocationReceiver");
        getActivity().registerReceiver(new LocationReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Can do some parsing of the intent here
                handleLocationChange(intent);
            }
        }, filter);

        return view;
    }

    private void handleCellularSignalChange(SignalStrength signalStrength, View view) {
        SignalQuality _signalQuality = SignalUtils.getSignalQuality(signalStrength);

        if (signalQuality == _signalQuality)
            return;

        signalQuality = _signalQuality;

        ImageView cellularIcon = view.findViewById(R.id.cellular_signal_icon);
        TextView cellularText = view.findViewById(R.id.cellular_signal_text);

        int iconRes = R.drawable.ic_network_cell_great;
        int textRes = R.string.cellular_status_great;

        switch (signalQuality) {
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
    }

    private void handleLocationChange(Intent intent) {
        Log.d(TAG, "Received broadcast: " + intent.toString());
    }
}
