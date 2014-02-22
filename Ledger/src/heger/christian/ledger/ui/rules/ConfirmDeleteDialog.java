package heger.christian.ledger.ui.rules;

import heger.christian.ledger.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;

public class ConfirmDeleteDialog extends DialogFragment {
	protected static final String ARG_URI = "uri";
	
	public static ConfirmDeleteDialog newInstance(Uri uri) {
		ConfirmDeleteDialog fragment = new ConfirmDeleteDialog();
		Bundle args = new Bundle();
		args.putParcelable(ARG_URI, uri);
		fragment.setArguments(args);
		return fragment;
	}
	
	public ConfirmDeleteDialog() {}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstance) {
		final Uri uri = (Uri) getArguments().get(ARG_URI);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.confirm_delete_rule);
		builder.setPositiveButton(R.string.delete, new OnClickListener() {			
			public void onClick(DialogInterface dialog, int which) {
				new AsyncQueryHandler(getActivity().getContentResolver()) {
				}.startDelete(0, null, uri, null, null);
			}
		}).setNegativeButton(R.string.keep, null);
		
		return builder.create();
	}
}
