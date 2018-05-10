package com.louis.naturalnet.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.louis.naturalnet.R;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.data.WarningReceiver;
import org.json.JSONException;
import org.json.JSONObject;

public class WarningFragment extends Fragment {

    private boolean expanded = false;
    private Warning warning = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.warning_fragment, container, false);

        // Not the nicest way of getting the context. If we upgrade from API 21 to 23 we can use Fragment.getContext().
        final Context context = view.getContext();

        final LinearLayout title = view.findViewById(R.id.warning_title_layout);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View titleView) {
                expanded = !expanded;

                titleView.findViewById(R.id.warning_tick).setVisibility(expanded ? View.GONE : View.VISIBLE);
                titleView.findViewById(R.id.warning_icon).setVisibility(expanded ? View.VISIBLE : View.GONE);

                titleView.findViewById(R.id.no_warning_title).setVisibility(expanded ? View.GONE : View.VISIBLE);
                titleView.findViewById(R.id.warning_title).setVisibility(expanded ? View.VISIBLE : View.GONE);

                view.findViewById(R.id.warning_fragment_container).setBackgroundResource(
                        expanded ? R.drawable.yellow_rounded_fragment : R.drawable.green_rounded_fragment);

                // As a test, create a warning by clicking on the warning fragment.
                if (expanded && warning == null) {
                    QueueManager.getInstance().generateWarning();

                    // As part of a test, alert the BTDeviceManager of this new warning.
                    Intent warningIntent = new Intent("com.louis.naturalnet.bluetooth.TempWarningReceiver");
                    context.sendBroadcast(warningIntent);
                }
            }
        });

        IntentFilter warningFilter = new IntentFilter("com.louis.naturalnet.data.WarningReceiver");
        context.registerReceiver(new WarningReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    JSONObject warningJSON = new JSONObject(intent.getStringExtra("warning"));
                    warning = new Warning(warningJSON);
                    title.callOnClick();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, warningFilter);

        return view;
    }

}
