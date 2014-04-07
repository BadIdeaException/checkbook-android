package heger.christian.ledger.ui.entry;

import heger.christian.ledger.OutOfKeysReaction;
import heger.christian.ledger.OutOfKeysReaction.KeyRequestResultListener;
import heger.christian.ledger.R;
import heger.christian.ledger.SturdyAsyncQueryHandler;
import heger.christian.ledger.adapters.FilteringAdapter;
import heger.christian.ledger.control.MonthsElapsedCalculator;
import heger.christian.ledger.control.rules.AsyncRuleMatcher;
import heger.christian.ledger.control.rules.AsyncRuleMatcher.OnRuleMatchingCompleteListener;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.db.SQLDateFormat;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.EntryContract;
import heger.christian.ledger.providers.EntryMetadataContract;
import heger.christian.ledger.providers.MonthContract;
import heger.christian.ledger.providers.OutOfKeysException;
import heger.christian.ledger.textwatchers.CurrencyFormattingTextWatcher;
import heger.christian.ledger.textwatchers.RuleApplicationTextWatcher;
import heger.christian.ledger.ui.CurrencyValueFormatter;
import heger.christian.ledger.ui.categories.CategoriesActivity;
import heger.christian.ledger.ui.spreadsheet.SpreadsheetActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class EntryActivity extends Activity implements OnRuleMatchingCompleteListener, LoaderCallbacks<Cursor> {
	private EditText editCaption;
	private Switch switchEarningExpense;
	private EditText editValue;
	private Spinner spinCategory;
	private Button btnDate;
	private Button btnTime;
	private EditText editDetails;
	private TextView txtSaveHint;
	private Button btnSave;

	/**
	 * The adapter for the categories spinner
	 */
	private CursorAdapter adapter;

	/**
	 * Holds the currently set date/time for the new entry.
	 */
	private Calendar calendar;

	private boolean userChosenCategory = false;
	/* Guard flag to suppress spinner's OnItemSelectedListener from processing code-generated selections */
	private boolean suppressSpinnerListener = false;

	private AsyncRuleMatcher matcher;
	private RuleApplicationTextWatcher ruleApplicationWatcher;

	/**
	 * Attempts to set the category spinner value to category with the passed id, suppressing
	 * the attached spinner listener.
	 * @param id
	 */
	private void setSelectedCategory(long id) {
		// Can't simply use setSelection(entry.category) here since
		// adding and deleting categories might have broken the
		// otherwise consecutive numbering in the category id column
		Cursor cursor = ((SimpleCursorAdapter) spinCategory.getAdapter()).getCursor();
		int colIndex = cursor.getColumnIndex(CategoryContract._ID);
		cursor.moveToPosition(-1);
		while (cursor.moveToNext())
			if (cursor.getLong(colIndex) == id) {
				suppressSpinnerListener = true;
				spinCategory.setSelection(cursor.getPosition());
				break;
			}
	}

	private long getSelectedCategory() {
		Cursor cursor = ((SimpleCursorAdapter) spinCategory.getAdapter()).getCursor();
		cursor.moveToPosition(spinCategory.getSelectedItemPosition());
		return cursor.getLong(cursor.getColumnIndex(CategoryContract._ID));
	}

	private void updateDateTimeButtons() {
		btnDate.setText(DateFormat.getDateFormat(this).format(calendar.getTime()));
		btnTime.setText(DateFormat.getTimeFormat(this).format(calendar.getTime()));
	}

	private void updateSaveButton() {
		// Save button is enabled only if a value has been entered
		// Hint textview will warn (prioritized as below) if
		// 1. no value is entered
		// 2. the value entered is zero
		// 3. caption is empty
		if (editValue.getText().length() == 0) {
			txtSaveHint.setText(R.string.txt_save_hint_no_value);
			btnSave.setEnabled(false);
			return;
		}
		if (CurrencyValueFormatter.parseLenient(editValue.getText().toString()) == 0) {
			txtSaveHint.setText(R.string.txt_save_hint_no_value);
			btnSave.setEnabled(false);
			return;
		}
		if (editCaption.getText().length() == 0) {
			txtSaveHint.setText(R.string.txt_save_hint_no_caption);
			btnSave.setEnabled(true);
			return;
		}
		txtSaveHint.setText("");
		btnSave.setEnabled(true);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putString(EntryContract.COL_NAME_DATETIME, new SQLDateFormat().format(calendar.getTime()));
		state.putBoolean(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY, userChosenCategory);
	}

	// Step 2a of setting up the UI. Load entry asynchronously.
	private void loadEntry(Uri uri) {
		final Bundle bundle = new Bundle();
		AsyncQueryHandler query = new AsyncQueryHandler(getContentResolver()) {
			int count = 2;
			@Override
			public void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor != null && cursor.moveToFirst()) {
					if (token == 0) {
						bundle.putString(EntryContract.COL_NAME_CAPTION, cursor.getString(cursor.getColumnIndex(EntryContract.COL_NAME_CAPTION)));
						bundle.putInt(EntryContract.COL_NAME_VALUE, cursor.getInt(cursor.getColumnIndex(EntryContract.COL_NAME_VALUE)));
						bundle.putLong(EntryContract.COL_NAME_CATEGORY, cursor.getLong(cursor.getColumnIndex(EntryContract.COL_NAME_CATEGORY)));
						bundle.putString(EntryContract.COL_NAME_DATETIME, cursor.getString(cursor.getColumnIndex(EntryContract.COL_NAME_DATETIME)));
						bundle.putString(EntryContract.COL_NAME_DETAILS, cursor.getString(cursor.getColumnIndex(EntryContract.COL_NAME_DETAILS)));
						count--;
					} else if (token == 1) {
						bundle.putBoolean(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY, cursor.getInt(cursor.getColumnIndex(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY)) != 0);
						count--;
					}
				}
				if (count == 0) fillUI(bundle);
			}
		};
		query.startQuery(0, null, uri, null, null, null, null);
		query.startQuery(1, null,
				ContentUris.withAppendedId(EntryMetadataContract.CONTENT_URI, ContentUris.parseId(uri)),
				null, null, null, null);
	}

	protected void fillUI(final Bundle bundle) {
		if (bundle.containsKey(EntryContract.COL_NAME_CAPTION)) {
			// Temporarily remove the RuleApplicationTextWatcher to keep it from
			// changing the category
			editCaption.removeTextChangedListener(ruleApplicationWatcher);
			editCaption.setText(bundle.getString(EntryContract.COL_NAME_CAPTION));
			editCaption.addTextChangedListener(ruleApplicationWatcher);
		}
		if (bundle.containsKey(EntryContract.COL_NAME_VALUE)) {
			switchEarningExpense.setChecked(bundle.getInt(EntryContract.COL_NAME_VALUE) > 0);
			editValue.setText(String.valueOf(bundle.getInt(EntryContract.COL_NAME_VALUE)));
		}
		if (bundle.containsKey(EntryContract.COL_NAME_DATETIME)) {
			try {
				calendar.setTime(new SQLDateFormat().parse(bundle.getString(EntryContract.COL_NAME_DATETIME)));
			} catch (ParseException x) {
				Log.e(EntryActivity.this.getClass().getCanonicalName(), "Parsing the date/time string of the entry failed. Setting entry date/time to current time.", x);
				new DialogFragment() {
					@Override
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(R.string.dlg_parsing_time_failed).setPositiveButton(android.R.string.ok, null);
						return builder.create();
					}
				}.show(getFragmentManager(), getClass().getCanonicalName() + ".ParseFailureDialog");
				calendar.setTimeInMillis(System.currentTimeMillis());
			}
			updateDateTimeButtons();
		}
		if (bundle.containsKey(EntryContract.COL_NAME_DETAILS))
			editDetails.setText(bundle.getString(EntryContract.COL_NAME_DETAILS));
		if (bundle.containsKey(EntryContract.COL_NAME_CATEGORY)) {
			// If categories are already loaded, set selection, otherwise defer call
			if (adapter.getCursor() != null) {
				setSelectedCategory(bundle.getLong(EntryContract.COL_NAME_CATEGORY));
			} else {
				spinCategory.post(new Runnable() {
					@Override
					public void run() {
						if (adapter.getCursor() != null) {
							setSelectedCategory(bundle.getLong(EntryContract.COL_NAME_CATEGORY));
						} else
							spinCategory.post(this);
					}
				});
			}
		}
		if (bundle.containsKey(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY))
			userChosenCategory = bundle.getBoolean(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY, true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.actvy_entry);

		editCaption = (EditText) findViewById(R.id.edit_caption);
		editValue = (EditText) findViewById(R.id.edit_value);
		switchEarningExpense = (Switch) findViewById(R.id.switch_earning_expense);
		spinCategory = (Spinner) findViewById(R.id.spin_category);
		btnDate = (Button) findViewById(R.id.btn_date);
		btnTime = (Button) findViewById(R.id.btn_time);
		editDetails = (EditText) findViewById(R.id.edit_details);
		txtSaveHint = (TextView) findViewById(R.id.txt_save_hint);
		btnSave = (Button) findViewById(R.id.btn_save);

		adapter = new SimpleCursorAdapter(EntryActivity.this,
				android.R.layout.simple_spinner_item,
				null,
				new String[] { CategoryContract.COL_NAME_CAPTION } ,
				new int[] { android.R.id.text1 },
				0) {
			@Override
			public long getItemId(int position) {
				Cursor cursor = getCursor();
				cursor.moveToPosition(position);
				return cursor.getLong(cursor.getColumnIndex(CategoryContract._ID));
			}
		};
		spinCategory.setAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);

		// Attach text watcher to caption edit to toggle enabled state of the save button
		editCaption.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateSaveButton();
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});

		// Attach text watcher to value edit to toggle enabled state of the save button
		editValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateSaveButton();
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		// Make the value edit field accept digits as well as the currency symbol, the monetary decimal separator,
		// the grouping separator and breaking and non-breaking spaces
		// Otherwise it will reject the formatting made by the CurrencyFormattingTextWatcher
		editValue.setKeyListener(DigitsKeyListener.getInstance("0123456789" +
				((DecimalFormat) NumberFormat.getCurrencyInstance()).getDecimalFormatSymbols().getCurrencySymbol() +
				((DecimalFormat) NumberFormat.getCurrencyInstance()).getDecimalFormatSymbols().getMonetaryDecimalSeparator() +
				((DecimalFormat) NumberFormat.getCurrencyInstance()).getDecimalFormatSymbols().getGroupingSeparator() +
				" \u00A0"));

		// Attach text watcher to format entered values into currency output on the fly
		editValue.addTextChangedListener(new CurrencyFormattingTextWatcher());

		ruleApplicationWatcher = new RuleApplicationTextWatcher();
		matcher = new AsyncRuleMatcher(getContentResolver());
		matcher.setOnRuleMatchingCompleteListener(this);
		ruleApplicationWatcher.setRuleMatcher(matcher);
		editCaption.addTextChangedListener(ruleApplicationWatcher);

		spinCategory.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (!suppressSpinnerListener)
					userChosenCategory = true;
				suppressSpinnerListener = false;
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		calendar = Calendar.getInstance();
		updateDateTimeButtons();
		if (savedInstanceState == null && getIntent().getData() != null) {
			// Read entry from storage if URI present
			loadEntry(getIntent().getData());
		} else if (savedInstanceState == null
				&& getIntent().getData() == null
				&& getIntent().getExtras() != null
				&& !getIntent().getExtras().isEmpty()) {
			// Make entry from extras, if present
			fillUI(getIntent().getExtras());
		} else if (savedInstanceState != null) {
			// Make entry from saved state, if present
			fillUI(savedInstanceState);
		}

		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
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

	public void onCancelClick(View v) {
		NavUtils.navigateUpFromSameTask(this);
	}

	private int getValue() {
		int sign = switchEarningExpense.isChecked() ? +1 : -1;
		return sign * CurrencyValueFormatter.parseLenient(editValue.getText().toString());
	}

	public void onSaveClick(View v) {
		final ContentValues entryValues = new ContentValues();
		entryValues.put(EntryContract.COL_NAME_CAPTION, editCaption.getText().toString());
		entryValues.put(EntryContract.COL_NAME_DETAILS, editDetails.getText().toString());
		entryValues.put(EntryContract.COL_NAME_DATETIME, new SQLDateFormat().format(calendar.getTime()));
		entryValues.put(EntryContract.COL_NAME_VALUE, getValue());
		// Store the selected category:
		entryValues.put(EntryContract.COL_NAME_CATEGORY, getSelectedCategory());

		final ContentValues entryMetadataValues = new ContentValues();
		entryMetadataValues.put(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY, userChosenCategory);

		// Prepare an intent for going back to the spreadsheet to the month we've just inserted to
		final Intent intent = new Intent(Intent.ACTION_VIEW,
				ContentUris.withAppendedId(MonthContract.CONTENT_URI, MonthsElapsedCalculator.getMonthsElapsed(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))),
				this,
				SpreadsheetActivity.class);

		Uri uri = getIntent().getData();
		if (uri != null) {
			// URI present - this is an update on an existing entry
			AsyncQueryHandler query = new AsyncQueryHandler(getContentResolver()) {
			};
			query.startUpdate(0, null, uri, entryValues, null, null);
			Uri metadataURI = ContentUris.withAppendedId(EntryMetadataContract.CONTENT_URI, ContentUris.parseId(uri));
			query.startUpdate(0, null, metadataURI, entryMetadataValues, null, null);
			// Now that both updates are started, go to the activity
			startActivity(intent);
		} else {
			// Otherwise do an insert of a new entry
			new SturdyAsyncQueryHandler(getContentResolver()) {
				@Override
				public void onInsertComplete(int token, Object cookie, Uri uri) {
					// Set metadata primary key to be the same as that of the just inserted entry
					entryMetadataValues.put(EntryMetadataContract._ID, ContentUris.parseId(uri));
					new AsyncQueryHandler(getContentResolver()) {
					}.startInsert(0, null, EntryMetadataContract.CONTENT_URI, entryMetadataValues);
					// Since entry was inserted okay, it's safe to start the activity now
					startActivity(intent);
				}
				@Override
				public void onError(int token, Object cookie, RuntimeException error) {
					if (error instanceof OutOfKeysException) {
						OutOfKeysReaction handler = OutOfKeysReaction.newInstance(EntryActivity.this);
						handler.setResultListener(new KeyRequestResultListener() {
							@Override
							public void onSuccess() {
								// New keys are available, try again
								startInsert(0, null, EntryContract.CONTENT_URI, entryValues);
							}
							@Override
							public void onFailure() {}
						});
						handler.handleOutOfKeys();
					}
				}
			}.startInsert(0, null, EntryContract.CONTENT_URI, entryValues);
		}
	}

	public void onDateClick(View v) {
		DatePickerDialog dlg = new DatePickerDialog(this, new OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker picker, int year, int month, int day) {
				calendar.set(year, month, day);
				updateDateTimeButtons();
			}
		}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		dlg.show();
	}

	public void onTimeClick(View v) {
		TimePickerDialog dlg = new TimePickerDialog(this, new OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker picker, int hour, int minute) {
				calendar.set(Calendar.HOUR, hour);
				calendar.set(Calendar.MINUTE, minute);
				updateDateTimeButtons();
			}
		},
		calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(this));
		dlg.show();
	}

	@Override
	public void onRuleMatchingComplete(final Map<Integer,Integer> matchResult) {
		String matches = "[ ";
		for (Map.Entry<Integer, Integer> m: matchResult.entrySet()) {
			matches += "(" + m.getKey() + "," + m.getValue() + ") ";
		}
		matches += " ]";
		Log.i("Rule Matching", "Rules were matched against caption, matches: " + matches);
		Log.i("Rule Matching", "" + userChosenCategory);

		// Do not override user choices
		if (userChosenCategory)
			return;

		// Do not change the category if the currently selected one is in the match result set
		if (matchResult.containsKey(getSelectedCategory()))
			return;

		// If the result contains only one match, that means only one category follows from the
		// caption. Set the spinner to that category and display a toast to the user
		if (matchResult.size() == 1) {
			// Set spinner
			int categoryID = matchResult.keySet().iterator().next();
			setSelectedCategory(categoryID);
			userChosenCategory = false;

			// Prepare toast
			Cursor cursor = ((SimpleCursorAdapter) spinCategory.getAdapter()).getCursor();
			cursor.moveToPosition(spinCategory.getSelectedItemPosition());
			String toastString = String.format(getResources().getString(R.string.toast_category_set),
					cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION)));
			Toast toast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
			toast.show();
		}

		// There were multiple matches. Let the user choose one of them.
		if (matchResult.size() > 1) {
			// Filter from the categories cursor all the categories that are in the match result
			class ResultAdapter extends FilteringAdapter {
				CursorAccessHelper helper;
				public ResultAdapter(Adapter adapter) {
					super(adapter);
					helper = new CursorAccessHelper(((CursorAdapter) getAdapter()).getCursor());
				}
				@Override
				public boolean filter(int position) {
					helper.getCursor().moveToPosition(position);
					return matchResult.containsKey(helper.getInt(CategoryContract._ID));
				}
				@Override
				public long getItemId(int position) {
					helper.getCursor().moveToPosition(getInternalPosition(position));
					return helper.getInt(CategoryContract._ID);
				}
			}
			// Create cursor adapter for the selection list that is backed by the spinner's cursor
			SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this,
					android.R.layout.simple_list_item_1,
					((CursorAdapter) spinCategory.getAdapter()).getCursor(),
					new String[] { CategoryContract.COL_NAME_CAPTION },
					new int[] { android.R.id.text1 }, 0);
			// Filter that adapter
			final ResultAdapter adapter = new ResultAdapter(cursorAdapter);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setAdapter(adapter, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setSelectedCategory((int) adapter.getItemId(which));
					userChosenCategory = true;
				}
			});
			builder.setTitle(R.string.choose_category);
			builder.create().show();
		}
	}

	public void onGuessCategoryClick(View view) {
		userChosenCategory = false;
		matcher.matchStrict(editCaption.getText().toString());
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (id == 0)
			return new CursorLoader(this, CategoryContract.CONTENT_URI, null, null, null, null);
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

			// If the database contains no categories yet, this whole exercise will not make much sense, because
			// the entry could never be inserted into the database without a category.
			// Therefore, test for this case and present the user with an option to go directly to
			// the category management screen if the cursor is empty
			if (data.getCount() == 0) {
				final DialogFragment dialog = new DialogFragment() {
					@Override
					public Dialog onCreateDialog(Bundle savedInstance) {
						return new AlertDialog.Builder(EntryActivity.this)
						.setMessage(R.string.dlg_no_categories_message)
						.setPositiveButton(R.string.yes, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW,
										null,
										EntryActivity.this, CategoriesActivity.class);
								startActivity(intent);
							}
						}).setNegativeButton(R.string.no, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								onCancelClick(null);
							}
						}).create();
					}
				};
				// Can't call dialog.show directly here, it will result in an IllegalStateException.
				// The reason is that the Loader callback might fire when the activity has already
				// saved its state and is en route to be stopped/destroyed.
				// Posting to the UI thread ensures that the dialog fragment is shown even if the
				// loader finishes while the activity is not visible.
				spinCategory.post(new Runnable() {
					@Override
					public void run() {
						dialog.show(getFragmentManager(), getClass().getCanonicalName() + ".NoCategoriesDialog");
					}
				});
			} else {
				adapter.swapCursor(data);
			}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == 0)
			((CursorAdapter) spinCategory.getAdapter()).swapCursor(null);
	}
}