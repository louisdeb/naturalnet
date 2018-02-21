package com.louis.naturalnet.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.louis.naturalnet.R;

public class NetworkFragment extends Fragment {

    private boolean expanded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.network_fragment, container, false);

        LinearLayout title = view.findViewById(R.id.network_title_layout);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expanded = !expanded;

                // Toggle chevron direction
                view.findViewById(R.id.chevron_down_icon).setVisibility(expanded ? View.GONE : View.VISIBLE);
                view.findViewById(R.id.chevron_up_icon).setVisibility(expanded ? View.VISIBLE : View.GONE);
            }
        });

        return view;
    }

}
