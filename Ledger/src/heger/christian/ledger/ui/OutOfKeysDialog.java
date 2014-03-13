package heger.christian.ledger.ui;

import heger.christian.ledger.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class OutOfKeysDialog extends DialogFragment {
	public static OutOfKeysDialog newInstance() {
		return new OutOfKeysDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstance) {
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.out_of_keys_title)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.out_of_keys_message)
				.setPositiveButton(android.R.string.ok, null).create();
	}
}
