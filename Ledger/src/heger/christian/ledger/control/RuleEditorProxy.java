package heger.christian.ledger.control;

import heger.christian.ledger.BuildConfig;
import heger.christian.ledger.R;
import heger.christian.ledger.adapters.RulesAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

public class RuleEditorProxy {	
	private static final String TAG = "RuleEditorProxy";
	
	protected static final long PROXY_CLOSED_ID = RulesAdapter.NOT_EDITING;
	
	private long id;
	
	private static class CachedValues {
		private String caption;
		private int category;

		private int cursorPos;
	}
	private CachedValues cachedValues = new CachedValues();
	private boolean cacheValid = false;
	
	private View item;
	private ListView list;
	private RulesAdapter adapter;
	
	private class ProxyScrollListener implements OnScrollListener {
		int position;
		int prevScrollPosition = list.getFirstVisiblePosition(); 
		int prevVisibleItemCount = list.getLastVisiblePosition() - prevScrollPosition + 1;
		public ProxyScrollListener() {
			int firstVisiblePosition = list.getFirstVisiblePosition();
			// Find initial position of item if it is visible
			for (position = 0; position < list.getChildCount(); position++) {
				if (list.getItemIdAtPosition(position + firstVisiblePosition) == id) {
					item = list.getChildAt(position);
					return;
				}
			}
			position = ListView.INVALID_POSITION;
		}
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {}		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// If the visible portion of the list hasn't actually changed, exit now to save performance
			if (prevScrollPosition == firstVisibleItem && visibleItemCount == prevVisibleItemCount)
				return;
			
			// Update values
			prevScrollPosition = firstVisibleItem;
			prevVisibleItemCount = visibleItemCount;
			int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
						
			if (item != null) {
				// If we're currently holding a reference to the item, that must mean it was visible on the last
				// scroll. In that case, it can currently be in three positions: in the same position as previously,
				// right above that position, or right below. 
				// Check those positions, if the item couldn't be found there, it must have been scrolled
				// off screen.	
				// (Note: Despite the premature exit at the beginning of this callback, the item can still be in the 
				// same position as previously, because the window size may have changed.))
				
				// Check previous position
				int p = position;
				if (p >= 0 && list.getChildAt(p) == item) {
					// If item was found there, update position and exit
					position = p;
					return;
				}
				// Check position before previous position
				p = position - 1;
				if (p >= 0 && list.getChildAt(p) == item) {					
					position = p;
					return;
				}
				// Check position after previous position
				p = position + 1;
				if (p < visibleItemCount && list.getChildAt(p) == item) {
					position = p;
					return;
				}
				// If the item could not be found in any of the possible positions,
				// it must have been scrolled off screen. Cache value and invalidate item reference
				EditText editCaption = (EditText) item.findViewById(R.id.edit_caption);
				Spinner spinCategory = (Spinner) item.findViewById(R.id.spin_category);
				cachedValues.caption = editCaption.getText().toString();
				cachedValues.category = (int) spinCategory.getSelectedItemId();
				cachedValues.cursorPos = editCaption.getSelectionStart();
				cacheValid = true;
				item = null;
				if (BuildConfig.DEBUG) Log.d(TAG,"Invalidated item and cached value " + cachedValues.caption);				
			} else {
				// If we're not currently holding an item reference, that means the item was invisible
				// on the last scroll. In that case, it can now either scroll into view at the top or the
				// bottom of the list. Check those two positions, if both come up blank, the item is still
				// invisible.
				if (list.getItemIdAtPosition(firstVisibleItem) == id) {
					// Scrolled on screen: acquire view handling the editing
					item = list.getChildAt(0);
					EditText editCaption = (EditText) item.findViewById(R.id.edit_caption);
					editCaption.setSelection(cachedValues.cursorPos);
					position = 0;
					if (BuildConfig.DEBUG) Log.d(TAG,"Acquired item " + item);
				} else if (list.getItemIdAtPosition(lastVisibleItem) == id) {
					item = list.getChildAt(visibleItemCount - 1);
					EditText editCaption = (EditText) item.findViewById(R.id.edit_caption);
					editCaption.setSelection(cachedValues.cursorPos);
					position = visibleItemCount - 1;
					if (BuildConfig.DEBUG) Log.d(TAG,"Acquired item " + item);
				}
				
			}
		}
	};
	
//	private class ProxyScrollListener implements OnScrollListener {
//		int position = ListView.INVALID_POSITION;
//		public void onScrollStateChanged(AbsListView view, int scrollState) {}		
//		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//			// Lazily calculate position in the list
//			if (position == ListView.INVALID_POSITION) {
//				position = adapter.getItemPosition(id);
//				if (position == -1) 
//					throw new NoSuchElementException("Could not find item with id " + id);
//				position += list.getHeaderViewsCount();
//			}
//			
//			int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;			
//			if (BuildConfig.DEBUG) Log.d(TAG,"Current visible window on list is " + firstVisibleItem + " to " + lastVisibleItem + ", edited position is " + position);
//			// Scrolled off screen: store values, invalidate view reference
//			if (item != null &&
//					(position < firstVisibleItem || position > lastVisibleItem)) {
//				caption = ((EditText) item.findViewById(R.id.edit_caption)).getText().toString();
//				category = (int) ((Spinner) item.findViewById(R.id.spin_category)).getSelectedItemId();
//				cacheValid = true;
//				item = null;
//				if (BuildConfig.DEBUG) Log.d(TAG,"Invalidated item and cached values (" + caption + "," + category +")");
//			}
//			// Scrolled on screen: acquire view handling the editing
//			if (item == null &&
//					(position >= firstVisibleItem && position <= lastVisibleItem)) {
//				item = list.getChildAt(position - firstVisibleItem);
//				cacheValid = false;
//				if (BuildConfig.DEBUG) Log.d(TAG,"Acquired item " + item);
//			}
//		}
//	};
	
	public RuleEditorProxy(ListView list, RulesAdapter adapter, long id) {
		this.list = list;
		this.id = id;
		this.adapter = adapter;
		adapter.setProxy(this);
		list.setOnScrollListener(new ProxyScrollListener());		
	}
	
	public void close() {		
		list.setOnScrollListener(null);
		adapter.setProxy(null);
		item = null;
		id = PROXY_CLOSED_ID;
	}
	
	public boolean isAlive() {
		return id != PROXY_CLOSED_ID;
	}
	
	private boolean isVisible() {
		return item != null;
	}
	
	public String getCaption() {
		if (!isAlive()) 
			throw new IllegalStateException("Cannot access closed proxy");
			
		// View is visible, read value from ui
		if (isVisible()) {			
			return ((EditText) item.findViewById(R.id.edit_caption)).getText().toString();
		} else
			// Return stored value
			return cachedValues.caption;
	}
	
	public void setInitialValues(String caption, int category) {
		cachedValues.caption = caption;
		cachedValues.category = category;
		cacheValid = true;		
	}
	
	public long getId() {
		return id;
	}
	
	public int getCategory() {
		if (!isAlive()) 
			throw new IllegalStateException("Cannot access closed proxy");

		if (isVisible()) {
			return (int) ((Spinner) item.findViewById(R.id.spin_category)).getSelectedItemId();
		} else 
			return cachedValues.category;
	}

	public boolean wantsOverride() {
		return (item != null || cacheValid);
	}
}
