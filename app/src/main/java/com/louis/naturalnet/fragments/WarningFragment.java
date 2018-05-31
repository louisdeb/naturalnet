package com.louis.naturalnet.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.louis.naturalnet.R;
import com.louis.naturalnet.data.QueueManager;
import com.louis.naturalnet.data.Warning;
import com.louis.naturalnet.data.WarningReceiver;
import com.louis.naturalnet.device.NaturalNetDevice;
import com.louis.naturalnet.signal.LocationReceiver;
import com.louis.naturalnet.signal.SignalUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class WarningFragment extends Fragment {

    private boolean expanded = false;
    private Warning warning = null;
    private Warning backgroundWarning = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.warning_fragment, container, false);

        // Not the nicest way of getting the context. If we upgrade from API 21 to 23 we can use Fragment.getContext().
        Context context = view.getContext();

        final LinearLayout title = view.findViewById(R.id.warning_title_layout);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View titleView) {
                handleTitleClick(view, titleView, title);
            }
        });

        IntentFilter warningFilter = new IntentFilter("com.louis.naturalnet.data.WarningReceiver");
        context.registerReceiver(new WarningReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    handleWarningReceived(intent, title);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, warningFilter);

        IntentFilter locationFilter = new IntentFilter("com.louis.naturalnet.signal.LocationReceiver");
        getActivity().registerReceiver(new LocationReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    handleLocationReceived(intent, title);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, locationFilter);

        return view;
    }

    private void handleWarningReceived(Intent intent, LinearLayout title) throws JSONException {
        JSONObject warningJSON = new JSONObject(intent.getStringExtra("warning"));
        if (intent.getBooleanExtra("inZone", false)) {
            backgroundWarning = null;
            warning = new Warning(warningJSON);
            title.callOnClick();
        } else {
            backgroundWarning = new Warning(warningJSON);
            warning = null;
            title.callOnClick();
        }
    }

    private void handleLocationReceived(Intent intent, LinearLayout title) throws JSONException {
        Location location = SignalUtils.getLocation(intent);
        if (backgroundWarning != null) {
            if (NaturalNetDevice.locationInZone(location, backgroundWarning.getZone())) {
                warning = backgroundWarning;
                backgroundWarning = null;
                title.callOnClick();
            }
        } else if (warning != null) {
            if (NaturalNetDevice.locationInZone(location, warning.getZone())) {
                backgroundWarning = warning;
                warning = null;
                title.callOnClick();
            }
        }
    }

    private void handleTitleClick(View view, View titleView, LinearLayout title) {
        boolean hasWarning = warning != null;
        boolean hasBackgroundWarning = backgroundWarning != null;

        ImageView titleIconView = titleView.findViewById(R.id.warning_icon);
        TextView titleTextView = titleView.findViewById(R.id.warning_title);

        int titleIconRes = hasWarning ? R.drawable.ic_warning : R.drawable.ic_tick;
        int titleTextRes = hasWarning ? R.string.warning_received : R.string.no_warnings;
        int backgroundRes = hasWarning ? R.drawable.yellow_rounded_fragment : R.drawable.green_rounded_fragment;

        titleIconView.setImageDrawable(ContextCompat.getDrawable(getActivity(), titleIconRes));
        titleTextView.setText(titleTextRes);
        view.findViewById(R.id.warning_fragment_container).setBackgroundResource(backgroundRes);

        if (!hasWarning && !hasBackgroundWarning) {
            // For our test implementation, issue a warning.
            warning = QueueManager.getInstance().generateWarning();
            title.callOnClick();

            // If there's no warning, perform no action.
            return;
        }

        // If there is a background warning, display 'Warning Nearby' and collapse the content view.
        if (hasBackgroundWarning && !hasWarning) {
            titleTextView.setText(R.string.warning_nearby);
            titleIconView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_warning));
            view.findViewById(R.id.warning_fragment_container).setBackgroundResource(R.drawable.yellow_rounded_fragment);
            view.findViewById(R.id.warning_content_layout).setVisibility(View.GONE);
        }

        if (!hasWarning)
            return;

        expanded = !expanded;

        // Populate the warning information.
        if (expanded) {
            TextView issuedWarningTitle = view.findViewById(R.id.issued_warning_title);
            issuedWarningTitle.setText(warning.type);

            TextView issuedBy = view.findViewById(R.id.issued_by);
            issuedBy.setText(warning.issuer);

            TextView issuedAt = view.findViewById(R.id.issued_at);
            issuedAt.setText(dateWithoutZone(warning.issueTime));

            TextView magnitude = view.findViewById(R.id.warning_magnitude);
            magnitude.setText(warning.magnitude);

            TextView impactTime = view.findViewById(R.id.warning_impact_time);
            impactTime.setText(dateWithoutZone(warning.impactTime));

            TextView warningMessage = view.findViewById(R.id.issued_warning_message);
            warningMessage.setText(warning.message);

            TextView recommendedActions = view.findViewById(R.id.issued_warning_actions);
            recommendedActions.setText(warning.recommendedActions);
        }

        view.findViewById(R.id.warning_content_layout).setVisibility(expanded ? View.VISIBLE : View.GONE);
    }

    private String dateWithoutZone(Date date) {
        String s = date.toString();
        int endOfTime = s.indexOf(':') + 6;
        return s.substring(0, endOfTime);
    }

}
