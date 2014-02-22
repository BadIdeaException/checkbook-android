package heger.christian.ledger.ui.spreadsheet;

import heger.christian.ledger.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;

public class SelectMonthDialog extends DialogFragment implements OnValueChangeListener {
	protected static final String ARG_MIN_MONTH = "arg_min_month";
	protected static final String ARG_MIN_YEAR = "arg_min_year";
	protected static final String ARG_MONTH = "arg_month";
	protected static final String ARG_YEAR = "arg_year";
	
	private NumberPicker pickMonth, pickYear;
	private int minMonth = Calendar.JANUARY;
	
	public interface DialogListener {
		/**
		 * Callback to be invoked when the user has clicked the positive button of the dialog
		 * @param dialog - The dialog that was clicked
		 * @param month - The selected month
		 * @param year - The selected year
		 */
		public void onDialogPositiveClick(DialogFragment dialog, int month, int year);
		/**
		 * Callback to be invoked when the user has clicked the negative button of the dialog
		 * @param dialog - The dialog that was clicked
		 * @param month - The selected month at the time of the click
		 * @param year - The selected year at the time of the click
		 */		
		public void onDialogNegativeClick(DialogFragment dialog, int month, int year);
	}
	private DialogListener listener;
	
	public static SelectMonthDialog newInstance(int minMonth, int minYear, int month, int year) {
		SelectMonthDialog fragment = new SelectMonthDialog();
		Bundle args = new Bundle();
		args.putInt(ARG_MIN_MONTH, minMonth);		
		args.putInt(ARG_MIN_YEAR, minYear);
		args.putInt(ARG_MONTH, month);
		args.putInt(ARG_YEAR, year);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstance) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dlg_select_month, null);
		builder.setView(view);	
		
		DialogInterface.OnClickListener internalClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (listener == null) return;			
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE: 
						listener.onDialogPositiveClick(SelectMonthDialog.this, pickMonth.getValue(), pickYear.getValue());
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						listener.onDialogNegativeClick(SelectMonthDialog.this, pickMonth.getValue(), pickYear.getValue());
				}
			}
		};
		builder.setPositiveButton(android.R.string.ok, internalClickListener)
			.setNegativeButton(android.R.string.cancel, internalClickListener);
				
		pickMonth = (NumberPicker) view.findViewById(R.id.pick_month);
		pickMonth.setDisplayedValues(new DateFormatSymbols().getMonths());
		pickYear = (NumberPicker) view.findViewById(R.id.pick_year);
		pickYear.setOnValueChangedListener(this);		
		pickYear.setMinValue(1970); // Fallback in case no argument bundle was attached
		pickYear.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
		
		Bundle args = getArguments();
		if (args != null) {
			Calendar now = Calendar.getInstance();
			setMin(args.getInt(ARG_MIN_MONTH, now.get(Calendar.MONTH)), 
					args.getInt(ARG_MIN_YEAR, now.get(Calendar.YEAR)));
			setValue(args.getInt(ARG_MONTH, now.get(Calendar.MONTH)),
					args.getInt(ARG_YEAR, now.get(Calendar.YEAR)));
		}
		
		return builder.create();
	}
	
	private void setMin(int minMonth, int minYear) {
		this.minMonth = minMonth;		
		pickYear.setMinValue(minYear);
	}

	private void adjustPickerBounds(int year) {
		int minVal = Calendar.JANUARY;
		int maxVal = Calendar.DECEMBER;
		
		if (year == pickYear.getMinValue()) {
			minVal = minMonth;			
		} 
		if (year == Calendar.getInstance().get(Calendar.YEAR)) {
			maxVal = Calendar.getInstance().get(Calendar.MONTH);
		}		
		pickMonth.setMinValue(minVal);
		pickMonth.setMaxValue(maxVal);
		pickMonth.setWrapSelectorWheel(minVal == Calendar.JANUARY && maxVal == Calendar.DECEMBER);		
	}
	
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		adjustPickerBounds(newVal);
	}
	
	private void setValue(int month, int year) {
		pickYear.setValue(year);
		adjustPickerBounds(year);
		pickMonth.setValue(month);
	}

	public DialogListener getDialogListener() {
		return listener;
	}

	public void setDialogListener(DialogListener listener) {
		this.listener = listener;
	}
}
