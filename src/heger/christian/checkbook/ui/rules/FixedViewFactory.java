package heger.christian.checkbook.ui.rules;

import heger.christian.checkbook.R;
import heger.christian.checkbook.ui.categories.CategoriesActivity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FixedViewFactory {
	private final RulesActivity rulesActivity;
	
	public FixedViewFactory(RulesActivity rulesActivity) {
		this.rulesActivity = rulesActivity;
	}

	public View createEmptyView() {
		LinearLayout layout = new LinearLayout(rulesActivity);
		layout.setOrientation(LinearLayout.VERTICAL);
		TypedArray styledAttr = rulesActivity.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorHint } );
		TextView textview = new TextView(rulesActivity);
		layout.setId(android.R.id.empty);
		textview.setText(R.string.no_rules);
		
		textview.setTextColor(styledAttr.getColor(0, 0x00000000));		
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
