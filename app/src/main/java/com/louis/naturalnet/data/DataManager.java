package com.louis.naturalnet.data;


public class DataManager {

//	private final static String dir_name = "test";
//	private final static String log_file_name = "log.txt";
//
//	Context mContext;
//	File logFile;
//
//	private final static String TAG = "Data Manager";
//
//	private static DataManager dataManager = null;
//	
//	private DataManager(Context context){
//		mContext = context;
//
//		Log.d(TAG, String.valueOf(isExternalStorageWritable()));
//		Log.d(TAG, String.valueOf(isExternalStorageReadable()));
//
//		File directory = new File(Environment.getExternalStorageDirectory(), dir_name);
//		if(!directory.exists()){
//			if(!directory.mkdirs()){
//				Log.d(TAG, "not create directory " + directory.getAbsolutePath());
//			}else{
//				logFile = new File(directory.toString(), log_file_name);
//			}
//		}else{
//			logFile = new File(directory.toString(), log_file_name);
//		}
//	}
//
//	public static DataManager getInstance(Context context){
//		if(dataManager == null){
//			dataManager = new DataManager(context);
//		}
//		return dataManager;
//	}
//
//	public void saveLog(String log){
//		try {
//			BufferedWriter out = new BufferedWriter(new FileWriter(logFile.getAbsolutePath(), true));
//			out.write(log);
//			out.newLine();
//			out.flush();
//			out.close();
//		} catch (Exception e) {
//			Log.e(TAG, "Error opening Log.", e);
//		}
//	}
//
//	public boolean isExternalStorageWritable() {
//		String state = Environment.getExternalStorageState();
//		if (Environment.MEDIA_MOUNTED.equals(state)) {
//			return true;
//		}
//		return false;
//	}
//
//	/* Checks if external storage is available to at least read */
//	public boolean isExternalStorageReadable() {
//		String state = Environment.getExternalStorageState();
//		if (Environment.MEDIA_MOUNTED.equals(state) ||
//				Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//			return true;
//		}
//		return false;
//	}

}
