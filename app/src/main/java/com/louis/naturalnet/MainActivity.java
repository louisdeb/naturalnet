package com.louis.naturalnet;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

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
    }

}
