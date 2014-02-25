package heger.christian.ledger.adapters;

import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.EntryContract;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.SimpleCursorTreeAdapter;

public class SpreadsheetAdapter extends SimpleCursorTreeAdapter {
	private final String sqlTimestamp;
	
	private LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		/*
		 * groupPosition for which to load is expected to be passed in id
		 */
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Cursor groupCursor = getGroup(id);
			long category = groupCursor.getLong(groupIdColumn);
			// Get all entries that belong to the given category and whose date/time column, when reset to the 
			// beginning of the month, matches the month/year combo this adapter was created for
			return new CursorLoader(context, 
					EntryContract.CONTENT_URI, 
					new String[] { EntryContract._ID, EntryContract.COL_NAME_CAPTION, EntryContract.COL_NAME_CATEGORY, EntryContract.COL_NAME_DATETIME, EntryContract.COL_NAME_VALUE },
					EntryContract.COL_NAME_CATEGORY + "=" + category + " AND "  
							+ "datetime(" + EntryContract.COL_NAME_DATETIME + ",'start of month')=?",
					new String[] { sqlTimestamp },

					EntryContract.COL_NAME_DATETIME);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {		
			setChildrenCursor(loader.getId(), data);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			setChildrenCursor(loader.getId(), null);
		}
	};
	
	public SpreadsheetAdapter(Context context, Cursor cursor, int groupLayout,
			String[] groupFrom, int[] groupTo, int childLayout,
			String[] childFrom, int[] childTo, int month, int year) {
		super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom,
				childTo);
		this.context = context;
		if (cursor != null)
			indexGroupColumns(cursor);
		// Prepare SQL timestring for the beginning of the month and year this adapter is created for
		// month + 1: passed month is zero-based here, but isn't in SQL
		sqlTimestamp = String.valueOf(year) + "-" + String.format("%02d",month + 1) + "-01 00:00:00";
	}

	private int groupIdColumn;
	private int childIdColumn;
	
	private Context context;
	
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int groupPosition = groupCursor.getPosition();
		
		LoaderManager loaderManager = ((FragmentActivity) context).getSupportLoaderManager();
		Loader<?> loader = loaderManager.getLoader(groupPosition);
		// If a loader is already associated with this position, and that loader has not been reset, do so.
		// Otherwise initialize a new loader.
		// Always initiating a new loader will result in an infinite cycle and stack overflow 
		if (loader != null && !loader.isReset()) {
			loaderManager.restartLoader(groupPosition, null, loaderCallbacks);
		} else {
			loaderManager.initLoader(groupPosition, null, loaderCallbacks);
		}
		// Children cursor will be set by setChildrenCursor in loader callback
		return null;
	}
	
	@Override
	public void setChildrenCursor(int groupPosition, Cursor childrenCursor) {
		if (childrenCursor != null)
			indexChildColumns(childrenCursor);
		if (getCursor() != null)
			super.setChildrenCursor(groupPosition, childrenCursor);
	}

	@Override
	public void setGroupCursor(Cursor groupCursor) {
		if (groupCursor != null)
			indexGroupColumns(groupCursor);
		super.setGroupCursor(groupCursor);
	}
	
	private void indexGroupColumns(Cursor sample) {
		groupIdColumn = sample.getColumnIndex(CategoryContract._ID);
	}
	
	private void indexChildColumns(Cursor sample) {
		childIdColumn = sample.getColumnIndex(EntryContract._ID);
	}
}
