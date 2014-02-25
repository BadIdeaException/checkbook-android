package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.RulesAdapter;
import heger.christian.ledger.control.RuleEditorProxy;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.RulesContract;
import android.app.ActionBar;
import android.app.DialogFragment;
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
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class RulesActivity extends ListActivity implements LoaderCallbacks<Cursor> {	
	private static final String TAG = "RulesActivity";	

	private static final int ACTION_BAR_DEFAULT = 
			ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME| ActionBar.DISPLAY_SHOW_TITLE;

	private static final int LOADER_RULES = 1;
	private static final int LOADER_CATEGORIES = 2;

	private static final String STATE_EDITING_ID = "editing_id";
	private static final String STATE_EDITING_VALUE_CAPTION = "editing_value_caption";
	private static final String STATE_EDITING_VALUE_CATEGORY = "editing_value_category";

	private RulesAdapter rulesAdapter;
	CursorAdapter categoriesAdapter;

	private RuleEditorProxy proxy;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Show the Up button in the action bar.
		setupActionBar();

		categoriesAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item,
				null,
				new String[] { CategoryContract.COL_NAME_CAPTION },
				new int[] { android.R.id.text1 }, 0) {
			@Override
			public long getItemId(int position) {
				Cursor cursor = getCursor();
				cursor.moveToPosition(position);
				return CursorAccessHelper.getInt(cursor, CategoryContract._ID);
			}
		}; 		
		getLoaderManager().initLoader(LOADER_CATEGORIES, null, this);
		
		FixedViewFactory factory = new FixedViewFactory(this);
		getListView().addHeaderView(factory.createAddView());
		
		rulesAdapter = new RulesAdapter(this, R.layout.listitem_rules, null, 0);
		rulesAdapter.setCategoryAdapter(categoriesAdapter);
		
		setListAdapter(rulesAdapter);		
		getLoaderManager().initLoader(LOADER_RULES, null, this);
		ListView list = getListView();
		list.setItemsCanFocus(true);	
		list.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				
		// Re-created activity while a row was being edited.
		// Restore editing state
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_EDITING_ID)) {
			long id = savedInstanceState.getLong(STATE_EDITING_ID);			
			proxy = new RuleEditorProxy(list, rulesAdapter, id);
			proxy.setInitialValues(savedInstanceState.getString(STATE_EDITING_VALUE_CAPTION), savedInstanceState.getInt(STATE_EDITING_VALUE_CATEGORY));
			startEditing(id);
		}
	}

	@Override
	public void onBackPressed() {
		if (rulesAdapter.getEditingId() == RulesAdapter.NOT_EDITING)
			super.onBackPressed();
		else 
			stopEditing();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (proxy != null && proxy.isAlive()) {
			state.putLong(STATE_EDITING_ID, rulesAdapter.getEditingId());			
			state.putString(STATE_EDITING_VALUE_CAPTION, proxy.getCaption());
			state.putInt(STATE_EDITING_VALUE_CATEGORY, proxy.getCategory());
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
			case LOADER_RULES: 
				return new CursorLoader(this, RulesContract.CONTENT_URI, null, null, null, null);				
			case LOADER_CATEGORIES:
				return new CursorLoader(this, CategoryContract.CONTENT_URI, null, null, null, null);
			default:
				return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
		switch (loader.getId()) {
			case LOADER_RULES:				
				rulesAdapter.swapCursor(data);
				if (data.getCount() == 0)
					getListView().setEmptyView(new FixedViewFactory(this).createEmptyView());
				break;
			case LOADER_CATEGORIES:
				categoriesAdapter.swapCursor(data);
				if (data.getCount() == 0) 
					// If no categories exist in the db, show a special view
					getListView().setEmptyView(new FixedViewFactory(this).createNoCategoriesView());				
				break;
		}		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
			case LOADER_RULES:
				rulesAdapter.swapCursor(null);
				break;
			case LOADER_CATEGORIES:
				categoriesAdapter.swapCursor(null);
				rulesAdapter.setCategoryAdapter(null);
				break;
		}		
	}	

	public void startEditing(final long id) {
		ListView list = getListView();		
		long prevId = rulesAdapter.getEditingId();
		rulesAdapter.setEditingId(id);
		
		// Update view for the previous editing position
		View v = getItemForId(prevId);
		if (v != null) {
			rulesAdapter.getView(rulesAdapter.getItemPosition(prevId), v, list);
		}
		// Update view for new editing position, if any
		if (id != RulesAdapter.NOT_EDITING) {			
			v = getItemForId(id);
			if (v != null) {
				rulesAdapter.getView(rulesAdapter.getItemPosition(id), v, list);
			}
			
			// If there is an alive proxy for this id already, reuse the existing proxy. 
			// (This is the case if the user clicks the edit button for the same item he's
			// already editing, or when recreating from a configuration change)
			if (!(proxy != null && proxy.isAlive() && proxy.getId() == id)) {
				if (proxy != null) 
					// Close an existing proxy if necessary
					proxy.close();					
				// Create new proxy for this edit
				proxy = new RuleEditorProxy(list, rulesAdapter, id);
			}
			
			// Set action bar to display custom action mode containing "Done" and "Cancel" buttons
			getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ACTION_BAR_DEFAULT | ActionBar.DISPLAY_SHOW_CUSTOM);
			getActionBar().setCustomView(R.layout.edit_actionbar);

			
		}
	}
	
	public void stopEditing() {
		try {
			long prevId = rulesAdapter.getEditingId(); 

			rulesAdapter.setEditingId(RulesAdapter.NOT_EDITING);
			ListView list = getListView();
			// Update view for the previous editing position
			// Is the item currently visible?
			View v = getItemForId(prevId);
			if (v != null) {
				rulesAdapter.getView(rulesAdapter.getItemPosition(prevId), v, list);
			}
			getActionBar().setDisplayOptions(ACTION_BAR_DEFAULT);
		} finally {
			proxy.close();		
		}
	}

	public void onEditClick(View view) {
		ViewGroup item = (ViewGroup) view.getParent().getParent();				
		ListView list = getListView();
		long id = list.getItemIdAtPosition(list.getPositionForView(item));
		startEditing(id);
	}

	public void onDeleteClick(View view) {
		View item = (View) view.getParent().getParent();
		ListView list = getListView();
		long id = list.getItemIdAtPosition(list.getPositionForView(item));
		// Break off if view was off screen or couldn't be found. (This should really never happen)
		if (id == ListView.INVALID_ROW_ID) {
			Log.e(TAG, "Couldn't find id for item requested for deletion");
			return;
		}
		if (id == rulesAdapter.getEditingId()) 
			stopEditing();
		
		Uri uri = ContentUris.withAppendedId(RulesContract.CONTENT_URI, id);		
		DialogFragment dialog = ConfirmDeleteDialog.newInstance(uri);
		dialog.show(getFragmentManager(), ConfirmDeleteDialog.class.toString());
	}

	public void onCustomActionModeClick(View view) {
		switch (view.getId()) {
			case R.id.action_done:				
				// Store values in the database
				Uri uri = ContentUris.withAppendedId(RulesContract.CONTENT_URI, rulesAdapter.getEditingId());
				ContentValues content = new ContentValues(1);
				content.put(RulesContract.COL_NAME_ANTECEDENT, proxy.getCaption());
				content.put(RulesContract.COL_NAME_CONSEQUENT, proxy.getCategory());
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, content, null, null);
				//$FALL-THROUGH$
			case R.id.action_cancel:				
				// Put the row out of editing mode
				stopEditing();
		}
		
	}

	private View getItemForId(long id) {
		ListView list = getListView();
		// Shortcut to avoid the loop 
		if (id == ListView.INVALID_ROW_ID)
			return null;
		
		for (int position = list.getFirstVisiblePosition(); position <= list.getLastVisiblePosition(); position++) 
			if (list.getItemIdAtPosition(position) == id) {
				position -= list.getFirstVisiblePosition();
				return list.getChildAt(position);
				
			}
		return null; 
	}
}
