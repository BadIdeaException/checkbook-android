package heger.christian.ledger.control;

import heger.christian.ledger.BuildConfig;
import heger.christian.ledger.R;
import heger.christian.ledger.adapters.CategoriesAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;

public class CategoryEditorProxy {
	public static final String TAG = "CategoryEditorProxy";

	private static final long PROXY_CLOSED_ID = CategoriesAdapter.NOT_EDITING;
	
	private long id; // Combined id
	private static class CachedValues {	
		private String caption;
		private int cursorPos;
	}
	private CachedValues cachedValues = new CachedValues();
	private boolean cacheValid = false;
	
	private View item;
	private ExpandableListView list;
	private CategoriesAdapter adapter;
	
	private class ProxyScrollListener implements OnScrollListener {
		int position = ListView.INVALID_POSITION;;
		int prevScrollPosition = list.getFirstVisiblePosition(); 
		int prevVisibleItemCount = list.getLastVisiblePosition() - prevScrollPosition + 1;
		
		/**
		 * Perform a full search of the visible portion of the list in order to 
		 * acquire the item view and position values.
		 * Returns true if the search was successful. 
		 */
		private boolean fullSearch() {
			int firstVisiblePosition = list.getFirstVisiblePosition();
			// Find initial position of item if it is visible
			for (position = 0; position < list.getChildCount(); position++) {
				if (list.getItemIdAtPosition(position + firstVisiblePosition) == id) {
					item = list.getChildAt(position);
					return true;
				}
			}
			position = ListView.INVALID_POSITION;
			return false;
		}
		private boolean needsFullSearch() {
			return position == ListView.INVALID_POSITION && item == null;
		}
		public void onScrollStateChanged(AbsListView view, int scrollState) {}		
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (needsFullSearch()) {
				fullSearch();
				return;
			}
			
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
				cachedValues.caption = editCaption.getText().toString();
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
	
	public CategoryEditorProxy(ExpandableListView list, CategoriesAdapter adapter, long id) {
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
	
	public long getId() {
		return id;
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
	
	public void setInitialValue(String caption) {
		cachedValues.caption = caption;
		cacheValid = true;
	}
	
	public boolean wantsOverride() {
		return isAlive() && (item != null || cacheValid);
	}
}
