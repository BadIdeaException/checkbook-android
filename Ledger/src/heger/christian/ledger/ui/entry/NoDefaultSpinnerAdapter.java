package heger.christian.ledger.ui.entry;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class NoDefaultSpinnerAdapter implements SpinnerAdapter {
	public static final int HIDING_POSITION = 0;
	
	private SpinnerAdapter adapter;
	private Context context;
	private String defaultText;
	
	public NoDefaultSpinnerAdapter(Context context, SpinnerAdapter adapter) {
		this.adapter = adapter;
		this.context = context;
	}
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (adapter != null)
			adapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (adapter != null)
			adapter.unregisterDataSetObserver(observer);
	}

	@Override
	public int getCount() {
		return 1 + (adapter != null ? adapter.getCount() : 0);
	}

	@Override
	public Object getItem(int position) {
		if (position != HIDING_POSITION && adapter != null)
			return adapter.getItem(position > HIDING_POSITION ? position - 1 : position);
	
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position != HIDING_POSITION && adapter != null)
			return adapter.getItemId(position > HIDING_POSITION ? position - 1 : position);
		return 0;
	}

	@Override
	public boolean hasStableIds() {
		if (adapter != null)
			return adapter.hasStableIds();
		return true;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (position != HIDING_POSITION && adapter != null)
			return adapter.getDropDownView(position > HIDING_POSITION ? position - 1 : position, convertView, parent);
		
		// FIXME Makes the spinner dropdown list appear empty
		View dummy = adapter.getDropDownView(1, null, parent);
//		dummy.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		
//		dummy = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_spinner_dropdown_item, null); 
		dummy.setVisibility(View.GONE);
		return dummy;
	}

	@Override
	public int getItemViewType(int position) {
		if (position != HIDING_POSITION && adapter != null)
			return adapter.getItemViewType(position > HIDING_POSITION ? position - 1 : position);
		return IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public int getViewTypeCount() {
		return adapter != null ? adapter.getViewTypeCount() : 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position != HIDING_POSITION && adapter != null)
			return adapter.getView(position > HIDING_POSITION ? position - 1 : position, convertView, parent);
		
		return getDefaultView(parent);
	}
	
	protected Context getContext() {
		return context;
	}
	
	protected View getDefaultView(ViewGroup parent) {
		TextView textview = new TextView(getContext());
		textview.setText(getDefaultText());
		TypedArray styledAttr = context.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorHint });
		textview.setTextColor(styledAttr.getColor(0, 0));
		styledAttr.recycle();
		return textview;
	}

	public String getDefaultText() {
		return defaultText;
	}

	public void setDefaultText(String defaultText) {
		this.defaultText = defaultText;
	}

	public void setDefaultText(int resid) {
		setDefaultText(context.getResources().getString(resid));
	}

	public SpinnerAdapter getAdapter() {
		return adapter;
	}
}
