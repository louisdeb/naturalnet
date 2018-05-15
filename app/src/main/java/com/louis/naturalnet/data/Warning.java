package com.louis.naturalnet.data;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

public class Warning {
    private static String TAG = "Warning";

    public static String WARNING_ID = "warningId";
    public static String WARNING_ISSUER = "issuer";
    public static String WARNING_ISSUE_TIME = "issueTime";
    public static String WARNING_TYPE = "type";
    public static String WARNING_MAGNITUDE = "magnitude";
    public static String WARNING_IMPACT_TIME = "impactTime";
    public static String WARNING_LON_START = "lonStart";
    public static String WARNING_LAT_START = "latStart";
    public static String WARNING_LON_END = "lonEnd";
    public static String WARNING_LAT_END = "latEnd";
    public static String WARNING_RECOMMENDED_ACTIONS = "recommendedActions";
    public static String WARNING_MESSAGE = "warningMessage";

    public int warningId;
    public String issuer;
    public Date issueTime;
    public String type;
    public String magnitude;
    public Date impactTime;
    private long lonStart;
    private long latStart;
    private long lonEnd;
    private long latEnd;
    public String recommendedActions;
    public String message;

    // Create a test warning.
    public Warning() {
        warningId = 1;
        issuer = "Test Issuer";
        issueTime = Calendar.getInstance().getTime();
        type = "Swarm of wasps";
        magnitude = "1000";
        impactTime = new Date(issueTime.getTime() + (5 * 60000));
        lonStart = 0;
        latStart = 0;
        lonEnd = 1;
        latEnd = 1;
        recommendedActions = "Run!";
        message = "Bloody wasps";
    }

    public Warning(JSONObject obj) {
        try {
            warningId = Integer.parseInt(obj.getString(WARNING_ID));
            issuer = obj.getString(WARNING_ISSUER);
            issueTime = new Date(obj.getLong(WARNING_ISSUE_TIME));
            type = obj.getString(WARNING_TYPE);
            magnitude = obj.getString(WARNING_MAGNITUDE);
            impactTime = new Date(obj.getLong(WARNING_IMPACT_TIME));
            lonStart = obj.getLong(WARNING_LON_START);
            latStart = obj.getLong(WARNING_LAT_START);
            lonEnd = obj.getLong(WARNING_LON_END);
            latEnd = obj.getLong(WARNING_LAT_END);
            recommendedActions = obj.getString(WARNING_RECOMMENDED_ACTIONS);
            message = obj.getString(WARNING_MESSAGE);
        } catch (JSONException e) {
            Log.d(TAG, "Couldn't create Warning from passed JSON object: " + obj.toString());
            e.printStackTrace();
        }
    }

    private JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        try {
            obj.put(WARNING_ID, warningId);
            obj.put(WARNING_ISSUER, issuer);
            obj.put(WARNING_ISSUE_TIME, issueTime.getTime());
            obj.put(WARNING_TYPE, type);
            obj.put(WARNING_MAGNITUDE, magnitude);
            obj.put(WARNING_IMPACT_TIME, impactTime.getTime());
            obj.put(WARNING_LON_START, lonStart);
            obj.put(WARNING_LAT_START, latStart);
            obj.put(WARNING_LON_END, lonEnd);
            obj.put(WARNING_LAT_END, latEnd);
            obj.put(WARNING_RECOMMENDED_ACTIONS, recommendedActions);
            obj.put(WARNING_MESSAGE, message);
        } catch (JSONException e) {
            Log.d(TAG, "Failed to create JSON from warning");
            e.printStackTrace();
        }

        return obj;
    }

    public String toString() {
        return this.toJSON().toString();
    }
}
