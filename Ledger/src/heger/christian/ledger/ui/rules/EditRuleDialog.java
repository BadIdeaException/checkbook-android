package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.CategoryContract;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class EditRuleDialog extends DialogFragment {
	public static final String ARG_CAPTION = "caption";
	public static final String ARG_CATEGORY = "category";
	
	public interface EditRuleDialogListener {
		public void onClose(String caption, long category);
	}
	private EditRuleDialogListener listener = null;
	private EditText editCaption;
	private Spinner spinCategory;
	private Cursor categories;
	private CursorAdapter adapter;
	
	public static EditRuleDialog newInstance(String caption, long category, Cursor categories) {
		EditRuleDialog dialog = new EditRuleDialog();
		if (categories != null)
			dialog.categories = categories;
		else
			throw new IllegalArgumentException("Cannot edit rule without categories");
		
		Bundle args = new Bundle();
		args.putString(ARG_CAPTION, caption);
		args.putLong(ARG_CATEGORY, category);
		
		dialog.setArguments(args);
		return dialog;
	}

	/**
	 * Tries to find the position within the adapter that corresponds to a given id.
	 * @param id - The category id to look for
	 * @return The position within the adapter that has the given id, or -1 if the 
	 * id doesn't exist in the adapter, or the adapter is <code>null</code>.
	 */
	private int getCategoryPosition(long id) {
		if (adapter == null) return -1;
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == id)
				return i;
		}
		return -1;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstance) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dlg_edit_rule, null);
		builder.setView(view);	
		editCaption = (EditText) view.findViewById(R.id.edit_caption);
		
		spinCategory = (Spinner) view.findViewById(R.id.spin_category);
		
		adapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_spinner_item,
				categories,
				new String[] { CategoryContract.COL_NAME_CAPTION },
				new int[] { android.R.id.text1 }, 0) {
			@Override
			public long getItemId(int position) {
				Cursor cursor = getCursor();
				cursor.moveToPosition(position);
				return CursorAccessHelper.getInt(cursor, CategoryContract._ID);
			}
		}; 		
		spinCategory.setAdapter(adapter);
				
		Bundle args = getArguments();
		if (args != null) {
			String caption = args.getString(ARG_CAPTION);
			if (caption == null) caption = "";
			editCaption.setText(caption);
			int position = -1;
			if (args.containsKey(ARG_CATEGORY))
				position = getCategoryPosition(args.getLong(ARG_CATEGORY));
			if (position != -1)
				spinCategory.setSelection(position);
		}
		editCaption.selectAll();
		editCaption.requestFocus();
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (listener != null) listener.onClose(editCaption.getText().toString(), spinCategory.getSelectedItemId());
				dismiss();			
			}
		}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
		return builder.create();
	}
	
	public void setCategories(Cursor categories) {
		this.categories = categories;
		// Null check is necessary because this might get called before onCreate
		if (adapter != null)
			adapter.swapCursor(categories);
	}
	
	/**
	 * @return the listener
	 */
	public EditRuleDialogListener getDialogListener() {
		return listener;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setDialogListener(EditRuleDialogListener listener) {
		this.listener = listener;
	}
}
