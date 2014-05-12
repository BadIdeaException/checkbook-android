package heger.christian.checkbook.ui.rules;

import heger.christian.checkbook.OutOfKeysReaction;
import heger.christian.checkbook.R;
import heger.christian.checkbook.OutOfKeysReaction.KeyRequestResultListener;
import heger.christian.checkbook.accounts.Authenticator;
import heger.christian.checkbook.providers.CategoryContract;
import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.providers.OutOfKeysException;
import heger.christian.checkbook.providers.RuleContract;
import heger.christian.checkbook.ui.rules.EditRuleDialog.EditRuleDialogListener;
import heger.christian.checkbook.util.SturdyAsyncQueryHandler;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.LayoutTransition;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class RulesActivity extends ListActivity implements LoaderCallbacks<Cursor> {
	private class AddRuleDialogListener implements EditRuleDialogListener {
		@Override
		public void onClose(String caption, long category) {
			final Uri uri = RuleContract.CONTENT_URI;
			final ContentValues values = new ContentValues();
			values.put(RuleContract.COL_NAME_ANTECEDENT, caption);
			values.put(RuleContract.COL_NAME_CONSEQUENT, category);
			new SturdyAsyncQueryHandler(getContentResolver()) {
				@Override
				public void onError(int token, Object cookie, RuntimeException error) {
					if (error instanceof OutOfKeysException) {
						OutOfKeysReaction handler = OutOfKeysReaction.newInstance(RulesActivity.this);
						handler.setResultListener(new KeyRequestResultListener() {
							@Override
							public void onSuccess() {
								// New keys are available, try again
								startInsert(0, null, uri, values);
							}
							@Override
							public void onFailure() {}
						});
						handler.handleOutOfKeys();
					}
				}
			}.startInsert(0, null, uri, values);
		}
	}
	private class ModifyRuleDialogListener implements EditRuleDialogListener {
		private final long id;
		private final String caption;
		private final long category;
		public ModifyRuleDialogListener(long id, String caption, long category) {
			this.id = id;
			this.caption = caption;
			this.category = category;
		}
		@Override
		public void onClose(String caption, long category) {
			Uri uri = ContentUris.withAppendedId(RuleContract.CONTENT_URI, id);

			ContentValues values = null;
			if (!caption.equals(this.caption)) {
				values = new ContentValues();
				values.put(RuleContract.COL_NAME_ANTECEDENT, caption);
			}
			if (category != this.category) {
				if (values == null) values = new ContentValues();
				values.put(RuleContract.COL_NAME_CONSEQUENT, category);
			}
			if (values != null)
				new AsyncQueryHandler(getContentResolver()) {}.startUpdate(0, null, uri, values, null, null);
		}
	}

	private class RuleViewBinder implements ViewBinder {
		private class RuleColumns {
			private int caption;
			private int category;
			public void indexColumns(Cursor sample) {
				caption = sample.getColumnIndex(RuleContract.COL_NAME_ANTECEDENT);
				category = sample.getColumnIndex(RuleContract.COL_NAME_CONSEQUENT);
			}
		}
		private class CategoryColumns {
			private int id;
			private int caption;
			public void indexColumns(Cursor sample) {
				id = sample.getColumnIndex(CategoryContract._ID);
				caption = sample.getColumnIndex(CategoryContract.COL_NAME_CAPTION);
			}
		}
		private RuleColumns ruleColumns;
		private CategoryColumns categoryColumns;
		private Cursor categories;
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (ruleColumns == null) {
				ruleColumns = new RuleColumns();
				ruleColumns.indexColumns(cursor);
			}
			if (columnIndex == ruleColumns.caption) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			}
			if (columnIndex == ruleColumns.category && categories != null) {
				categories.moveToPosition(-1);
				while (categories.moveToNext())
					if (categories.getLong(categoryColumns.id) == cursor.getLong(ruleColumns.category)) {
						((TextView) view).setText(categories.getString(categoryColumns.caption));
						return true;
					}
			}
			return false;
		}

		public void setCategories(Cursor categories) {
			if (categories != this.categories) {
				this.categories = categories;
				if (categories != null) {
					categoryColumns = new CategoryColumns();
					categoryColumns.indexColumns(categories);
				} else
					categoryColumns = null;
			}
		}
	}

	private static final String LOG_TAG = RulesActivity.class.getSimpleName();

	private static final int LOADER_RULES = 1;
	private static final int LOADER_CATEGORIES = 2;

	private static final String STATE_EDITING_ID = "editing_id";
	private static final String STATE_EDITING_VALUE_CAPTION = "editing_value_caption";
	private static final String STATE_EDITING_VALUE_CATEGORY = "editing_value_category";

	private static final String EDIT_DIALOG_TAG = EditRuleDialog.class.getCanonicalName();

	private SimpleCursorAdapter adapter;
	private RuleViewBinder viewBinder;
	private MenuItem menuAdd;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rules, menu);
		menuAdd = menu.findItem(R.id.menu_add);
		onDataChanged();
		return true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Show the Up button in the action bar.
		setupActionBar();

		getLoaderManager().initLoader(LOADER_CATEGORIES, null, this);

		adapter = new SimpleCursorAdapter(this,
				R.layout.listitem_rules,
				null,
				new String[] { RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_CONSEQUENT },
				new int[] { R.id.txt_caption, R.id.txt_category }, 0) {
			@Override
			public long getItemId(int position) {
				Cursor cursor = getCursor();
				cursor.moveToPosition(position);
				return cursor.getLong(cursor.getColumnIndex(RuleContract._ID));
			}
		};
		viewBinder = new RuleViewBinder();
		adapter.setViewBinder(viewBinder);

		setListAdapter(adapter);
		getLoaderManager().initLoader(LOADER_RULES, null, this);
		ListView list = getListView();
		list.setItemsCanFocus(true);
		list.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		list.setLayoutTransition(new LayoutTransition());

		// Re-created activity while a category was being edited.
		// Reattach dialog listener
		if (savedInstanceState != null) {
			EditRuleDialog dialog = (EditRuleDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
			if (dialog != null) {
				dialog.setCategories(viewBinder.categories);
				EditRuleDialogListener listener;
				if (savedInstanceState.containsKey(STATE_EDITING_ID)) {
					// Create listener for modifying an existing rule
					String caption = savedInstanceState.getString(STATE_EDITING_VALUE_CAPTION);
					if (caption == null) caption = "";
					long category = savedInstanceState.getLong(STATE_EDITING_VALUE_CATEGORY);
					listener = new ModifyRuleDialogListener(savedInstanceState.getLong(STATE_EDITING_ID), caption, category);
				} else {
					// Create listener for a new rule
					listener = new AddRuleDialogListener();
				}
				dialog.setDialogListener(listener);
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
		// If currently editing an existing rule, store edited id, old caption and old category
		EditRuleDialog dialog = (EditRuleDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
		if (dialog != null) {
			EditRuleDialogListener listener = dialog.getDialogListener();
			// If modifying existing rule, save parameters necessary to recreate
			if (listener instanceof ModifyRuleDialogListener) {
				state.putLong(STATE_EDITING_ID, ((ModifyRuleDialogListener) listener).id);
				state.putString(STATE_EDITING_VALUE_CAPTION, ((ModifyRuleDialogListener) listener).caption);
				state.putLong(STATE_EDITING_VALUE_CATEGORY, ((ModifyRuleDialogListener) listener).category);
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
			case LOADER_RULES:
				return new CursorLoader(this, RuleContract.CONTENT_URI, null, null, null, null);
			case LOADER_CATEGORIES:
				return new CursorLoader(this, CategoryContract.CONTENT_URI, null, null, null, null);
			default:
				return null;
		}
	}

	private void onDataChanged() {
		boolean hasCategories = viewBinder.categories != null && viewBinder.categories.getCount() > 0;
		boolean hasRules = adapter.getCount() > 0;
		ListView list = getListView();
		if (!hasCategories && !hasRules) {
			// If no categories exist in the db, show a special view
			list.setEmptyView(new FixedViewFactory(this).createNoCategoriesView());
		} else if (!hasRules) {
			list.setEmptyView(new FixedViewFactory(this).createEmptyView());
		}
		if (menuAdd != null) // May not have been inflated yet if the loaders were really quick
			menuAdd.setEnabled(hasCategories);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
		ListView list = getListView();
		switch (loader.getId()) {
			case LOADER_RULES:
				adapter.swapCursor(data);
				break;
			case LOADER_CATEGORIES:
				viewBinder.setCategories(data);
				// If currently showing a dialog, update its categories
				EditRuleDialog dialog = (EditRuleDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
				if (dialog != null) {
					dialog.setCategories(data);
				}
				// Update all visible rule views
				for (int i = list.getFirstVisiblePosition(); i <= list.getLastVisiblePosition(); i++)
					adapter.bindView(list.getChildAt(i), this, adapter.getCursor());
				break;
		}
		onDataChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
			case LOADER_RULES:
				adapter.swapCursor(null);
				break;
			case LOADER_CATEGORIES:
				viewBinder.setCategories(null);
				EditRuleDialog dialog = (EditRuleDialog) getFragmentManager().findFragmentByTag(EDIT_DIALOG_TAG);
				if (dialog != null) {
					dialog.setCategories(null);
				}
				break;
		}
	}

	public void onEditClick(View view) {
		ListView list = getListView();
		int position = list.getPositionForView(view);
		position -= list.getHeaderViewsCount();
		long id = adapter.getItemId(position);

		Cursor cursor = (Cursor) adapter.getItem(position);
		String caption = cursor.getString(cursor.getColumnIndex(RuleContract.COL_NAME_ANTECEDENT));
		long category = cursor.getLong(cursor.getColumnIndex(RuleContract.COL_NAME_CONSEQUENT));

		EditRuleDialog dialog = EditRuleDialog.newInstance(caption, category, viewBinder.categories);
		dialog.setDialogListener(new ModifyRuleDialogListener(id, caption, category));
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
	}

	public void onDeleteClick(View view) {
		View item = (View) view.getParent().getParent();
		ListView list = getListView();
		long id = list.getItemIdAtPosition(list.getPositionForView(item));
		// Break off if view was off screen or couldn't be found. (This should really never happen)
		if (id == ListView.INVALID_ROW_ID) {
			Log.e(LOG_TAG, "Couldn't find id for item requested for deletion");
			return;
		}

		Uri uri = ContentUris.withAppendedId(RuleContract.CONTENT_URI, id);
		DialogFragment dialog = ConfirmDeleteDialog.newInstance(uri);
		dialog.show(getFragmentManager(), ConfirmDeleteDialog.class.toString());
	}

	public boolean onAddClick(MenuItem menu) {
		// Per default, select first category available
		viewBinder.categories.moveToFirst();
		long category = viewBinder.categories.getLong(viewBinder.categories.getColumnIndex(CategoryContract._ID));

		EditRuleDialog dialog = EditRuleDialog.newInstance("", category, viewBinder.categories);
		dialog.setDialogListener(new AddRuleDialogListener());
		dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);

		Account account = AccountManager.get(this).getAccountsByType(Authenticator.ACCOUNT_TYPE)[1];
		ContentResolver.requestSync(account, CheckbookContentProvider.AUTHORITY, new Bundle());

		return true;
	}
}
