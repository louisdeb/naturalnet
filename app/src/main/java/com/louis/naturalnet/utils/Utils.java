package com.louis.naturalnet.utils;

public class Utils {

	public static boolean isInteger(String s) {
	    return isInteger(s,10);
	}

	// These all work by checking information in the device name.
    // We might not actually want to store this information in the device name but instead
    // send it in a packet.

	public static boolean isOppNetRelay(String s){
		return (s.length() > 5) &&
			   (s.substring(0, 6).equals("OppNet") && getDeviceType(s).compareToIgnoreCase("R") == 0);
	}
	
	public static int getQueueLen(String s){
		return Integer.parseInt(s.split(":")[3]);
	}
	
	public static int getBatteryLevel(String s){
		return Integer.parseInt(s.split(":")[4]);
	}
	
	public static String getDeviceID(String s){
		return s.split(":")[2];
	}
	
	private static String getDeviceType(String s){
		return s.split(":")[1];
	}
	
	private static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}

}
