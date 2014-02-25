package heger.christian.ledger.ui.categories;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.CategoriesAdapter;
import heger.christian.ledger.control.CategoryEditorProxy;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.SupercategoryContract;
import android.app.ActionBar;
import android.app.ExpandableListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

public class CategoriesActivity extends ExpandableListActivity implements LoaderCallbacks<Cursor> {
	private static final String TAG = "CategoryActivity";

	private static final int ACTION_BAR_DEFAULT = 
			ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME| ActionBar.DISPLAY_SHOW_TITLE;

	private static final String STATE_EDITING_ID = "editing_id";
	private static final String STATE_EDITING_VALUE_CAPTION = "editing_value_caption";
	

	private CategoriesAdapter adapter;
	private CategoryEditorProxy proxy;
	
	private View createAddSupercategoryView() {
		TextView textview = (TextView) getLayoutInflater().inflate(R.layout.add_category, null);
		textview.setText(R.string.add_supercategory);
		int paddingLeft = getResources().getDimensionPixelOffset(R.dimen.expandable_list_group_indentation);
		textview.setPadding(paddingLeft, 0, 0, 0);
		textview.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(SupercategoryContract.COL_NAME_CAPTION, getResources().getString(R.string.new_supercategory));
				new AsyncQueryHandler(getContentResolver()) {						
					@Override
					public void onInsertComplete(int token, Object cookie, Uri uri) {
						long id = adapter.getCombinedGroupId(ContentUris.parseId(uri));
						startEditing(id);
					}
				}.startInsert(0, null, SupercategoryContract.CONTENT_URI, values);
			}
		});
		return textview;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new CategoriesAdapter(this, null, R.layout.listitem_categories, R.layout.listitem_categories);
		getLoaderManager().initLoader(0, null, this);
		
		ExpandableListView list = getExpandableListView();
		list.addHeaderView(createAddSupercategoryView());
		list.setAdapter(adapter);
		list.setItemsCanFocus(true);
		
		list.setIndicatorBounds(5, 5 + Math.round(getResources().getDimension(R.dimen.expandable_list_indicator_size)));
		list.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if (childPosition == 0) {
					final long supercategory = adapter.getGroupId(groupPosition);
					ContentValues values = new ContentValues();					
					values.put(CategoryContract.COL_NAME_CAPTION, getResources().getString(R.string.new_category));
					values.put(CategoryContract.COL_NAME_SUPERCATEGORY, supercategory);
					new AsyncQueryHandler(getContentResolver()) {						
						@Override
						public void onInsertComplete(int token, Object cookie, Uri uri) {
							long id = adapter.getCombinedChildId(supercategory, ContentUris.parseId(uri));
							startEditing(id);
						}
					}.startInsert(0, null, CategoryContract.CONTENT_URI, values);
					return true;
				}
				return false;
			}
		});
		
		// Re-created activity while a row was being edited.
		// Restore editing state
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_EDITING_ID)) {
			long id = savedInstanceState.getLong(STATE_EDITING_ID);			
			proxy = new CategoryEditorProxy(list, adapter, id);
			proxy.setInitialValue(savedInstanceState.getString(STATE_EDITING_VALUE_CAPTION));
			startEditing(id);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (proxy != null && proxy.isAlive()) {
			state.putLong(STATE_EDITING_ID, adapter.getEditingCombinedId());
			state.putString(STATE_EDITING_VALUE_CAPTION, proxy.getCaption());
		}
	}
	
	@Override
	public void onBackPressed() {
		if (adapter.getEditingCombinedId() == CategoriesAdapter.NOT_EDITING)
			super.onBackPressed();
		else 
			stopEditing();
	}
	
	public void onCustomActionModeClick(View view) {
		switch (view.getId()) {
			case R.id.action_done:				
				// Store values in the database
				Uri uri;
				ContentValues values = new ContentValues();
				long id = adapter.getEditingCombinedId();
				if (adapter.isChildId(id)) {
					uri = ContentUris.withAppendedId(CategoryContract.CONTENT_URI, adapter.extractChildId(id));
					values.put(CategoryContract.COL_NAME_CAPTION, proxy.getCaption());
//					values.put(CategoryContract.COL_NAME_SUPERCATEGORY, adapter.extractGroupId(id));
				} else {
					uri = ContentUris.withAppendedId(SupercategoryContract.CONTENT_URI, adapter.extractGroupId(id));
					values.put(SupercategoryContract.COL_NAME_CAPTION, proxy.getCaption());
				}		
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, values, null, null);
				//$FALL-THROUGH$
			case R.id.action_cancel:				
				// Put the row out of editing mode
				stopEditing();
		}
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, SupercategoryContract.CONTENT_URI, null, null, null, SupercategoryContract._ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {		
		adapter.setGroupCursor(data);		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.setGroupCursor(null);
	}
	
	private void updateViewAt(long packedPosition) {
		// Only update on valid position data
		if (packedPosition != ExpandableListView.PACKED_POSITION_VALUE_NULL) {
			ExpandableListView list = getExpandableListView();
			// Convert packed position into a flat position
			int flatPosition = list.getFlatListPosition(packedPosition);
			// Get view corresponding to flat position
			View v = list.getChildAt(flatPosition);
			// Convert packed position into groupPosition/childPosition
			int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
			switch (ExpandableListView.getPackedPositionType(packedPosition)) {
				case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
					adapter.getGroupView(groupPosition, 
							list.isGroupExpanded(groupPosition), 
							v, 
							list);
					break;
				case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
					int prevChildPosition = ExpandableListView.getPackedPositionChild(packedPosition);
					adapter.getChildView(groupPosition, 
							prevChildPosition, 
							adapter.getChildrenCount(groupPosition) + 1 == prevChildPosition, 
							v, 
							list);
					break;
			}
		}
	}
	
	public void startEditing(long combinedId) {
		long prevId = adapter.getEditingCombinedId();
		adapter.setEditingCombinedId(combinedId);
		
		// Update previously edited view, if any		
		if (prevId != CategoriesAdapter.NOT_EDITING) {
			long packedPosition = getPackedPositionForId(prevId);
			updateViewAt(packedPosition);
		}
		
		// Update new editing row	
		if (combinedId != CategoriesAdapter.NOT_EDITING) {
			long packedPosition = getPackedPositionForId(combinedId);
			updateViewAt(packedPosition);
			
			// Set action bar to display custom action mode containing "Done" and "Cancel" buttons
			getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ACTION_BAR_DEFAULT | ActionBar.DISPLAY_SHOW_CUSTOM);
			getActionBar().setCustomView(R.layout.edit_actionbar);

			// Only create new proxy if none for the requested id exists
			if (!(proxy != null && proxy.isAlive() && proxy.getId() == combinedId)) {
				if (proxy != null)
					proxy.close();
				proxy = new CategoryEditorProxy(getExpandableListView(), adapter, combinedId);
			}
		}
	}
	
	public void stopEditing() {
		try {
			long prevId = adapter.getEditingCombinedId(); 
			adapter.setEditingCombinedId(CategoriesAdapter.NOT_EDITING);
			
			long packedPosition = getPackedPositionForId(prevId);
			updateViewAt(packedPosition);
			
			getActionBar().setDisplayOptions(ACTION_BAR_DEFAULT);
		} finally {
			proxy.close();		
		}
	}
	
	public void onEditClick(View view) {
		View item = (View) view.getParent();
		ExpandableListView list = getExpandableListView();
		// Convert flat list position into packed position and from there into a groupPosition/childPosition
		int flatPosition = list.getPositionForView(item);
		long packedPosition = list.getExpandableListPosition(flatPosition);
		int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
		long groupId = adapter.getGroupId(ExpandableListView.getPackedPositionGroup(packedPosition));
		
		long combinedId;
		switch (ExpandableListView.getPackedPositionType(packedPosition)) {
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				combinedId = adapter.getCombinedGroupId(groupId);
				break;
			case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
				int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
				long childId = adapter.getChildId(groupPosition, childPosition);
				combinedId = adapter.getCombinedChildId(groupId, childId);
				break;
			default:
				Log.e(TAG, "Illegal packed position when trying to start editing");
				return;
		}
		
		startEditing(combinedId);
	}
	
	public void onDeleteClick(View view) {
		View item = (View) view.getParent();
		ExpandableListView list = getExpandableListView();
		
		int flatPosition = list.getPositionForView(item);
		long packedPosition = list.getExpandableListPosition(flatPosition);
		int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
		long groupId = adapter.getGroupId(groupPosition);
		
		long id;
		Uri uri;
		switch (ExpandableListView.getPackedPositionType(packedPosition)) {
			case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
				int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
				long childId = adapter.getChildId(groupPosition, childPosition);
				id = adapter.getCombinedChildId(groupId, childId);
				uri = ContentUris.withAppendedId(CategoryContract.CONTENT_URI, childId);
				break;
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				id = adapter.getCombinedGroupId(groupId);
				uri = ContentUris.withAppendedId(SupercategoryContract.CONTENT_URI, groupId);
				break;
			default:
				Log.e(TAG, "Illegal packed position when trying to delete");
				return;
		}
		
		// If trying to delete the currently editing row, stop editing first
		if (adapter.getEditingCombinedId() == id) {
			stopEditing();
		}
		
		new AsyncQueryHandler(getContentResolver()) {
		}.startDelete(0, null, uri, null, null);
	}
	
	public long getPackedPositionForId(long id) {
		ExpandableListView list = getExpandableListView();
		for (int position = list.getFirstVisiblePosition(); position <= list.getLastVisiblePosition(); position++) {
			if (list.getItemIdAtPosition(position) == id) {
				return list.getExpandableListPosition(position);
			}
		}
		return ExpandableListView.PACKED_POSITION_VALUE_NULL;
	}	
}
