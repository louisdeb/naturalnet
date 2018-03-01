package com.louis.naturalnet.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.louis.naturalnet.R;

public class WarningFragment extends Fragment {

    private boolean expanded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.warning_fragment, container, false);

        LinearLayout title = view.findViewById(R.id.warning_title_layout);
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

            }
        });

        return view;
    }

}
