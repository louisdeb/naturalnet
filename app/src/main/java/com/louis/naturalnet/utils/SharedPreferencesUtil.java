package com.louis.naturalnet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class SharedPreferencesUtil {
	
	static public int loadSavedPreferences(Context context, String key, int defaultInt) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int result = sharedPreferences.getInt(key, defaultInt);
		return result;
	}
	
	static public boolean loadSavedPreferences(Context context, String key, boolean defaultBool) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean result = sharedPreferences.getBoolean(key, defaultBool);
		return result;
	}
	
	static public void savePreferences(Context context, String key, int value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}
	
	static public void savePreferences(Context context, String key, boolean value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
}