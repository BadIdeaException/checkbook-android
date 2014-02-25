package heger.christian.ledger.ui.spreadsheet;

import heger.christian.ledger.R;
import heger.christian.ledger.control.MonthsElapsedCalculator;
import heger.christian.ledger.providers.LedgerContentProvider;
import heger.christian.ledger.providers.MonthsContract;
import heger.christian.ledger.ui.categories.CategoriesActivity;
import heger.christian.ledger.ui.entry.EntryActivity;
import heger.christian.ledger.ui.rules.RulesActivity;
import heger.christian.ledger.ui.spreadsheet.SelectMonthDialog.DialogListener;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;




public class SpreadsheetActivity extends FragmentActivity {
	public static final String ARG_MONTH_ID = "monthID";

	/**
	 * Adapter that provides a certain number (encoded in the constant <code>RANGE</code>) of months before and after the
	 * current month. The adapter itself is stateless, but requires extensive communication with the view pager and its
	 * adapter to determine the current, earliest, and latest available month.
	 * @author chris
	 *
	 */
	private class NavigationAdapter extends BaseAdapter {
		private static final int RANGE = 3; // Number of months before and after current one to show in spinner 

		/**
		 * This is the item id for the "go to month" item
		 */
		public static final long ID_GO_TO_MONTH = 0x8000000000000001l;
		/**
		 * This is the item id for the "go to latest" item
		 */
		public static final long ID_GO_TO_LATEST = 0x8000000000000002l;
		/**
		 * This is the item id for the divider
		 */
		public static final long ID_DIVIDER = 0x8000000000000003l;
		
		public NavigationAdapter(Context context, int layout) {
			super();
		}
		private Integer getMonthIdForPosition(int position) {
			int current = pagerAdapter.getMonthId(pager.getCurrentItem());
			int result = Math.max(current - RANGE, pagerAdapter.getEarliest()) + position;
			if (result <= pagerAdapter.getLatest())
				return result;
			return null;
		}
		public Integer getPositionForMonth(int monthId) {
			int earliest = getMonthIdForPosition(0);
			if (monthId < earliest) return null;
			if (monthId - earliest > getCount() - 3) return null;
			return monthId - earliest;
		}
		@Override
		public int getCount() {
			int current = pagerAdapter.getMonthId(pager.getCurrentItem());
			int count = 1;
			count += Math.min(current - pagerAdapter.getEarliest(), RANGE); // Show at most RANGE months preceding the current one
			count += Math.min(pagerAdapter.getLatest() - current, RANGE); // Show at most RANGE months succeeding the current one
			count += 3; // divider plus "go to month" item plus "go to latest" item
			return count;
		}
		@Override
		public boolean areAllItemsEnabled() { return false; }
		@Override
		public boolean isEnabled(int position) {
			return position != getCount() - 3; // Only two items, but position is zero based
		}
		private View getDivider() {
			TypedArray styledAttr = getTheme().obtainStyledAttributes(new int[] { android.R.attr.listDivider });			
			TextView divider = new TextView(SpreadsheetActivity.this); 
			divider.setHeight(2 * (int) (getResources().getDisplayMetrics().density + 0.5f));
			divider.setBackground(styledAttr.getDrawable(0));
			styledAttr.recycle();
			return divider;			
		}
		private View newView() {
			return getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, null);
		}
		protected void bindView(View view, int position) {
			String text;
			long id = getItemId(position);
			// Cannot switch/case on a long:
			if (id == ID_GO_TO_LATEST) {
				text = getResources().getString(R.string.menu_goto_latest_title);
			} else if (id == ID_GO_TO_MONTH) { 
				text = getResources().getString(R.string.menu_goto_month_title);
			} else if (id == ID_DIVIDER) {
				throw new RuntimeException("Cannot bind data to divider view");
			} else { 
				int month = (int) id;
				text = DateFormatSymbols.getInstance().getMonths()[MonthsElapsedCalculator.getMonth(month)] + " " + MonthsElapsedCalculator.getYear(month);
			}
			((TextView) view.findViewById(android.R.id.text1)).setText(text);
		}
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			if (position == getCount() - 3) return getDivider(); // Only two items, but position is zero based

			if (convertView == null || convertView.findViewById(android.R.id.text1) == null)
				convertView = newView();
			bindView(convertView, position);
			return convertView;
		}
		/**
		 * Unsupported. Always returns <code>null</code>.
		 */
		@Override
		public Object getItem(int position) {
			return null;
		}
		/**
		 * Returns the item id for the passed position. For the last three items, this will be one of the constants
		 * <code>ID_GO_TO_LATEST</code>, <code>ID_GO_TO_MONTH</code> or <code>ID_DIVIDER</code>, otherwise it will be
		 * the month id for that position.
		 */
		@Override
		public long getItemId(int position) {
			switch (getCount() - 1 - position) {
				case 0: return ID_GO_TO_LATEST;
				case 1: return ID_GO_TO_MONTH;
				case 2: return ID_DIVIDER;
				default: return getMonthIdForPosition(position);
			}
		}
		@Override
		public boolean hasStableIds() { return true; }
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getDropDownView(position, convertView, parent);
		}
	}
	
	private class NavigationListener implements OnPageChangeListener, OnNavigationListener {
		private boolean inhibited = false;
		protected void inhibit() { inhibited = true; }
		protected void release() { inhibited = false; }
		private void moveToPosition(int monthId) {
			if (!inhibited) {
				try {
					inhibit();
					pager.setCurrentItem(pagerAdapter.getIndex(monthId), true);
					navigationAdapter.notifyDataSetChanged();
					getActionBar().setSelectedNavigationItem(navigationAdapter.getPositionForMonth(monthId));
				} finally {
					// Need to release asynchronously because calling setCurrentItem and notifyDataSetChanged will 
					// (asynchronously) refresh the UI and cause calls to their respective listeners. Therefore, simply
					// releasing the lock would result in an infinite loop.
					// When posting this runnable, any posts from setting the pager page and refreshing the spinner data
					// will already have been posted, so the release call is guaranteed to be behind them in the message queue.
					// Likewise, it is sure to be executed before the user can produce more input events, that is, any additional
					// swipe gestures or selections will be added to the message queue after the release call.
					pager.post(new Runnable() {
						@Override
						public void run() { release(); }
					});
				}
			}
		}
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			if ((itemId & 0x8000000000000000l) == 0) {
				int month = (int) itemId;
				moveToPosition(month);
			} else if (itemId == NavigationAdapter.ID_GO_TO_MONTH) {
				// Fake a move so that the drop down view won't show "go to month" as selected
				moveToPosition(pagerAdapter.getMonthId(pager.getCurrentItem()));
				SelectMonthDialog fragment = SelectMonthDialog.newInstance(MonthsElapsedCalculator.getMonth(pagerAdapter.getEarliest()), 
						MonthsElapsedCalculator.getYear(pagerAdapter.getEarliest()),
						MonthsElapsedCalculator.getMonth(pagerAdapter.getMonthId(pager.getCurrentItem())),
						MonthsElapsedCalculator.getYear(pagerAdapter.getMonthId(pager.getCurrentItem())));	
				fragment.setDialogListener(new DialogListener() {
					@Override
					public void onDialogPositiveClick(DialogFragment dialog, int month, int year) {
						moveToPosition(MonthsElapsedCalculator.getMonthsElapsed(month, year));
					}
					@Override
					public void onDialogNegativeClick(DialogFragment dialog, int month, int year) {
					}			
				});
				fragment.show(getSupportFragmentManager(), "gotomonth");
			} else if (itemId == NavigationAdapter.ID_GO_TO_LATEST) {
				moveToPosition(pagerAdapter.getLatest());
			}
			return true;
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		@Override
		public void onPageSelected(int position) {
			moveToPosition(pagerAdapter.getMonthId(position));
		}
		
	}
	protected class MonthPagerAdapter extends FragmentStatePagerAdapter {
		/**
		 * Number of elapsed months since January 1970 for the earliest month
		 * found in the database. This counts partial months as well, i.e. 
		 * for January 1970 itself, this will evaluate to 1.
		 * The actual query to the db is deferred until the first call of
		 * {@link getEarliest}.
		 */
		private int earliest = 0;
		
		public MonthPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}
		@Override
		public Fragment getItem(int index) {
			Fragment monthFragment = new MonthFragment();
			Bundle args = new Bundle();
			// Requested month id is "earliest" plus requested index
			args.putInt(ARG_MONTH_ID, getEarliest() + index);
			monthFragment.setArguments(args);
			return monthFragment;
		}
		@Override
		public int getCount() {
			Calendar c = Calendar.getInstance();
			int monthsTillNow = MonthsElapsedCalculator.getMonthsElapsed(c.get(Calendar.MONTH), c.get(Calendar.YEAR));
			return monthsTillNow - getEarliest() + 1; // +1: page index is zero-based
		}	
		
		public int getIndex(int monthId) {
			return monthId - getEarliest();
		}
		public int getMonthId(int index) {
			return getEarliest() + index;
		}
		
		/**
		 * Returns the number of elapsed months since January 1970 (the month id) for the earliest 
		 * entry found in the entries table. If the entries table is empty, the returned
		 * value will return the number of elapsed months since January 1970 up to <i>now</i>.
		 * <br><br>
		 * The query to the database is only performed once and then cached.
		 * Any subsequent calls will return the cached value.
		 * @return The number of elapsed months since January 1970 for the earliest entry in the
		 * database if one exists, or for <i>now</i> if the entries table is empty.
		 */
		public int getEarliest() {
			if (earliest == 0) {
				Cursor cursor = getContentResolver().query(MonthsContract.CONTENT_URI, 
						new String[] { "MIN(" + MonthsContract._ID + ") AS " + MonthsContract._ID }, 
						null, 
						null, 
						null);
				if (cursor.moveToFirst() && 
						!cursor.isNull(cursor.getColumnIndex(MonthsContract._ID))) { // In an empty table, cursor contains only a single null entry
					earliest = cursor.getInt(cursor.getColumnIndex(MonthsContract._ID));									
				} else {				
					Calendar calendar = Calendar.getInstance();
					earliest = MonthsElapsedCalculator.getMonthsElapsed(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)); 
				}
				cursor.close();
			}
			return earliest;
		}
		public int getLatest() {
			return getEarliest() + getCount() - 1;
		}
	}
	private ViewPager pager;
	private MonthPagerAdapter pagerAdapter;
	private NavigationAdapter navigationAdapter;
	private NavigationListener navigationListener = new NavigationListener();
	
	/**
	 * Converts the month id from a content URI into a page number for the pager adapter.
	 * No checks on the passed URI are made, the passed parameter is expected to contain an appropriate URI!
	 * @param uri
	 * @return
	 */
	private int getPageFromURI(Uri uri) {
		int page = pagerAdapter.getIndex(((int) ContentUris.parseId(uri)));
		return page;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		
		Uri uri = intent.getData();
		if (uri != null && (LedgerContentProvider.URI_MATCHER.match(uri) == LedgerContentProvider.URI_MONTHS_ID)) {
			pager.setCurrentItem(getPageFromURI(uri));
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.actvy_spreadsheet);
		
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		navigationAdapter = new NavigationAdapter(this,android.R.layout.simple_spinner_dropdown_item);

		pager = (ViewPager) findViewById(R.id.viewpager);
		pagerAdapter = new MonthPagerAdapter(getSupportFragmentManager()); 
		pager.setAdapter(pagerAdapter);		
		pager.setOnPageChangeListener(navigationListener);
		
		// Figure out which page to display in the view pager.
		// 1. If the intent contains a content URI pointing to a month id, display that month
		// 2. Alternately, if the savedInstanceState contains a month id, display that month
		// 3. Per default, show the latest month		
		int page;
		Uri uri = getIntent().getData();
		if (uri != null && (LedgerContentProvider.URI_MATCHER.match(uri) == LedgerContentProvider.URI_MONTHS_ID)) {
			page = getPageFromURI(uri); 
		} else if (savedInstanceState != null && savedInstanceState.containsKey(ARG_MONTH_ID)) {
			page = pagerAdapter.getIndex(savedInstanceState.getInt(ARG_MONTH_ID));
		} else {
			page = pagerAdapter.getLatest(); // Display the latest month
		}
		
		getActionBar().setListNavigationCallbacks(navigationAdapter, navigationListener);

		pager.setCurrentItem(page);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle out) {
		int monthId = pagerAdapter.getMonthId(pager.getCurrentItem());
		out.putInt(ARG_MONTH_ID, monthId);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.spreadsheet, menu);
		return true;
	}

	public boolean onAddEntryClick(MenuItem menu) {
		Intent intent = new Intent(this, EntryActivity.class);
		startActivity(intent);
		return true;
	}
	
	public boolean onGoToMonthClick(MenuItem menu) {
		SelectMonthDialog fragment = SelectMonthDialog.newInstance(MonthsElapsedCalculator.getMonth(pagerAdapter.getEarliest()), 
				MonthsElapsedCalculator.getYear(pagerAdapter.getEarliest()),
				MonthsElapsedCalculator.getMonth(pagerAdapter.getMonthId(pager.getCurrentItem())),
				MonthsElapsedCalculator.getYear(pagerAdapter.getMonthId(pager.getCurrentItem())));	
		fragment.setDialogListener(new DialogListener() {
			@Override
			public void onDialogPositiveClick(DialogFragment dialog, int month, int year) {
				pager.setCurrentItem(pagerAdapter.getIndex(MonthsElapsedCalculator.getMonthsElapsed(month, year)), true);
			}
			@Override
			public void onDialogNegativeClick(DialogFragment dialog, int month, int year) {}			
		});
		fragment.show(getSupportFragmentManager(), "gotomonth");
		return true;
	}
	
	public boolean onManageCategoriesClick(MenuItem menu) {
		Intent intent = new Intent(this, CategoriesActivity.class);
		startActivity(intent);
		return true;
	}

	public boolean onManageRulesClick(MenuItem menu) {
		Intent intent = new Intent(this, RulesActivity.class);
		startActivity(intent);
		return true;
	}

}
