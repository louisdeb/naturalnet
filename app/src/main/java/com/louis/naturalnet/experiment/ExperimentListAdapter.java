package com.louis.naturalnet.experiment;

import com.louis.naturalnet.R;
import com.louis.naturalnet.R.color;
import com.louis.naturalnet.R.string;

import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ExperimentListAdapter extends ArrayAdapter<Experiment> {

	Context context;
	int layoutResourceId;

	public ExperimentListAdapter(Context context, int resourceid) {
		super(context, resourceid, ExperimentList.experimentList);
		this.layoutResourceId = resourceid;
		this.context = context;
	}

	/**
	 * comparator to sort list
	 * @author fshi
	 */
	public class ExperimentComparator implements Comparator<Experiment>
	{
		@Override
		public int compare(Experiment lhs, Experiment rhs) {
			// TODO Auto-generated method stub
			return rhs.joined - lhs.joined;
		}
	}


	public void sortList(){
		Collections.sort(ExperimentList.experimentList, new ExperimentComparator());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ExperimentHolder holder = null;

		if(row == null)
		{
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new ExperimentHolder();

//			holder.name = (TextView)row.findViewById(R.id.experiment_name);
//			holder.endTime = (TextView)row.findViewById(R.id.experiment_deadline);
//			holder.desc = (TextView)row.findViewById(R.id.experiment_desc);
			row.setTag(holder);
		}
		else
		{
			holder = (ExperimentHolder)row.getTag();
		}

		Experiment experiment = ExperimentList.experimentList.get(position);
		if(experiment != null){
			holder.name.setText(experiment.name);
			holder.endTime.setText(experiment.endTime);
			holder.desc.setText(experiment.description);
		}

		return row;
	}

	static class ExperimentHolder
	{
		TextView name;
		TextView desc;
		TextView endTime;
	}


}
