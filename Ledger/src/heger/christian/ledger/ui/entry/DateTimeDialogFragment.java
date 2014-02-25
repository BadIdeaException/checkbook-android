package heger.christian.ledger.ui.entry;

import heger.christian.ledger.R;

import java.text.DateFormat;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class DateTimeDialogFragment extends DialogFragment implements OnDateChangedListener, OnTimeChangedListener {
	private DatePicker datePicker;
	private TimePicker timePicker;
	
	private DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
	private Calendar calendar = Calendar.getInstance();
	
	public interface OnDateTimeSetListener {
		public void onDateTimeSet(DatePicker datePicker, TimePicker timePicker, int year, int month, int day, int hour, int minute);
	}
	
	private OnDateTimeSetListener listener;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    View view = inflater.inflate(R.layout.frag_entry_datetime, null);
	    ((LinearLayout) view).setOrientation(getResources().getConfiguration().orientation);	    
	    view.requestLayout();
	    builder.setView(view);
	   	  
	    datePicker = (DatePicker) view.findViewById(R.id.pick_date);
	    timePicker = (TimePicker) view.findViewById(R.id.pick_time);
	    
	    builder.setTitle("");
	    
	    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (listener != null) {
					listener.onDateTimeSet(datePicker, timePicker, 
							datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), 
							timePicker.getCurrentHour(), timePicker.getCurrentMinute());
				}
			}
		});
	    deferredInit();
	    builder.setTitle(formatter.format(calendar.getTime()));
	    return builder.create();
	}
	
	public void init(int year, int month, int day, int hour, int minute) {
		if (datePicker != null && timePicker != null)
			deferredInit();
		calendar.set(year, month, day, hour, minute);
	}

	protected void deferredInit() {
		datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), this);
		timePicker.setCurrentHour(calendar.get(Calendar.HOUR));
		timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
		timePicker.setOnTimeChangedListener(this);				
	}
	
	public void setOnDateTimeSetListener(OnDateTimeSetListener listener) {
		this.listener = listener;
	}
	
	protected void updateTitle() {
		getDialog().setTitle(formatter.format(calendar.getTime()));
	}
	
	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		calendar.set(Calendar.MINUTE, minute);
		updateTitle();
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, monthOfYear);
		calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		updateTitle();
	}
}
