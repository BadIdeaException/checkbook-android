package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.RulesContract;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class RowEditor extends heger.christian.ledger.ui.categories.RowEditor {
	/* package private */ CursorAdapter categoriesAdapter;

	private TextView txtCategory;
	
	@Override
	protected void onEditStart(Bundle args) {
		super.onEditStart(args);
		ViewGroup row = getRow();
			
		txtCategory = (TextView) row.findViewById(R.id.txt_category);
		Spinner spinCategory = new Spinner(row.getContext());	
		spinCategory.setId(R.id.spin_category);
		spinCategory.setLayoutParams(txtCategory.getLayoutParams());
		spinCategory.setAdapter(categoriesAdapter);
		
		CursorAccessHelper helper = new CursorAccessHelper(categoriesAdapter.getCursor());
		int category = (int) ContentUris.parseId((Uri) txtCategory.getTag()); 
		int position = helper.moveToId(category);
		spinCategory.setSelection(position != -1 ? position : Spinner.INVALID_POSITION);
		
		ViewGroup parent = (ViewGroup) txtCategory.getParent();
		int index = parent.indexOfChild(txtCategory);
		parent.findViewById(R.id.txt_category).setVisibility(View.GONE);
		parent.addView(spinCategory,index);
	}
	
	/**
	 * @return An array containing the new caption for the textview and the id of the
	 * selected category.
	 */
	@Override 
	protected Object onEditStop() {
		ViewGroup row = getRow();
		
		Spinner spinCategory = (Spinner) row.findViewById(R.id.spin_category); 
		categoriesAdapter.getCursor().moveToPosition(spinCategory.getSelectedItemPosition());
		int category = new CursorAccessHelper(categoriesAdapter.getCursor()).getInt(RulesContract._ID);
		
		ViewGroup parent = (ViewGroup) spinCategory.getParent();
		parent.findViewById(R.id.txt_category).setVisibility(View.VISIBLE);
		parent.removeView(spinCategory);
		
		return new Object[] { super.onEditStop(), category };
	}

}
