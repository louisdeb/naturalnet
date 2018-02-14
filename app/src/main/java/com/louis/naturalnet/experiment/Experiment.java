package com.louis.naturalnet.experiment;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

public class Experiment {

	public String id;
	public String name;
	public String description;
	public String startTime;
	public String endTime;
	public int joined = 0;
	public ArrayList<LatLng> area;
	
	public Experiment(){
		area = new ArrayList<LatLng>();
	}
	
	/**
	 * deep copy
	 * @param exp
	 */
	public Experiment(Experiment exp){
		id = exp.id;
		name = exp.name;
		description = exp.description;
		startTime = exp.startTime;
		endTime = exp.endTime;
		joined = exp.joined;
		area = new ArrayList<LatLng>();
		for(LatLng latLng : exp.area){
			area.add(new LatLng(latLng.latitude, latLng.longitude));
		}
	}
	
}
