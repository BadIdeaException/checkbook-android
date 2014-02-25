package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import heger.christian.ledger.adapters.RulesAdapter;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.RulesContract;
import heger.christian.ledger.ui.categories.CategoriesActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class FixedViewFactory {
	private final RulesActivity rulesActivity;
	
	public FixedViewFactory(RulesActivity rulesActivity) {
		this.rulesActivity = rulesActivity;
	}

	public View createAddView() {
		TextView textview = new TextView(rulesActivity);
		TypedArray styledAttr = rulesActivity.getTheme().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });			
		textview.setFocusable(true);
		textview.setClickable(true);
		textview.setNextFocusRightId(View.NO_ID);
		textview.setTextAppearance(rulesActivity, android.R.style.TextAppearance_Medium);		
		textview.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		textview.setGravity(Gravity.CENTER_VERTICAL);		
		textview.setText(R.string.add_rule);
		textview.setOnClickListener(new OnClickListener() {				
			@Override
			public void onClick(View v) {
				new AsyncTask<Object, Object, Long>() {
					@Override
					protected Long doInBackground(Object... params) {
						ContentValues values = new ContentValues();
						values.put(RulesContract.COL_NAME_ANTECEDENT, "");
						// Need an arbitrary category id for insertion. This gets the minimum id in the table
						Cursor cursor = rulesActivity.getContentResolver().query(CategoryContract.CONTENT_URI, 
								new String[] { "MIN(" + CategoryContract._ID + ") AS " + CategoryContract._ID }, 
								null, null, null);
						cursor.moveToFirst();
						values.put(RulesContract.COL_NAME_CONSEQUENT, CursorAccessHelper.getInt(cursor, CategoryContract._ID));
						Uri uri = rulesActivity.getContentResolver().insert(RulesContract.CONTENT_URI, values);												
						return ContentUris.parseId(uri);
					}
					@Override
					protected void onPostExecute(final Long id) {	
						rulesActivity.getListAdapter().registerDataSetObserver(new DataSetObserver() {
							@Override
							public void onChanged() {
								RulesAdapter adapter = (RulesAdapter) rulesActivity.getListAdapter();
								adapter.unregisterDataSetObserver(this);
								int position = adapter.getItemPosition(id); 
								rulesActivity.getListView().smoothScrollToPositionFromTop(position,0);
								rulesActivity.startEditing(id);
							}
						});						
					}
				}.execute((Object[]) null);
			}
		});
		textview.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_input_add, 0, 0, 0);
		textview.setBackground(styledAttr.getDrawable(0));
		styledAttr.recycle();
		return textview;
	}

	public View createEmptyView() {
		LinearLayout layout = new LinearLayout(rulesActivity);
		layout.setOrientation(LinearLayout.VERTICAL);
		TypedArray styledAttr = rulesActivity.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorHint } );
		TextView textview = new TextView(rulesActivity);
		layout.setId(android.R.id.empty);
		textview.setText(R.string.no_rules);
		
		textview.setTextColor(styledAttr.getColor(0, 0x00000000));		
		layout.addView(createAddView());
		layout.addView(textview);
		styledAttr.recycle();
	
		layout.setVisibility(View.GONE);
		ViewGroup parent = ((ViewGroup) rulesActivity.getListView().getParent());
		parent.removeView(parent.findViewById(android.R.id.empty));
		parent.addView(layout);
		
		return layout;
	}
	
	public View createNoCategoriesView() {
		LinearLayout layout = new LinearLayout(rulesActivity);
		layout.setOrientation(LinearLayout.VERTICAL);
		TypedArray styledAttr = rulesActivity.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorHint } );
		TextView textview = new TextView(rulesActivity);
		layout.setId(android.R.id.empty);
		textview.setText(R.string.rules_no_categories);
		textview.setTextColor(styledAttr.getColor(0, 0x000000));		
		
		TextView addView = (TextView) createAddView();
		// Clone the add icon and desaturate it
		Drawable icon = addView.getCompoundDrawables()[0].mutate();
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		icon.setColorFilter(new ColorMatrixColorFilter(matrix));
		addView.setCompoundDrawables(icon, null, null, null);
		addView.setEnabled(false);
		
		// Create internal link to the manage categories screen
		TextView txtManageCategories = new TextView(rulesActivity);
		int dp10 = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, rulesActivity.getResources().getDisplayMetrics()));
		Spannable spannable = new SpannableString(rulesActivity.getResources().getString(R.string.menu_manage_categories_title)); 
		spannable.setSpan(new ClickableSpan() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(rulesActivity, CategoriesActivity.class);
				rulesActivity.startActivity(intent);
			}
		}, 0,spannable.length(),0);
		txtManageCategories.setMovementMethod(LinkMovementMethod.getInstance());
		txtManageCategories.setText(spannable);
		txtManageCategories.setPadding(0, dp10, 0, dp10);		
		
		layout.addView(addView);
		layout.addView(textview);
		layout.addView(txtManageCategories);
		
		styledAttr.recycle();
		layout.setVisibility(View.GONE);
		ViewGroup parent = ((ViewGroup) rulesActivity.getListView().getParent());
		parent.removeView(parent.findViewById(android.R.id.empty));
		parent.addView(layout);

		return layout;
	}
}
