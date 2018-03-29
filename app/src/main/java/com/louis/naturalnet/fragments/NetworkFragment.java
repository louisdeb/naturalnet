package com.louis.naturalnet.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.louis.naturalnet.R;

public class NetworkFragment extends Fragment {

    TelephonyManager telephonyManager;
    MyPhoneStateListener mPhoneStateListener;
    int mSignalStrength = 0;

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

        mPhoneStateListener = new MyPhoneStateListener();
        telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        return view;
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            // Returns a signal int [0..31]
            mSignalStrength = signalStrength.getGsmSignalStrength();

            // To dBm
            // mSignalStrength = (2 * mSignalStrength) - 113;

            // Log.d("NetworkFragment", "signal strength: " + mSignalStrength);
            // Log.d("NetworkFragment", "level: " + signalStrength.getLevel());
        }

    }

}
