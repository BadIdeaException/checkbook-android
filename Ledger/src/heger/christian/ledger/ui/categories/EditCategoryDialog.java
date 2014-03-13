package heger.christian.ledger.ui.categories;

import heger.christian.ledger.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
public class EditCategoryDialog extends DialogFragment {
	protected static final String ARG_CAPTION = "caption";
	
	public interface EditCategoryDialogListener {
		public void onClose(String caption);
	}
	private EditCategoryDialogListener listener = null;
	private EditText editCaption;
	
	public static EditCategoryDialog newInstance(String caption) {
		EditCategoryDialog dialog = new EditCategoryDialog();
		Bundle args = new Bundle();
		args.putString(ARG_CAPTION, caption);
		dialog.setArguments(args);
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstance) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dlg_edit_category, null);
		builder.setView(view);	
		editCaption = (EditText) view.findViewById(R.id.edit_caption);
		editCaption.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == R.id.ime_enter || actionId == EditorInfo.IME_NULL) {
					onPositiveClick();
					return true;
				}
				return false;
			}
		});

		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onPositiveClick();
			}
		}).setNegativeButton(android.R.string.cancel, null);
		Bundle args = getArguments();
		if (args != null) {
			String caption = args.getString(ARG_CAPTION);
			if (caption == null) caption = "";
			editCaption.setText(caption);
		}
		editCaption.selectAll();
		editCaption.requestFocus();
		
		return builder.create();
	}
	
	protected void onPositiveClick() {
		if (listener != null) listener.onClose(editCaption.getText().toString());
		dismiss();
	}

	/**
	 * @return the listener
	 */
	public EditCategoryDialogListener getDialogListener() {
		return listener;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setDialogListener(EditCategoryDialogListener listener) {
		this.listener = listener;
	}
}
