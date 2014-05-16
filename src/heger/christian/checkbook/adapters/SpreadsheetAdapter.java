package heger.christian.checkbook.adapters;

import heger.christian.checkbook.R;
import heger.christian.checkbook.providers.CategoryContract;
import heger.christian.checkbook.providers.EntryContract;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;

public class SpreadsheetAdapter extends SimpleCursorTreeAdapter {
	private static final String TAG = "SpreadsheetAdapter";

	protected static final int GRP_TYPE_ITEM = 0;
	protected static final int GRP_TYPE_DIVIDER = 1;

	private final String sqlTimestamp;

	private LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		/*
		 * groupPosition for which to load is expected to be passed in id
		 */
		@Override
		public Loader<Cursor> onCreateLoader(int groupPosition, Bundle args) {
			Cursor groupCursor = getGroup(groupPosition);
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
		groupPosition *= 2;

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

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setBackground(View view, int resourceId) {
		int[] attrs = new int[] { android.R.attr.selectableItemBackground };
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs);
		Drawable selectableItemBackground = a.getDrawable(0);
		a.recycle();

		LayerDrawable layers = new LayerDrawable(new Drawable[] {
				context.getResources().getDrawable(resourceId),
				selectableItemBackground
		});

		// Setting the nine-patch will overwrite the padding values,
		// so we'll save them here and reset them after
		int left = view.getPaddingLeft();
		int top = view.getPaddingTop();
		int right = view.getPaddingRight();
		int bottom = view.getPaddingBottom();

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackgroundDrawable(layers);
		} else {
			view.setBackground(layers);
		}

		view.setPadding(left, top, right, bottom);
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view;
		if (getGroupType(groupPosition) == GRP_TYPE_ITEM) {
			groupPosition = getInternalGroupPosition(groupPosition);
			view = super.getGroupView(groupPosition, isExpanded, convertView, parent);
			if (isExpanded && super.getChildrenCount(groupPosition) > 0) {
				setBackground(view, R.drawable.tile_top);
			} else {
				setBackground(view, R.drawable.tile_full);
			}
		} else {
			view = new View(this.context);
			view.setBackgroundResource(R.drawable.spacer);
		}
		return view;
	}

	private int getInternalGroupPosition(int groupPosition) {
		return groupPosition / 2;
	}

	@Override
	public int getGroupType(int groupPosition) {
		return groupPosition % 2;
	}

	@Override
	public int getGroupTypeCount() { return 2; }

	@Override
	public int getGroupCount() {
		int count = super.getGroupCount();
		if (count > 0) count = 2 * count - 1;
		return count;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		groupPosition = getInternalGroupPosition(groupPosition);
		View view = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
		if (isLastChild) {
			setBackground(view, R.drawable.tile_bottom);
		} else {
			setBackground(view, R.drawable.tile_middle);
		}
		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return 0; }
		groupPosition = getInternalGroupPosition(groupPosition);
		return super.getChildrenCount(groupPosition);
	}

	@Override
	public Cursor getGroup(int groupPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return null; }
		groupPosition = getInternalGroupPosition(groupPosition);
		return super.getGroup(groupPosition);
	}

	@Override
	public Cursor getChild(int groupPosition, int childPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return null; }
		groupPosition = getInternalGroupPosition(groupPosition);
		return super.getChild(groupPosition, childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return ExpandableListView.INVALID_ROW_ID; }
		groupPosition = getInternalGroupPosition(groupPosition);
		return super.getChildId(groupPosition, childPosition);
	}

	@Override
	public void setChildrenCursor(int groupPosition, Cursor childrenCursor) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { throw new IllegalArgumentException("Tried to set children for divider group " + groupPosition); }
		groupPosition = getInternalGroupPosition(groupPosition);
		if (childrenCursor != null)
			indexChildColumns(childrenCursor);
		if (getCursor() != null)
			super.setChildrenCursor(groupPosition, childrenCursor);
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return; }
		groupPosition = getInternalGroupPosition(groupPosition);
		super.onGroupExpanded(groupPosition);
	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
		if (getGroupType(groupPosition) == GRP_TYPE_DIVIDER) { return; }
		groupPosition = getInternalGroupPosition(groupPosition);
		super.onGroupCollapsed(groupPosition);
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
