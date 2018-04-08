package com.louis.naturalnet.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.louis.naturalnet.R;
import com.louis.naturalnet.signal.CellularSignalListener;
import com.louis.naturalnet.signal.CellularSignalReceiver;

public class NetworkFragment extends Fragment {

    private static String TAG = "NetworkFragment";

    private boolean expanded = false;

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
                super.onSignalStrengthsChanged(signalStrength);
                int gsmSignal = signalStrength.getGsmSignalStrength();

                // Utilise this GSM Signal to alter the UI
                Log.d(TAG, "GSM: " + gsmSignal);
            }
        });

        return view;
    }
}
