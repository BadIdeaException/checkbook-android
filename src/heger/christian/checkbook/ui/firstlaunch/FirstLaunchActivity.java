package heger.christian.checkbook.ui.firstlaunch;

import heger.christian.checkbook.R;
import heger.christian.checkbook.accounts.Authenticator;
import heger.christian.checkbook.providers.CheckbookContentProvider;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FirstLaunchActivity extends Activity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v13.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.actvy_first_launch);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.first_launch, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class
			// below).
			return PlaceholderFragment.newInstance(position);
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_first_launch,
					container, false);

			ImageView image = (ImageView) rootView.findViewById(R.id.image);
			TextView caption = (TextView) rootView.findViewById(R.id.caption);
			TextView details = (TextView) rootView.findViewById(R.id.details);
			TextView login = (TextView) rootView.findViewById(R.id.login);
			ViewGroup progress = (ViewGroup) rootView.findViewById(android.R.id.progress);

			int imageRes;
			int captionRes;
			int detailsRes;
			int loginRes;

			int position = getArguments().getInt(ARG_SECTION_NUMBER);
			switch (position) {
				case 0:
					imageRes = R.drawable.spreadsheet;
					captionRes = R.string.caption1;
					detailsRes = R.string.details1;
					loginRes = R.string.skip_to_login;
					break;
				case 1:
					imageRes = R.drawable.money;
					captionRes = R.string.caption2;
					detailsRes = R.string.details2;
					loginRes = R.string.skip_to_login;
					break;
				case 2:
					imageRes = R.drawable.airplane;
					captionRes = R.string.caption3;
					detailsRes = R.string.details3;
					loginRes = R.string.login;
					break;
				default:
					return rootView;
			}
			image.setImageResource(imageRes);
			caption.setText(captionRes);
			details.setText(detailsRes);

			Spannable spannable = new SpannableString(getResources().getString(loginRes));
			spannable.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View view) {
					AccountManager manager = AccountManager.get(getActivity());
					manager.addAccount(Authenticator.ACCOUNT_TYPE, Authenticator.TOKEN_TYPE_ACCESS, null, null, getActivity(), new AccountManagerCallback<Bundle>() {
						@Override
						public void run(AccountManagerFuture<Bundle> future) {
							try {
								Bundle bundle = future.getResult();
								Account account = new Account(bundle.getString(AccountManager.KEY_ACCOUNT_NAME), bundle.getString(AccountManager.KEY_ACCOUNT_TYPE));
								ContentResolver.setSyncAutomatically(account, CheckbookContentProvider.AUTHORITY, true);
								ContentResolver.requestSync(account, CheckbookContentProvider.AUTHORITY, new Bundle());
							// Don't care about any exceptions - sync will just not automatically be enabled
							} catch (OperationCanceledException x) {
							} catch (AuthenticatorException x) {
							} catch (IOException x) {
							}
						}
					}, null);
					getActivity().finish();
				}
			}, 0,spannable.length(),0);
			login.setMovementMethod(LinkMovementMethod.getInstance());
			login.setText(spannable);

			for (int i = 0; i < progress.getChildCount(); i++) {
				progress.getChildAt(i).setSelected(i == position);
			}

			return rootView;
		}
	}

}
