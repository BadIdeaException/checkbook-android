package heger.christian.ledger.ui.entry;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.FilteringAdapter;
import heger.christian.ledger.control.MonthsElapsedCalculator;
import heger.christian.ledger.control.rules.AsyncRuleMatcher;
import heger.christian.ledger.control.rules.AsyncRuleMatcher.OnRuleMatchingCompleteListener;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.db.SQLDateFormat;
import heger.christian.ledger.model.EntryParameterObject;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.EntryContract;
import heger.christian.ledger.providers.EntryMetadataContract;
import heger.christian.ledger.providers.LedgerContentProvider;
import heger.christian.ledger.providers.MonthsContract;
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
import java.util.concurrent.CountDownLatch;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class EntryActivity extends Activity implements OnRuleMatchingCompleteListener, LoaderCallbacks<Cursor> {	
	private EditText editCaption;
	private View viewEarningExpense;
	private EditText editValue;
	private Spinner spinCategory;
	private Button btnDate;
	private Button btnTime;
	private EditText editDetails;
	private TextView txtSaveHint;
	private Button btnSave;

	// Latch to allow the AsyncTask responsible for filling the UI to wait for the CursorLoader filling the
	// categories spinner. Without this, there might be problems if the UI filler task attempts to set the
	// category before the spinner has been populated
	CountDownLatch categoriesLatch = new CountDownLatch(1);
	
	/**
	 * Holds the currently set date/time for the new entry. 
	 */
	private Calendar calendar;
	
	private boolean userChosenCategory = false;
	/* Guard flag to suppress spinner's OnItemSelectedListener from processing code-generated selections */
	private boolean suppressSpinnerListener = false;
	
	private AsyncRuleMatcher matcher;
	
	/**
	 * Attempts to set the category spinner value to category with the passed id
	 * @param id
	 */
	private void setSelectedCategory(int id) {
		// Can't simply use setSelection(entry.category) here since
		// adding and deleting categories might have broken the
		// otherwise consecutive numbering in the category id column
		Cursor cursor = ((SimpleCursorAdapter) spinCategory.getAdapter()).getCursor();
		int colIndex = cursor.getColumnIndex(CategoryContract._ID);
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) 
			if (cursor.getInt(colIndex) == id) {
				suppressSpinnerListener = true;
				spinCategory.setSelection(cursor.getPosition());
				break;
			} 		
	}
	
	private int getSelectedCategory() {
		Cursor cursor = ((SimpleCursorAdapter) spinCategory.getAdapter()).getCursor();
		cursor.moveToPosition(spinCategory.getSelectedItemPosition());
		return cursor.getInt(cursor.getColumnIndex(CategoryContract._ID));
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
		state.putSerializable("Calendar", calendar);
	}
	
	/*
	 * Step 1 of setting up the UI. Attach any "passive" listeners here.
	 * ("Passive" meaning they are only doing formatting, not changing data)
	 */
	private void prepareUI() {
		// Attach text watcher to caption edit to toggle enabled state of the save button
		editCaption.addTextChangedListener(new TextWatcher() {			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateSaveButton();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}			
			public void afterTextChanged(Editable s) {}
		});
		
		// Attach text watcher to value edit to toggle enabled state of the save button
		editValue.addTextChangedListener(new TextWatcher() {			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateSaveButton();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}			
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
		loadEntry();
	}
	// Step 2a of setting up the UI. Load entry asynchronously.
	private void loadEntry() {
		new AsyncTask<Intent,Object,Object[]>() {
			FrameLayout overlay;
			@Override
			protected void onPreExecute() {
				// Create a semi-transparent overlay to show the progress bar
				overlay = new FrameLayout(EntryActivity.this);
				
				ProgressBar progressBar = new ProgressBar(EntryActivity.this);				
				overlay.setClickable(true);
				overlay.setBackgroundColor(getResources().getColor(R.color.transparent_white));
				overlay.addView(progressBar);
				FrameLayout.LayoutParams lp = (LayoutParams) progressBar.getLayoutParams();
				lp.width= Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, getResources().getDisplayMetrics()));
				lp.height=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, getResources().getDisplayMetrics()));
				lp.gravity = 0x11;
				EntryActivity.this.addContentView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			}			
			@Override
			protected Object[] doInBackground(Intent... params) {
				if (params.length == 0)
					return null;
				// Will contain [ EntryParameterObject, userChosenCategory ]
				Object[] result = new Object[2];
				result[1] = false;
				
				Intent intent = params[0];
				// Initialize entry to an "empty" entry with datetime "now".
				EntryParameterObject entry = new EntryParameterObject(null, null, null, null, new SQLDateFormat().format(Calendar.getInstance().getTime()), null);
				// Does the intent carry a URI with an ID? If so, retrieve the entry data from the 
				// database
				Uri uri = intent.getData();
				if (uri != null && LedgerContentProvider.URI_MATCHER.match(uri) == LedgerContentProvider.URI_ENTRIES_ID) {
					Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
					Uri metadataURI = ContentUris.withAppendedId(EntryMetadataContract.CONTENT_URI, 
							ContentUris.parseId(uri));
					Cursor entryMetadataCursor = getContentResolver().query(metadataURI, 
							new String[] { EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY } , 
							null, 
							null, 
							null);
					if (entryCursor.moveToFirst()) {
						entry = EntryParameterObject.readFromCursor(entryCursor);
					}
					if (entryMetadataCursor.moveToFirst()) {
						CursorAccessHelper helper = new CursorAccessHelper(entryMetadataCursor);
						result[1] = helper.getInt(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY) != 0;
					}
					entryCursor.close();
					entryMetadataCursor.close();
				}

				// Read any values that might have been passed in the extras and override the entry's values with them
				if (intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					if (extras.containsKey(EntryContract.COL_NAME_CAPTION))
						entry.caption = extras.getString(EntryContract.COL_NAME_CAPTION);
					if (extras.containsKey(EntryContract.COL_NAME_CATEGORY))
						entry.category = extras.getInt(EntryContract.COL_NAME_CATEGORY);
					if (extras.containsKey(EntryContract.COL_NAME_DATETIME))
						entry.datetime = extras.getString(EntryContract.COL_NAME_DATETIME);
					if (extras.containsKey(EntryContract.COL_NAME_DETAILS))
						entry.details = extras.getString(EntryContract.COL_NAME_DETAILS);
					if (extras.containsKey(EntryContract.COL_NAME_VALUE))
						entry.value = extras.getInt(EntryContract.COL_NAME_VALUE);		
				}	
				result[0] = entry;
				// Wait for the category load latch here, since this is the latest possible time we can do it without blocking the 
				// UI thread
				try {
					categoriesLatch.await();
				} catch (InterruptedException e) {
					Log.e("EntryActivity", "Interrupted while retrieving entry values",e);
					cancel(true);
				}
				return result;
			}
			@Override
			protected void onPostExecute(Object[] result) {
				// Remove progress overlay
				((ViewGroup) overlay.getParent()).removeView(overlay);
				
				userChosenCategory = (Boolean) result[1];
				populateUI((EntryParameterObject) result[0]);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getIntent());		
		loadCategories();
	}
	// Step 2b of setting up the UI. Load the categories asynchronously.
	// This runs concurrently to loadEntry().
	private void loadCategories() {
		SimpleCursorAdapter spinnerAdapter = new SimpleCursorAdapter(EntryActivity.this, 
				android.R.layout.simple_spinner_item, 
				null, 
				new String[] { CategoryContract.COL_NAME_CAPTION } , 
				new int[] { android.R.id.text1 }, 
				0);
		spinCategory.setAdapter(spinnerAdapter);
		getLoaderManager().initLoader(0, null, this); 			
	}
	/*
	 * Step 3 of setting up the UI. Fill the views with the values determined by 
	 * loadEntry()
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void populateUI(EntryParameterObject entry) {
		calendar = Calendar.getInstance();
		if (entry.datetime != null && !entry.datetime.isEmpty())
			try {
				calendar.setTime(new SQLDateFormat().parse(entry.datetime));
			} catch (ParseException e) {
				Log.e(EntryActivity.this.getClass().getCanonicalName(), "Parsing the date/time string of the entry failed. Setting entry date/time to current time.", e);
				new DialogFragment() {
					@Override
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(R.string.dlg_parsing_time_failed).setPositiveButton(android.R.string.ok, null);
						return builder.create();
					}
				}.show(getFragmentManager(), "parsing datetime failed");
				calendar.setTimeInMillis(System.currentTimeMillis());
			}
		
		editCaption.setText(entry.caption);
		if (entry.value != null) {
			if (viewEarningExpense instanceof Spinner)
				((Spinner) viewEarningExpense).setSelection(entry.value > 0 ? 0 : 1);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && viewEarningExpense instanceof Switch)
				((Switch) viewEarningExpense).setChecked(entry.value > 0);
			editValue.setText(CurrencyValueFormatter.format(entry.value));
		}
		btnDate.setText(DateFormat.getDateFormat(EntryActivity.this).format(calendar.getTime()));
		btnTime.setText(DateFormat.getTimeFormat(EntryActivity.this).format(calendar.getTime()));
		editDetails.setText(entry.details);

		// Set category spinner to reflect entry category if set.
		if (entry.category != null) {
			setSelectedCategory(entry.category);
		}	
		finishUI();
	}
	/*
	 * Step 4 of setting up the UI. Attach any "active" listeners.
	 * ("Active" meaning they change data, such as the spinner selection listener
	 * (sets userChosenCategory) and the RuleApplicationTextWatcher (initiates
	 * category guessing, potentially changing the category and potentially a
	 * costly operation)
	 */
	private void finishUI() {
		// Attach text watcher to caption to auto-set category based on caption
		RuleApplicationTextWatcher ruleApplicationWatcher = new RuleApplicationTextWatcher();
		matcher = new AsyncRuleMatcher(getContentResolver());
		matcher.setOnRuleMatchingCompleteListener(this);
		ruleApplicationWatcher.setRuleMatcher(matcher);
		editCaption.addTextChangedListener(ruleApplicationWatcher);
		
		spinCategory.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (!suppressSpinnerListener)
					userChosenCategory = true;
				suppressSpinnerListener = false;
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.actvy_entry);

		editCaption = (EditText) findViewById(R.id.edit_caption);
		editValue = (EditText) findViewById(R.id.edit_value);
		viewEarningExpense = findViewById(R.id.view_earning_expense);
		spinCategory = (Spinner) findViewById(R.id.spin_category);
		btnDate = (Button) findViewById(R.id.btn_date);
		btnTime = (Button) findViewById(R.id.btn_time);
		editDetails = (EditText) findViewById(R.id.edit_details);
		txtSaveHint = (TextView) findViewById(R.id.txt_save_hint);
		btnSave = (Button) findViewById(R.id.btn_save);
		
		prepareUI();
		
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
//		getMenuInflater().inflate(R.menu.entry, menu);
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
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void onSaveClick(View v) {
		ContentValues entryValues = new ContentValues();
		entryValues.put(EntryContract.COL_NAME_CAPTION, editCaption.getText().toString());
		entryValues.put(EntryContract.COL_NAME_DETAILS, editDetails.getText().toString());
		entryValues.put(EntryContract.COL_NAME_DATETIME, new SQLDateFormat().format(calendar.getTime()));
		int sign = 1;
		if (viewEarningExpense instanceof Spinner)
			sign = ((Spinner) viewEarningExpense).getSelectedItemPosition() == 0 ? -1 : +1;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && viewEarningExpense instanceof Switch) {
			sign = ((Switch) viewEarningExpense).isChecked() ? +1 : -1;
		}
		entryValues.put(EntryContract.COL_NAME_VALUE, sign * CurrencyValueFormatter.parseLenient(editValue.getText().toString()));
		// Store the selected category:
		entryValues.put(EntryContract.COL_NAME_CATEGORY, getSelectedCategory());
		
		ContentValues entryMetadataValues = new ContentValues();
		entryMetadataValues.put(EntryMetadataContract.COL_NAME_USER_CHOSEN_CATEGORY, userChosenCategory);
		
		Uri uri = getIntent().getData();		
		if (uri != null) {
			// URI present - this is an update on an existing entry
			getContentResolver().update(uri, entryValues, null, null);
			Uri metadataURI = ContentUris.withAppendedId(EntryMetadataContract.CONTENT_URI, 
					ContentUris.parseId(uri));
			getContentResolver().update(metadataURI, entryMetadataValues, null, null);
		} else {
			// Otherwise do an insert of a new entry
			uri = getContentResolver().insert(EntryContract.CONTENT_URI, entryValues);
			// Set metadata primary key to be the same as that of the just inserted entry
			entryMetadataValues.put(EntryMetadataContract._ID, ContentUris.parseId(uri));
			getContentResolver().insert(EntryMetadataContract.CONTENT_URI, entryMetadataValues);
		}		
		// Go back to the spreadsheet, selecting the month we've just inserted to
		Intent intent = new Intent(Intent.ACTION_VIEW, 
				ContentUris.withAppendedId(MonthsContract.CONTENT_URI,
						MonthsElapsedCalculator.getMonthsElapsed(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))),
				this, SpreadsheetActivity.class);
		startActivity(intent);
	}
	
	public void onDateClick(View v) {
		DatePickerDialog dlg = new DatePickerDialog(this, new OnDateSetListener() {					
			public void onDateSet(DatePicker picker, int year, int month, int day) {
				calendar.set(year, month, day);
				btnDate.setText(DateFormat.getDateFormat(EntryActivity.this).format(calendar.getTime()));
			}
		}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		dlg.show();
	}

	public void onTimeClick(View v) {
		TimePickerDialog dlg = new TimePickerDialog(this, new OnTimeSetListener() {
			public void onTimeSet(TimePicker picker, int hour, int minute) {
				calendar.set(Calendar.HOUR, hour);
				calendar.set(Calendar.MINUTE, minute);
				btnTime.setText(DateFormat.getTimeFormat(EntryActivity.this).format(calendar.getTime()));
			}					
		}, 
		calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(this));		
		dlg.show();	
	}
	
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

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (id == 0)
			return new CursorLoader(this, CategoryContract.CONTENT_URI, null, null, null, null);
		return null;
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (loader.getId() == 0) {
			// If the database contains no categories yet, this whole exercise will not make much sense, because
			// the entry could never be inserted into the database without a category.
			// Therefore, test for this case and present the user with an option to go directly to
			// the category management screen if the cursor is empty
			if (data.getCount() == 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(EntryActivity.this);
				builder.setMessage(R.string.dlg_no_categories_message);
				builder.setPositiveButton(R.string.yes, new OnClickListener() {					
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, 
								null, 
								EntryActivity.this, CategoriesActivity.class);
						startActivity(intent);
					}
				}).setNegativeButton(R.string.no, new OnClickListener() {					
					public void onClick(DialogInterface dialog, int which) {
						onCancelClick(null);
					}
				});
				builder.create().show();
			} else 
				((CursorAdapter) spinCategory.getAdapter()).swapCursor(data);	
			// Release the latch so the task filling the UI can set the category spinner's value
			categoriesLatch.countDown();
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == 0)
			((CursorAdapter) spinCategory.getAdapter()).swapCursor(null);
	}
}