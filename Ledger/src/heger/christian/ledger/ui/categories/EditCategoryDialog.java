package heger.christian.ledger.ui.categories;

import heger.christian.ledger.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

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
		view.findViewById(R.id.btn_done).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDoneClick(v);
			}
		});
		
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

	public void onDoneClick(View v) {
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
