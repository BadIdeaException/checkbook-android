package heger.christian.ledger.ui.categories;

import heger.christian.ledger.OutOfKeysReaction;
import heger.christian.ledger.OutOfKeysReaction.KeyRequestResultListener;
import heger.christian.ledger.R;
import heger.christian.ledger.SturdyAsyncQueryHandler;
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
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class CategoriesActivity extends ListActivity implements LoaderCallbacks<Cursor> {
	private class AddCategoryDialogListener implements EditCategoryDialogListener {
		@Override
		public void onClose(String caption) {			
			final ContentValues values = new ContentValues();
			values.put(CategoryContract.COL_NAME_CAPTION, caption);
			new SturdyAsyncQueryHandler(getContentResolver()) {
				@Override
				public void onError(int token, Object cookie, RuntimeException error) {
					OutOfKeysReaction handler = OutOfKeysReaction.newInstance(CategoriesActivity.this);
					handler.setResultListener(new KeyRequestResultListener() {
						@Override
						public void onSuccess() {
							// New keys are available, try again
							startInsert(0, null, CategoryContract.CONTENT_URI, values);							
						}
						@Override
						public void onFailure() {}
					});
					handler.handleOutOfKeys();
				}
			}.startInsert(0, null, CategoryContract.CONTENT_URI, values);
		}
	}
	private class ModifyCategoryDialogListener implements EditCategoryDialogListener {
		private final long id;
		private final String caption;

		private ModifyCategoryDialogListener(long id, String caption) {
			this.id = id;
			this.caption = caption;
		}

		@Override
		public void onClose(String caption) {
			if (!caption.equals(this.caption)) {
				Uri uri = ContentUris.withAppendedId(CategoryContract.CONTENT_URI, id);
				ContentValues values = new ContentValues();
				values.put(CategoryContract.COL_NAME_CAPTION, caption);
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, values, null, null);
			}
		}
	}

	private static final String EDIT_DIALOG_TAG = EditCategoryDialog.class.getCanonicalName();

	private static final String STATE_EDITING_ID = "editing_id";
	private static final String STATE_EDITING_VALUE_CAPTION = "editing_caption";
	
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
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		adapter = new SimpleCursorAdapter(this, 
				R.layout.listitem_categories, 
				null, 
				new String[] { CategoryContract.COL_NAME_CAPTION }, 
				new int[] { R.id.txt_caption }, 0) {
			@Override
			public long getItemId(int position) {
				Cursor cursor = getCursor();
				cursor.moveToPosition(position);
				return cursor.getLong(cursor.getColumnIndex(CategoryContract._ID));
			}
		};

		getLoaderManager().initLoader(0, null, this);
		
		ListView list = getListView();
		list.setAdapter(adapter);
		list.setItemsCanFocus(true);
		
		// Re-created activity while a category was being edited.
		// Reattach dialog listener
		if (savedInstanceState != null) {
			EditCategoryDialog dialog = (EditCategoryDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
			if (dialog != null) {
				EditCategoryDialogListener listener;
				if (savedInstanceState.containsKey(STATE_EDITING_ID)) {
					// Create listener for modifying an existing rule
					String caption = savedInstanceState.getString(STATE_EDITING_VALUE_CAPTION);
					if (caption == null) caption = "";
					listener = new ModifyCategoryDialogListener(savedInstanceState.getLong(STATE_EDITING_ID), caption);
				} else {
					// Create listener for a new rule
					listener = new AddCategoryDialogListener();
				}
				dialog.setDialogListener(listener);
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		// If currently editing an existsting category, store edited id and old caption
		EditCategoryDialog dialog = (EditCategoryDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
		if (dialog != null) {
			EditCategoryDialogListener listener = (EditCategoryDialogListener) dialog.getDialogListener();
			// If modifying existing category, save parameters necessary to recreate
			if (listener instanceof ModifyCategoryDialogListener) {
				state.putLong(STATE_EDITING_ID, ((ModifyCategoryDialogListener) listener).id);
				state.putString(STATE_EDITING_VALUE_CAPTION, ((ModifyCategoryDialogListener) listener).caption);
			}
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
		
	public boolean onAddClick(MenuItem menu) {
		EditCategoryDialog dialog = EditCategoryDialog.newInstance(getResources().getString(R.string.new_category));
		dialog.setDialogListener(new AddCategoryDialogListener());
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
		
		return true;
	}
	
	public void onEditClick(View view) {
		View item = (View) view.getParent();
		ListView list = getListView();
		int position = list.getPositionForView(item);
		position -= getListView().getHeaderViewsCount();

		long id = adapter.getItemId(position);
		Cursor cursor = (Cursor) adapter.getItem(position);
		String caption = cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION));
		
		EditCategoryDialog dialog = EditCategoryDialog.newInstance(caption);
		dialog.setDialogListener(new ModifyCategoryDialogListener(id, caption));
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
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
