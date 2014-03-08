package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.RulesAdapter;
import heger.christian.ledger.control.RuleEditorProxy;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.RulesContract;
import heger.christian.ledger.ui.categories.EditRuleDialog;
import heger.christian.ledger.ui.categories.EditRuleDialog.EditRuleDialogListener;
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
	private class EditDialogListener implements EditRuleDialogListener {
		private final long id;
		private final String caption;
		private final long category;

		private EditDialogListener(long id, String caption, long category) {
			this.id = id;
			this.caption = caption;
			this.category = category;
		}

		@Override
		public void onClose(String caption, long category) {
			Uri uri = ContentUris.withAppendedId(RulesContract.CONTENT_URI, id);
			ContentValues values = null;
			if (!caption.equals(this.caption)) {
				values = new ContentValues();
				values.put(RulesContract.COL_NAME_ANTECEDENT, caption);
			}
			if (category != this.category) {
				if (values == null) values = new ContentValues();
				values.put(RulesContract.COL_NAME_CONSEQUENT, category);
			}
			if (values != null)
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, values, null, null);
		}
	}

	private static final String TAG = RulesActivity.class.getSimpleName();	

	private static final int LOADER_RULES = 1;
	private static final int LOADER_CATEGORIES = 2;

	private static final String STATE_EDITING_ID = "editing_id";
	private static final String STATE_EDITING_VALUE_CAPTION = "editing_value_caption";
	private static final String STATE_EDITING_VALUE_CATEGORY = "editing_value_category";

	private static final String EDIT_DIALOG_TAG = EditRuleDialog.class.getCanonicalName();

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
		
		rulesAdapter = new RulesAdapter(this, R.layout.listitem_rules, null, 0);
		rulesAdapter.setCategoryAdapter(categoriesAdapter);
		
		setListAdapter(rulesAdapter);		
		getLoaderManager().initLoader(LOADER_RULES, null, this);
		ListView list = getListView();
		list.setItemsCanFocus(true);	
		list.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				
		// Re-created activity while a category was being edited.
		// Reattach dialog listener
		if (savedInstanceState != null) {
			EditRuleDialog dialog = (EditRuleDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
			if (dialog != null && savedInstanceState.containsKey(STATE_EDITING_ID)) {
				String caption = savedInstanceState.getString(STATE_EDITING_VALUE_CAPTION);
				if (caption == null) caption = "";
				long category = savedInstanceState.getLong(STATE_EDITING_VALUE_CATEGORY);
				dialog.setDialogListener(new EditDialogListener(savedInstanceState.getLong(STATE_EDITING_ID), caption, category));
			}
		}
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
		long id = rulesAdapter.getItemId(position);
		
		Cursor cursor = (Cursor) rulesAdapter.getItem(position);
		String caption = cursor.getString(cursor.getColumnIndex(RulesContract.COL_NAME_ANTECEDENT));
		long category = cursor.getLong(cursor.getColumnIndex(RulesContract.COL_NAME_CONSEQUENT));
		
		EditRuleDialog dialog = EditRuleDialog.newInstance(caption, category, categoriesAdapter);
		dialog.setDialogListener(new EditDialogListener(id, caption, category));
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
	}

	public void onEditClick(View view) {
		ListView list = getListView();
		int position = list.getPositionForView(view);
		edit(position);		
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
		
		Uri uri = ContentUris.withAppendedId(RulesContract.CONTENT_URI, id);		
		DialogFragment dialog = ConfirmDeleteDialog.newInstance(uri);
		dialog.show(getFragmentManager(), ConfirmDeleteDialog.class.toString());
	}
}
