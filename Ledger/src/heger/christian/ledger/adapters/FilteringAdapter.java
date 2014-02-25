package heger.christian.ledger.adapters;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

/**
 * This class takes an <code>Adapter</code> and filters it by virtue of its {@link filter} method.
 * Filtering is done on demand, and filtering results are not cached by this class. Therefore, it is 
 * not suited well to scenarios where filtering requires heavy weight operations or the number of 
 * items in the internal adapter is large.
 * @author chris
 *
 */
public abstract  class FilteringAdapter implements SpinnerAdapter,ListAdapter {
	private final Adapter adapter;
	
	public FilteringAdapter(Adapter adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("Cannot instantiate without an adapter");
		this.adapter = adapter;
	}
	
	/**
	 * This method performs the actual filtering. When passed a position within the filtered adapter, it
	 * must return <code>true</code> if the value belonging to that position in the filtered adapter should
	 * be exposed by the <code>FilteringAdapter</code>, <code>false</code> if it shouldn't.
	 * <p>
	 * This implementation performs no caching of results. Therefore, since this method gets called 
	 * frequently, it should be fairly lightweight.
	 * @param position - The position in the filtered adapter to check
	 * @return Whether the indicated position in the filtered adapter should be accessible.
	 */
	public abstract boolean filter(int position);
	
	protected int getInternalPosition(int position) {
		int result = 0;
		while (position >= 0) {
			result++;
			if (filter(result)) 
				position--;
		}
		return result;
	}
	
	public Adapter getAdapter() {
		return adapter;
	}
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		adapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		adapter.unregisterDataSetObserver(observer);
	}

	@Override
	public int getCount() {
		int count = 0;
		for (int i = 0; i < adapter.getCount(); i++)
			if (filter(i))
				count++;
		return count;
	}

	@Override
	public Object getItem(int position) {
		return adapter.getItem(getInternalPosition(position));
	}

	@Override
	public long getItemId(int position) {
		return adapter.getItemId(getInternalPosition(position));
	}

	@Override
	public boolean hasStableIds() {
		return adapter.hasStableIds();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return adapter.getView(getInternalPosition(position), convertView, parent);
	}

	@Override
	public int getItemViewType(int position) {
		return adapter.getItemViewType(getInternalPosition(position));
	}

	@Override
	public int getViewTypeCount() {
		return adapter.getViewTypeCount();
	}

	@Override
	public boolean isEmpty() {
		int position = 0;
		while (position < adapter.getCount())
			if (filter(position))
				return true;
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return ((ListAdapter) adapter).isEnabled(getInternalPosition(position));
	}

	@Override
	public boolean areAllItemsEnabled() {
		return ((ListAdapter) adapter).areAllItemsEnabled();
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return ((SpinnerAdapter) adapter).getDropDownView(getInternalPosition(position), convertView, parent);
	}
}
