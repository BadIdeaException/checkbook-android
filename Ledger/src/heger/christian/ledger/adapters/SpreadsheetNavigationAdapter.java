package heger.christian.ledger.adapters;

import heger.christian.ledger.ui.spreadsheet.SpreadsheetActivity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public class SpreadsheetNavigationAdapter implements SpinnerAdapter {
	/*
	 * Item id is coded as follows:
	 * 1. Bit 64 is set if the item is a virtual entry, that is, one not corresponding directly to a month
	 * 2. Bit 63 is set if the item is clickable, otherwise this is graphical fanciwork. This is only evaluated if bit 64 is set
	 * 3. If bit 64 is set, the lower 32 bits encode the exact type of virtual entry. If it is unset, the lower 32 bits contain the month id
	 */
	protected static final long ITEM_THREE_DOTS = 0x8000000000000001l;
	protected static final long ITEM_DIVIDER = 0x8000000000000002l;
	protected static final long ITEM_GO_TO_MONTH = 0xC000000000000001l;
	protected static final long ITEM_GO_TO_LATEST = 0xC000000000000002l;
	
	private Context context;
	private int current;
	private int latest;
	private List<Long> items;
	
	public SpreadsheetNavigationAdapter(Context context, int current, int latest) {
		this.context = context;
		this.current = current;
		this.latest = latest;
	}

	public void registerDataSetObserver(DataSetObserver observer) {
		// TODO Auto-generated method stub

	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		// TODO Auto-generated method stub

	}

	public int getCount() {
		
		return 2;
	}

	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasStableIds() {
		return true;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View ruler = new View(context); 
		ruler.setBackgroundColor(0xFF00FF00);
//		theParent.addView(ruler,
//		new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 2));
		return ruler;
	}

	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 1;
	}

	public boolean isEmpty() {
		return false;
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent) {
//		long id = items.get(position);
		View ruler = new View(context); 
		ruler.setBackgroundColor(0xFF00FF00);
//		theParent.addView(ruler,
//		new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 2));
		return ruler;

	}

	public void setCurrent(int current) {
		this.current = current;
		// Compute number of entries there will be
		int size = 5; // Two past month + divider line + go to month + go to current
		switch (latest - current) { 
			case 0: break;
			case 1:
			case 2:
				size += latest - current + 1; // +1: Inselectable "..." item
				break;
			default: size += 3;
		}
		items = new ArrayList<Long>(size);
	}
}
