package heger.christian.checkbook.ui.spreadsheet;

import heger.christian.checkbook.R;
import heger.christian.checkbook.adapters.SpreadsheetAdapter;
import heger.christian.checkbook.control.MonthsElapsedCalculator;
import heger.christian.checkbook.providers.CategorySubtotalsContract;
import heger.christian.checkbook.providers.EntryContract;
import heger.christian.checkbook.ui.CurrencyValueFormatter;
import heger.christian.checkbook.ui.entry.EntryActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Paint;
import android.graphics.Typeface;
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
	private static final String TAG = MonthFragment.class.getSimpleName();

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
	public void onStart() {
		super.onStart();
		// Because of setRetainInstance(true), onCreate won't get called when navigating back to
		// SpreadsheetActivity from another app.
		// Therefore if the loader isn't currently running, start it to update data.
		if (getLoaderManager().getLoader(0) != null && !getLoaderManager().getLoader(0).isStarted())
			getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
		View view = inflater.inflate(R.layout.layout_month_spreadsheet,
				container, false);
		Bundle args = getArguments();

		this.month = args.getInt(SpreadsheetActivity.ARG_MONTH_ID);

		listEntries = (ExpandableListView) view.findViewById(R.id.list_entries);

		// Add divider between last entry and real footer view
		footerView = new View(getActivity());
		footerView.setBackgroundResource(R.drawable.spacer);
		footerView.setClickable(false);
		listEntries.addFooterView(footerView);

		footerView = getLayoutInflater(null).inflate(R.layout.listitem_spreadsheet_category, null);
		int left = footerView.getPaddingLeft();
		int top = footerView.getPaddingTop();
		int right = footerView.getPaddingRight();
		int bottom = footerView.getPaddingBottom();
		footerView.setBackgroundResource(R.drawable.tile_full);
		footerView.setPadding(left, top, right, bottom);
		listEntries.addFooterView(footerView, null, false);

		getLoaderManager().initLoader(0, null, this);
		adapter = new SpreadsheetAdapter(this.getActivity(),
				null,
				R.layout.listitem_spreadsheet_category,
				new String[] { CategorySubtotalsContract.COL_NAME_CAPTION, CategorySubtotalsContract.COL_NAME_VALUE },
				new int[] { R.id.txt_caption, R.id.txt_value },
				R.layout.listitem_spreadsheet_entry,
				new String[] { EntryContract.COL_NAME_CAPTION, EntryContract.COL_NAME_VALUE },
				new int[] { R.id.txt_caption, R.id.txt_value },
				MonthsElapsedCalculator.getMonth(month), MonthsElapsedCalculator.getYear(month));
		adapter.setViewBinder(new RowViewBinder(getActivity()));
		listEntries.setAdapter(adapter);
		listEntries.setOnChildClickListener(this);

		// Set the text view for the caption of the spreadsheet to be
		// <month> <year>
//		String monthName = DateFormatSymbols.getInstance().getMonths()[MonthsElapsedCalculator.getMonth(month)];
//		((TextView) view.findViewById(R.id.txt_month)).setText(monthName + " " + String.valueOf(MonthsElapsedCalculator.getYear(month)));

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
				ContentUris.withAppendedId(EntryContract.CONTENT_URI,entryId),
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
		TextView caption = (TextView) footerView.findViewById(R.id.txt_caption);
		TextView value = (TextView) footerView.findViewById(R.id.txt_value);
		binder.setViewValue(caption, cursor, 1);
		binder.setViewValue(value, cursor, 2);
		caption.setTypeface(caption.getTypeface(), Typeface.BOLD);
		value.setTypeface(value.getTypeface(), Typeface.BOLD);
		caption.setPaintFlags(caption.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		value.setPaintFlags(value.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
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
		if (data != null) {
			int valueColumn = data.getColumnIndex(CategorySubtotalsContract.COL_NAME_VALUE);
			data.moveToPosition(-1);
			while (data.moveToNext()) {
				total += data.getInt(valueColumn);
			}
		}
		updateFooter(total);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.setGroupCursor(null);
		updateFooter(0);
	}
}
