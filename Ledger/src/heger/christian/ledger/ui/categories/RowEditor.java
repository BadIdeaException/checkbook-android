package heger.christian.ledger.ui.categories;

import heger.christian.ledger.R;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class RowEditor {
	public interface RowEditorListener {
		/**
		 * Called when the passed row has been put into editing mode.
		 * @param row - The row that was put into editing mode
		 */
		public void onEditStart(ViewGroup row);
		/**
		 * Called when the passed row has been put out of editing mode.
		 * @param row - The row that was put out of editing mode
		 * @param caption - The new caption 
		 */
		public void onEditStop(ViewGroup row, Object values);
	}
	
	private ViewGroup row;
	private RowEditorListener listener;
	
	protected void onEditStart(Bundle args) {
		ViewGroup row = getRow();
		ViewGroup parent;
		TextView txtCaption = (TextView) row.findViewById(R.id.txt_caption);
				
		// Create a new EditText to replace caption text view
		final EditText editCaption = new EditText(row.getContext());
		editCaption.setId(R.id.edit_caption);
		editCaption.setText(txtCaption.getText());
		editCaption.selectAll();
		editCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX,txtCaption.getTextSize());
		editCaption.setSingleLine();
		editCaption.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((MarginLayoutParams) txtCaption.getLayoutParams());	
		layoutParams.width = txtCaption.getWidth() + Math.min(layoutParams.leftMargin, editCaption.getPaddingLeft());
		Log.d("Width", "TextView: " + txtCaption.getWidth() + " LayoutParams: " + layoutParams.width);
		layoutParams.height = LayoutParams.WRAP_CONTENT;
		layoutParams.weight = ((LinearLayout.LayoutParams) txtCaption.getLayoutParams()).weight;
		layoutParams.leftMargin = Math.max(layoutParams.leftMargin - editCaption.getPaddingLeft(), 0);	
		layoutParams.topMargin = Math.max(layoutParams.topMargin - 14, 0); // Best guess
		layoutParams.bottomMargin = Math.max(layoutParams.bottomMargin - 16, 0);
		editCaption.setLayoutParams(layoutParams);
		
						
		// Create "Done" button to replace edit button 
		ImageView btnEdit = (ImageView) row.findViewById(R.id.btn_edit);
		ImageView btnDone = new ImageView(row.getContext());
		btnDone.setId(R.id.btn_done);
		btnDone.setScaleType(btnEdit.getScaleType());
		btnDone.setScaleX(btnEdit.getScaleX());
		btnDone.setScaleY(btnEdit.getScaleY());
		btnDone.setImageResource(R.drawable.ic_checkmark);
		btnDone.setFocusable(true);
		btnDone.setBackground(btnEdit.getBackground());
		btnDone.setLayoutParams(btnEdit.getLayoutParams());
		btnDone.setPadding(btnEdit.getPaddingLeft(), btnEdit.getPaddingTop(), btnEdit.getPaddingRight(), btnEdit.getPaddingBottom());
		btnDone.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				stopEditing();				
			}
		});
		
		// Turn off edit button while already editing		
		parent = (ViewGroup) btnEdit.getParent();		
		btnEdit.setVisibility(View.GONE);
		parent.addView(btnDone, parent.indexOfChild(btnEdit));
			
		// Set listener so that when "done" is selected on the IME, editing is stopped
		editCaption.setOnEditorActionListener(new OnEditorActionListener() {			
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		            if (actionId == EditorInfo.IME_ACTION_DONE) {
		                stopEditing();
		            	return true;
		            }
		            return false;
		        }			
		});
		
		// Hide the caption text view
		txtCaption.setVisibility(View.GONE);		
		parent = (ViewGroup) txtCaption.getParent(); 
		parent.addView(editCaption, parent.indexOfChild(txtCaption));
		
		// Make sure that edit text can be reached using the dpad: When navigating to the row, focus the edittext
		row.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
					editCaption.requestFocus();
				
			}
		});
		
		// Grab focus for this EditText and display the soft input window
		editCaption.requestFocus();	
		final InputMethodManager mgr = (InputMethodManager) row.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		editCaption.postDelayed(new Runnable() { 
			public void run () { mgr.showSoftInput(editCaption, 0); }},50);		
	}
	
	/**
	 * Puts the editor's row into editing mode, that is hides the caption text view and
	 * replaces it with an appropriate edit text.
	 * @param args - Can be used to pass additional arguments 
	 */
	public void startEditing(Bundle args) {
		if (isEditing()) 
			return;
		
		onEditStart(args);
		
		if (listener != null)
			listener.onEditStart(getRow());
	}
	
	protected Object onEditStop() {
		String result;
		
		ViewGroup row = getRow();
		ViewGroup parent;
    	InputMethodManager mgr = (InputMethodManager) row.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(row.getWindowToken(), 0);
		
		TextView txtCaption = (TextView) row.findViewById(R.id.txt_caption); 
    	EditText editCaption = (EditText) row.findViewById(R.id.edit_caption);
    	ImageView btnDone = (ImageView) row.findViewById(R.id.btn_done);
    	
    	result = editCaption.getText().toString();
		txtCaption.setText(result);		
		
		parent = (ViewGroup) editCaption.getParent();
		parent.removeView(editCaption);
		parent = (ViewGroup) btnDone.getParent();
		parent.removeView(btnDone);
		parent.findViewById(R.id.btn_edit).setVisibility(View.VISIBLE);		
		txtCaption.setVisibility(View.VISIBLE);
		
		return result;
	}
	
	/**
	 * Puts the editor's row out of editing mode, that is hides the EditText and shows the 
	 * TextView, setting its text property to reflect the changes entered by the user.
	 * @return The new values for this row. In this implementation, a string containing the
	 * new value for the text view. 
	 */
	public Object stopEditing() {
		if (!isEditing())
			return null;
		
		Object result = onEditStop();
		
		if (listener != null)
			listener.onEditStop(row, result);		
	
		return result;
	}
	
	public ViewGroup getRow() {
		return row;
	}
	
	public void setRow(ViewGroup row) {
		if (this.row != row && isEditing())
			stopEditing();
		this.row = row;
	}
	
	public void setRowEditorListener(RowEditorListener listener) {
		this.listener = listener;
	}
	
	public boolean isEditing() {
		return row != null && row.findViewById(R.id.edit_caption) != null;
	}
}
