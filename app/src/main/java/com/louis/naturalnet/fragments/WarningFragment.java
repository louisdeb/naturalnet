package com.louis.naturalnet.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class WarningFragment extends Fragment {

    private boolean expanded = false;
    private Warning warning = null;
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.warning_fragment, container, false);

        // Not the nicest way of getting the context. If we upgrade from API 21 to 23 we can use Fragment.getContext().
        context = view.getContext();

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

    private void handleTitleClick(View view, View titleView, LinearLayout title) {
        boolean hasWarning = warning != null;

        ImageView titleIconView = titleView.findViewById(R.id.warning_icon);
        TextView titleTextView = titleView.findViewById(R.id.warning_title);

        int titleIconRes = hasWarning ? R.drawable.ic_warning : R.drawable.ic_tick;
        int titleTextRes = hasWarning ? R.string.warning_received : R.string.no_warnings;
        int backgroundRes = hasWarning ? R.drawable.yellow_rounded_fragment : R.drawable.green_rounded_fragment;

        titleIconView.setImageDrawable(ContextCompat.getDrawable(getActivity(), titleIconRes));
        titleTextView.setText(titleTextRes);
        view.findViewById(R.id.warning_fragment_container).setBackgroundResource(backgroundRes);

        if (!hasWarning) {
            // For our test implementation, issue a warning.
            warning = QueueManager.getInstance().generateWarning();

            // As part of a test, alert the BTDeviceManager of this new warning.
            Intent warningIntent = new Intent("com.louis.naturalnet.bluetooth.TempWarningReceiver");
            context.sendBroadcast(warningIntent);
            title.callOnClick();

            // If there's no warning, perform no action.
            return;
        }

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
