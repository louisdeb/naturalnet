package com.louis.naturalnet;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.louis.naturalnet.fragments.WarningFragment;

public class MainActivity extends Activity {

    private String logTag = "NaturalNet:";

    @Override
    public void onStart() {
        super.onStart();
        Log.d(logTag,"Started main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the default content view
        setContentView(R.layout.main_layout);

        if(findViewById(R.id.warning_fragment_container) != null) {
            WarningFragment warningFragment = new WarningFragment();
            getFragmentManager().beginTransaction().add(R.id.warning_fragment_container, warningFragment).commit();
        }
    }

}
