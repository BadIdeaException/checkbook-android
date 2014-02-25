package heger.christian.ledger.ui.spreadsheet;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.SpreadsheetAdapter;
import heger.christian.ledger.control.MonthsElapsedCalculator;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.EntryContract;
import heger.christian.ledger.ui.CurrencyValueFormatter;
import heger.christian.ledger.ui.entry.EntryActivity;

import java.text.DateFormatSymbols;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleCursorTreeAdapter.ViewBinder;
import android.widget.TextView;


public class MonthFragment extends Fragment implements LoaderCallbacks<Cursor>, OnChildClickListener {
	private static class RowViewBinder implements ViewBinder {
		private final Context context;
		public RowViewBinder(Context context) {
			this.context = context;
		}
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (cursor.getColumnName(columnIndex).equals(EntryContract.COL_NAME_CAPTION)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			}
			if (cursor.getColumnName(columnIndex).equals(EntryContract.COL_NAME_VALUE)) {
				TextView textview = (TextView) view;
				int value = cursor.getInt(columnIndex);
				textview.setText(CurrencyValueFormatter.format(value));
				if (value < 0) {
					textview.setTextColor(context.getResources().getColor(R.color.red));
				}
				return true;
			}
			return false;
		}
		
	}
	private ExpandableListView listEntries;
	private SpreadsheetAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
		View view = inflater.inflate(R.layout.layout_month_spreadsheet,
				container, false);
		Bundle args = getArguments();
		int year = MonthsElapsedCalculator.getYear(args.getInt(SpreadsheetActivity.ARG_MONTH_ID)); 
		int month = MonthsElapsedCalculator.getMonth(args.getInt(SpreadsheetActivity.ARG_MONTH_ID)); 

		getLoaderManager().initLoader(0, null, this);

		listEntries = (ExpandableListView) view.findViewById(R.id.list_entries);
		adapter = new SpreadsheetAdapter(this.getActivity(), 
				null, 
				R.layout.listitem_entries, 
				new String[] { CategoryContract.COL_NAME_CAPTION }, 
				new int[] { R.id.txt_caption }, 
				R.layout.listitem_entries, 
				new String[] { EntryContract.COL_NAME_CAPTION, EntryContract.COL_NAME_VALUE }, 
				new int[] { R.id.txt_caption, R.id.txt_value },
				month, year);
		adapter.setViewBinder(new RowViewBinder(getActivity()));
		listEntries.setAdapter(adapter);
		listEntries.setOnChildClickListener(this);
		
		// Set the text view for the caption of the spreadsheet to be
		// <month> <year>
		String monthName = DateFormatSymbols.getInstance().getMonths()[month]; 
		((TextView) view.findViewById(R.id.txt_month)).setText(monthName + " " + String.valueOf(year));
		
//		FrameLayout overlay = new FrameLayout(getActivity());
//		
//		ImageView btn = new ImageView(getActivity());
//		btn.setImageResource(R.drawable.action_autofill_new_month);
//		
////		overlay.setBackgroundColor(getResources().getColor(R.color.transparent_white));
//		overlay.addView(btn);
//		btn.setScaleX(0.33f);
//		btn.setScaleY(0.33f);
//		btn.setClickable(true);
//		FrameLayout.LayoutParams lp = (LayoutParams) btn.getLayoutParams();
////		lp.width=100;
////		lp.height=100;
//		lp.gravity = Gravity.CENTER;
//		getActivity().addContentView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		return view;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Cursor cursor = adapter.getChild(groupPosition, childPosition);
		// Get id value for the clicked entry
		long entryId = cursor.getLong(cursor.getColumnIndex(EntryContract._ID));
		Intent intent = new Intent(Intent.ACTION_EDIT, 
				ContentUris.appendId(EntryContract.CONTENT_URI.buildUpon(),entryId).build(), 
				getActivity(), 
				EntryActivity.class);
		startActivity(intent);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this.getActivity(), 
				CategoryContract.CONTENT_URI, 
				null, 
				null, 
				null, 
				CategoryContract._ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {		
		adapter.setGroupCursor(data);		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.setGroupCursor(null);		
	}
	//
//	private TreeEmulator treeEmulator;
//	
//	public static final String ARG_MONTH_ID = "monthID";
//
//	protected class QueryTask extends AsyncTask<Integer, Object, Cursor[]> {
//		@Override
//		protected Cursor[] doInBackground(Integer... params) {
//			Cursor[] cursors = new Cursor[3];
//			// First cursor contains all supercategory rows, 
//			// in order of their id column
//			cursors[0] = getActivity().getContentResolver().query(
//					SupercategoryContract.CONTENT_URI,
//					null, null, null,
//					SupercategoryContract._ID + " ASC");
//			// Second cursor contains all category rows, in order of their 
//			// supercategory columns. Ie first come all the subcategories
//			// of supercategory 1, then of supercategory 2, etc.
//			cursors[1] = getActivity()
//					.getContentResolver()
//					.query(CategoryContract.CONTENT_URI,
//							null,
//							null,
//							null,
//							CategoryContract.COL_NAME_SUPERCATEGORY
//									+ " ASC");
//			// Third cursor contains all entry rows for which the datetime,
//			// when set to the beginning of its associated month, is equal
//			// to the timestring for the beginning of the month/year passed 
//			// params.
//			// The result is ordered by the category column, ie first come 
//			// all the entries matching the above condition belonging to 
//			// category 1, then those of category 2, etc., then by the
//			// datetime.
//			cursors[2] = getActivity()
//					.getContentResolver()
//					.query(EntryContract.CONTENT_URI,
//							new String[] { EntryContract._ID, EntryContract.COL_NAME_CAPTION, EntryContract.COL_NAME_CATEGORY, EntryContract.COL_NAME_DATETIME, EntryContract.COL_NAME_VALUE },
//							"datetime(" 
//									+ EntryContract.COL_NAME_DATETIME
//									+ ",'start of month')=?",
//							new String[] { String.valueOf(params[1]) + "-" + String.format("%02d",params[0]) + "-01 00:00:00" },
//							EntryContract.COL_NAME_CATEGORY + " ASC, " 
//							+ EntryContract.COL_NAME_DATETIME + " ASC");
//			return cursors;
//		}
//
//		@Override
//		protected void onPostExecute(Cursor[] cursors) {
//			// Initialize all cursors to point before their first result rows
//			// This may be unnecessary since freshly returned cursors should 
//			// point there already anyway. However, since this is not guaranteed
//			// in the SDK documentation, we'll do it here just for safe measure
//			Cursor scCursor = cursors[0];
//			scCursor.moveToPosition(-1);
//			Cursor catCursor = cursors[1];
//			catCursor.moveToPosition(-1);
//			Cursor entriesCursor = cursors[2];
//			entriesCursor.moveToPosition(-1);
//			
//			// Create a table row factory
//			AbstractTableRowFactory factory = new TableRowFactory(getActivity());
//			// Find the table layout. In the nested loops, we'll later add rows to it.
//			TableLayout tableLayout = (TableLayout) getView().findViewById(R.id.layout_table);
//			
//			// Initialize the variables to compute the total revenues and expenses to 0
//			int totalRevenues = 0;
//			int totalExpenses = 0;
//			
//			// Iterate over cursor containing the supercategories		
//			while (scCursor.moveToNext()) {
//				SupercategoryParameterObject sc = SupercategoryParameterObject.readFromCursor(scCursor);
//				// Let the factory create a row and add it to the table
//				TableRow scRow = factory.createRow(sc);
//				tableLayout.addView(scRow, tableLayout.getChildCount() - 2);
//				// Set the tag to be the category's content uri (will be used later when 
//				// restoring expansion state during configuration changes (e.g. turning the device)
//				scRow.setTag(ContentUris.appendId(SupercategoryContract.CONTENT_URI.buildUpon(), sc.id).build());			
//				// OnClickListener to handle expanding and collapsing the descendant rows
//				scRow.setOnClickListener(new OnClickListener() {					
//					public void onClick(View v) {
//						TableRow row = (TableRow) v;
//						// Get current expansion state
//						boolean isExpanded = treeEmulator.isExpanded(row);								
//						// If currently expanded, new visibility for all children needs 
//						// to be GONE, otherwise VISIBLE
//						int newVisibility = (isExpanded ? View.GONE : View.VISIBLE);
//						// Reset expansion status of row
//						treeEmulator.setExpansion(row, !isExpanded);
//						for (View child: treeEmulator.getChildren(row)) {
//							child.setVisibility(newVisibility);			
//							for (View grandchild: treeEmulator.getChildren(child))
//								grandchild.setVisibility(newVisibility == View.VISIBLE && treeEmulator.isExpanded(child) ? View.VISIBLE : View.GONE);							
//						}
//					}
//				});
//				int scRevenues = 0;
//				int scExpenses = 0;
//				// Iterate over cursor containing the categories
//				// The loop will be broken off when a row not belonging 
//				// with the currently handled supercategory is encountered 
//				while (catCursor.moveToNext()) {					
//					CategoryParameterObject cat = CategoryParameterObject.readFromCursor(catCursor);
//					// Does the row the cursor is on now still belong to the currently
//					// handled supercategory?
//					if (sc.id == cat.supercategory) {
//						// Let the factory create a row and add it to the table
//						TableRow catRow = factory.createRow(cat);
//						tableLayout.addView(catRow, tableLayout.getChildCount() - 2);
//						// Set the tag to be the category's content uri (will be used later when 
//						// restoring expansion state during configuration changes (e.g. turning the device)
//						catRow.setTag(ContentUris.appendId(CategoryContract.CONTENT_URI.buildUpon(), cat.id).build());
//						// Add the catRow as a child of the current scRow 
//						treeEmulator.addChild(scRow, catRow);
//						// If the parent row is currently collapsed, hide the catRow.
//						// The first time the view is ever created, this will default to 
//						// false, but this ensures expansion state carries over when the
//						// user flips the device
//						if (!treeEmulator.isExpanded(scRow))
//							catRow.setVisibility(View.GONE);
//						// Initialize the newly built row's expansion state to true in the tree emulator 
////						treeEmulator.setExpansion(catRow, false);
//						// OnClickListener to handle expanding and collapsing the category row
//						catRow.setOnClickListener(new OnClickListener() {							
//							public void onClick(View v) {
//								TableRow row = (TableRow) v;
//								// Get current expansion state
//								boolean isExpanded = treeEmulator.isExpanded(row);								
//								// If currently expanded, new visibility for all children needs 
//								// to be GONE, otherwise VISIBLE
//								int newVisibility = (isExpanded ? View.GONE : View.VISIBLE);
//								// Reset expansion status of row
//								treeEmulator.setExpansion(row, !isExpanded);
//								for (View child: treeEmulator.getChildren(row)) {
//									child.setVisibility(newVisibility);								
//								}
//							}
//						});
//						// Initialize the category expenses and revenues to 0
//						// When handling entries belonging to this category, they will add
//						// to these values
//						int catRevenues = 0;
//						int catExpenses = 0;
//						// Iterate over cursor containing the entries
//						// The loop will be broken off when a row not belonging 
//						// with the currently handled category is encountered 
//						while (entriesCursor.moveToNext()) {
//							EntryParameterObject entry = EntryParameterObject.readFromCursor(entriesCursor);
//							// Does the row the cursor is on now still belong to the
//							// currently handled category?
//							if (cat.id == entry.category) {
//								// Let the factory create a row and add it to the table
//								TableRow entryRow = factory.createRow(entry);
//								tableLayout.addView(entryRow, tableLayout.getChildCount() - 2);
//								// Set the row's tag to be the content uri for the current entry
//								// If the value for this entry is positive, add it to the 
//								// category's revenues, otherwise to its expenses
//								entryRow.setTag(ContentUris.appendId(EntryContract.CONTENT_URI.buildUpon(),entry.id).build());
//								// Add the new row as a child of the current catRow
//								treeEmulator.addChild(catRow, entryRow);		
//								// If the parent row, or the supercategory row, is currently collapsed, hide the entryRow.
//								// The first time the view is ever created, this will default to 
//								// false, but this ensures expansion state carries over when the
//								// user flips the device
//								if (!treeEmulator.isExpanded(catRow) || !treeEmulator.isExpanded(scRow))
//									entryRow.setVisibility(View.GONE);
//								entryRow.setOnClickListener(new OnClickListener() {																		
//									public void onClick(View v) {
//										Intent intent = new Intent(Intent.ACTION_EDIT, (Uri) v.getTag(), getActivity(), EntryActivity.class);
//										startActivity(intent);
//									}
//								});
//								MonthFragment.this.registerForContextMenu(entryRow);
//								if (entry.value > 0)
//									catRevenues += entry.value;
//								else
//									catExpenses -= entry.value;
//							} else {
//								// This entry row shouldn't have been looked at yet - it belongs to the wrong category 
//								// Therefore go back one position, then break out from the "entry" while loop
//								entriesCursor.moveToPrevious();
//								break;
//							} // end else
//						} // end while ("entries")
//						// Add the category's revenues and expenses to the supercategory's 
//						// revenues and expenses, and adjust the catRow's text fields
//						scRevenues += catRevenues;
//						scExpenses += catExpenses;
//						((TextView) catRow.findViewById(R.id.txt_revenue)).setText(catRevenues > 0 ? CurrencyValueFormatter.format(catRevenues) : "");						
//						((TextView) catRow.findViewById(R.id.txt_expense)).setText(catExpenses > 0 ? CurrencyValueFormatter.format(catExpenses) : "");						
//					} // end if (sc.id == cat.supercategory)
//					else {
//						// This category row shouldn't have been looked at yet - it belongs to the wrong supercategory 
//						// Therefore go back one position, then break out from the "category" while loop
//						catCursor.moveToPrevious();
//						break;
//					}
//				}
//				// Add the supercategory's revenues and expenses to the total
//				totalRevenues += scRevenues;
//				totalExpenses += scExpenses;
//				
//				// Update the scRow's revenue and expense text fields
//				((TextView) scRow.findViewById(R.id.txt_revenue)).setText(CurrencyValueFormatter.format(scRevenues));
//				((TextView) scRow.findViewById(R.id.txt_expense)).setText(CurrencyValueFormatter.format(scExpenses));						
//			}
//			// Finally done, now let's do some final adjustments
//			// Adjust the text fields for the month's total revenues and expenses... 
//			((TextView) tableLayout.findViewById(R.id.txt_subtotal_revenue)).setText(CurrencyValueFormatter.format(totalRevenues));
//			((TextView) tableLayout.findViewById(R.id.txt_subtotal_expense)).setText(CurrencyValueFormatter.format(totalExpenses));
//			// ...and the displayed grand total, this one in red if negative
//			int grandtotal = totalRevenues - totalExpenses;
//			((TextView) tableLayout.findViewById(R.id.txt_grandtotal_value)).setText(CurrencyValueFormatter.format(grandtotal));
//			if (grandtotal < 0)
//				((TextView) tableLayout.findViewById(R.id.txt_grandtotal_value)).setTextColor(getResources().getColor(R.color.red));
//		}
//	}
//				
//	public MonthFragment() {
//		treeEmulator = new TreeEmulator();
//		treeEmulator.setIdentifier(new TagStringIdentifier());
//	}
//	
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setRetainInstance(true);
//	}
//	
//	@Override
//	public void onDetach() {
//		// Make sure no references to the table rows are left within
//		// the tree emulator
//		treeEmulator.clear();
//		super.onDetach();
//	}
//	
//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
//		super.onCreateContextMenu(menu, v, menuInfo);
//		MenuInflater inflater = getActivity().getMenuInflater();
//		inflater.inflate(R.menu.context_entry_row, menu);
//	}
//	
//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case R.id.menu_make_rule:
//				// TODO Create rule using the caption and category from the entry row on which the
//				// context menu was invoked
//				;
//		}
//		return true;
//	}
//	
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
//		View view = inflater.inflate(R.layout.layout_month_spreadsheet,
//				container, false);
//		Bundle args = getArguments();
//		
//		// Set the text view for the caption of the spreadsheet to be
//		// <month> <year>
//		int year = MonthsElapsedCalculator.getYear(args.getInt(ARG_MONTH_ID)); 
//		int month = MonthsElapsedCalculator.getMonth(args.getInt(ARG_MONTH_ID)); 
//		String monthName = DateFormatSymbols.getInstance().getMonths()[month]; 
//		((TextView) view.findViewById(R.id.txt_month)).setText(monthName + " " + String.valueOf(year));
//		
//		FrameLayout overlay = new FrameLayout(getActivity());
//		
//		ImageView btn = new ImageView(getActivity());
//		btn.setImageResource(R.drawable.action_autofill_new_month);
//		
////		overlay.setBackgroundColor(getResources().getColor(R.color.transparent_white));
//		overlay.addView(btn);
//		btn.setScaleX(0.33f);
//		btn.setScaleY(0.33f);
//		btn.setClickable(true);
//		FrameLayout.LayoutParams lp = (LayoutParams) btn.getLayoutParams();
////		lp.width=100;
////		lp.height=100;
//		lp.gravity = Gravity.CENTER;
//		getActivity().addContentView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
//				
//		new QueryTask().execute(month + 1,year); // month index is zero based, sql strings aren't
//
//		return view;
//	}

}
