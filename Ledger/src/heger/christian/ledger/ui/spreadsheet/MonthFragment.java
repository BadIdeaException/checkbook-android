package heger.christian.ledger.ui.spreadsheet;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.SpreadsheetAdapter;
import heger.christian.ledger.control.MonthsElapsedCalculator;
import heger.christian.ledger.providers.CategorySubtotalsContract;
import heger.christian.ledger.providers.EntryContract;
import heger.christian.ledger.ui.CurrencyValueFormatter;
import heger.christian.ledger.ui.entry.EntryActivity;

import java.text.DateFormatSymbols;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
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
	/**
	 * ViewBinder for displaying entries and categories with attached subtotals. Negative values will be printed in the color defined in
	 * <code>R.color.red</code>.
	 * @author chris
	 *
	 */
	private static class RowViewBinder implements ViewBinder {
		private final Context context;
		private int textColorPrimary;
		public RowViewBinder(Context context) {
			this.context = context;
			TypedArray styledAttr = context.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary } );
			textColorPrimary = styledAttr.getColor(0, 0xff000000);
			styledAttr.recycle();
		}
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String columnName = cursor.getColumnName(columnIndex);
			if (columnName.equals(EntryContract.COL_NAME_CAPTION) || columnName.equals(CategorySubtotalsContract.COL_NAME_CAPTION)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			}
			if (columnName.equals(EntryContract.COL_NAME_VALUE) || columnName.equals(CategorySubtotalsContract.COL_NAME_VALUE)) {
				TextView textview = (TextView) view;
				int value = cursor.getInt(columnIndex);
				textview.setText(CurrencyValueFormatter.format(value));
				if (value < 0) {
					textview.setTextColor(context.getResources().getColor(R.color.red));
				} else {
					textview.setTextColor(textColorPrimary);
				}
				return true;
			}
			return false;
		}
		
	}
	private ExpandableListView listEntries;
	private SpreadsheetAdapter adapter;
	// ID of the month this fragment is displaying
	private int month;
	private View footerView;
	
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
		
		this.month = args.getInt(SpreadsheetActivity.ARG_MONTH_ID);
		
		getLoaderManager().initLoader(0, null, this);

		listEntries = (ExpandableListView) view.findViewById(R.id.list_entries);
		footerView = getLayoutInflater(null).inflate(R.layout.listitem_entries, null);
		
		// Android only draws a divider between the last list item and the footer if the footer is selectable.
		// Making the actual footer selectable will make it acknowledge clicks with the standard blue background,
		// which for a no-op is somewhat ugly.
		// So, an empty (zero height) view is added as a workaround. Android will draw a divider between the 
		// last list item and this.
		listEntries.addFooterView(new View(getActivity()), null, true);
		listEntries.addFooterView(footerView, null, false);
		
		adapter = new SpreadsheetAdapter(this.getActivity(), 
				null, 
				R.layout.listitem_entries, 
				new String[] { CategorySubtotalsContract.COL_NAME_CAPTION, CategorySubtotalsContract.COL_NAME_VALUE }, 
				new int[] { R.id.txt_caption, R.id.txt_value }, 
				R.layout.listitem_entries, 
				new String[] { EntryContract.COL_NAME_CAPTION, EntryContract.COL_NAME_VALUE }, 
				new int[] { R.id.txt_caption, R.id.txt_value },
				MonthsElapsedCalculator.getMonth(month), MonthsElapsedCalculator.getYear(month));
		adapter.setViewBinder(new RowViewBinder(getActivity()));
		listEntries.setAdapter(adapter);
		listEntries.setOnChildClickListener(this);
		
		// Set the text view for the caption of the spreadsheet to be
		// <month> <year>
		String monthName = DateFormatSymbols.getInstance().getMonths()[MonthsElapsedCalculator.getMonth(month)]; 
		((TextView) view.findViewById(R.id.txt_month)).setText(monthName + " " + String.valueOf(MonthsElapsedCalculator.getYear(month)));
		
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
	
	/**
	 * Updates the footer view to the passed value. The caption is the string resource <i>R.string.total</i>.
	 * It creates a temporary one-row cursor, then passes it to a new instance of <code>{@link MonthFragment.RowViewBinder}</code>.
	 * The caption will be bound to the footer's child with id <i>R.id.txt_caption</i>, the value to that with id <i>R.id.txt_value</i>.
	 * @param newValue - The new total to display.
	 */
	private void updateFooter(int newValue) {
		// Create new one-row MatrixCursor
		// An (empty) _id column is inserted to keep the row indices in line with the actual data cursors and 
		// to ensure compatibility with the ExpandableListView
		MatrixCursor cursor = new MatrixCursor(new String[] { CategorySubtotalsContract._ID, 
					CategorySubtotalsContract.COL_NAME_CAPTION, 
					CategorySubtotalsContract.COL_NAME_VALUE }, 
				1);
		cursor.addRow(new Object[] { null, getResources().getString(R.string.total), newValue });
		// Move cursor to its only row
		cursor.moveToFirst();
		// Create view binder and pass in the appropriate columns and child views
		ViewBinder binder = new RowViewBinder(getActivity());
		binder.setViewValue(footerView.findViewById(R.id.txt_caption), cursor, 1);
		binder.setViewValue(footerView.findViewById(R.id.txt_value), cursor, 2);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri.Builder builder = CategorySubtotalsContract.CONTENT_URI.buildUpon();
		builder.appendQueryParameter(CategorySubtotalsContract.QUERY_ARG_MONTH, String.valueOf(month));
		return new CursorLoader(this.getActivity(), 
				builder.build(), 
				null, 
				null,
				null,
				CategorySubtotalsContract._ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {		
		adapter.setGroupCursor(data);	
		int total = 0;
		int valueColumn = data.getColumnIndex(CategorySubtotalsContract.COL_NAME_VALUE);
		data.moveToPosition(-1);
		while (data.moveToNext()) {
			total += data.getInt(valueColumn);
		}
		updateFooter(total);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.setGroupCursor(null);
		updateFooter(0);
	}
}
