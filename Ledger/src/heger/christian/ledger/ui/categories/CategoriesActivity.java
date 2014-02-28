package heger.christian.ledger.ui.categories;

import heger.christian.ledger.R;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.ui.categories.EditCategoryDialog.EditCategoryDialogListener;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class CategoriesActivity extends ListActivity implements LoaderCallbacks<Cursor> {
	private class EditDialogListener implements EditCategoryDialogListener {
		private final long id;
		private final String caption;

		private EditDialogListener(long id, String caption) {
			this.id = id;
			this.caption = caption;
		}

		@Override
		public void onClose(String caption) {
			if (caption != this.caption) {
				Uri uri = ContentUris.withAppendedId(CategoryContract.CONTENT_URI, id);
				ContentValues values = new ContentValues();
				values.put(CategoryContract.COL_NAME_CAPTION, caption);
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, values, null, null);
			}
		}
	}

	private static final String EDIT_DIALOG_TAG = EditCategoryDialog.class.getCanonicalName();

	private static final String ARG_EDITING_ID = "editing_id";
	private static final String ARG_EDITING_CAPTION = "editing_caption";
	
	private CursorAdapter adapter;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.categories, menu);
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set padding for content view programmatically since there is no layout resource we could set it in
		try {
			View parent = (View) getListView().getParent();
			DisplayMetrics metrics = getResources().getDisplayMetrics();
		    int horizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getDimension(R.dimen.activity_horizontal_margin), metrics);
		    int vertical = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getDimension(R.dimen.activity_vertical_margin), metrics);
			parent.setPadding(horizontal, vertical, horizontal, vertical);
		} catch (ClassCastException x) {
			Log.e(this.getClass().getCanonicalName(), "Failed to find parent view of list for setting padding", x);
		}
		
		adapter = new SimpleCursorAdapter(this, 
				R.layout.listitem_categories, 
				null, 
				new String[] { CategoryContract.COL_NAME_CAPTION }, 
				new int[] { R.id.txt_caption }, 0);

		getLoaderManager().initLoader(0, null, this);
		
		ListView list = getListView();
		list.setAdapter(adapter);
		list.setItemsCanFocus(true);
		
		// Re-created activity while a category was being edited.
		// Reattach dialog listener
		if (savedInstanceState != null) {
			EditCategoryDialog dialog = (EditCategoryDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
			if (dialog != null && savedInstanceState.containsKey(ARG_EDITING_ID)) {
				dialog.setDialogListener(new EditDialogListener(savedInstanceState.getLong(ARG_EDITING_ID), 
						savedInstanceState.getString(ARG_EDITING_CAPTION, "")));
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		// If currently editing, store edited id and old caption
		EditCategoryDialog dialog = (EditCategoryDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
		if (dialog != null) {
			EditDialogListener listener = (EditDialogListener) dialog.getDialogListener();
			state.putLong(ARG_EDITING_ID, listener.id);
			state.putString(ARG_EDITING_CAPTION, listener.caption);
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, CategoryContract.CONTENT_URI, null, null, null, CategoryContract._ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {		
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
		
	/**
	 * Edits the item at the specified position
	 * @param position - Position in the list to edit. Must be larger than the number of header views
	 * @throws IllegalArgumentException - If the position corresponds to one of the header views
	 */
	public void edit(int position) {
		if (position < getListView().getHeaderViewsCount()) {
			throw new IllegalArgumentException("Cannot edit position " + position + " with header views count " + getListView().getHeaderViewsCount());
		} else {
			position -= getListView().getHeaderViewsCount();
		}
		long id = adapter.getItemId(position);
		
		Cursor cursor = (Cursor) adapter.getItem(position);
		String caption = cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION));
		
		EditCategoryDialog dialog = EditCategoryDialog.newInstance(caption);
		dialog.setDialogListener(new EditDialogListener(id, caption));
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
	}

	public boolean onAddClick(MenuItem menu) {
		ContentValues values = new ContentValues();					
		values.put(CategoryContract.COL_NAME_CAPTION, getResources().getString(R.string.new_category));
		new AsyncQueryHandler(getContentResolver()) {						
			@Override
			public void onInsertComplete(int token, Object cookie, Uri uri) {
				final long id = ContentUris.parseId(uri);
				final ListView list = getListView();
				
				// Ugly hack: 
				// Add temporary layout listener to the list view to make sure the list view and its adapter have
				// already been updated to the insert before scrolling and editing 
				list.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() { 
						list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						
						int position;
						for (position = 0; position < adapter.getCount(); position++)
							if (adapter.getItemId(position) == id) break;
						position += list.getHeaderViewsCount();
						list.smoothScrollToPosition(position);
						edit(position);
					}
				});
			}
		}.startInsert(0, null, CategoryContract.CONTENT_URI, values);
		return true;
	}
	
	public void onEditClick(View view) {
		View item = (View) view.getParent();
		ListView list = getListView();
		
		int position = list.getPositionForView(item);
		edit(position);
	}
	
	public void onDeleteClick(View view) {
		View item = (View) view.getParent();
		ListView list = getListView();
		
		int position = list.getPositionForView(item) - list.getHeaderViewsCount();
		long id = adapter.getItemId(position);
		Uri uri = ContentUris.withAppendedId(CategoryContract.CONTENT_URI, id);
		
		new AsyncQueryHandler(getContentResolver()) {
		}.startDelete(0, null, uri, null, null);
	}
}
