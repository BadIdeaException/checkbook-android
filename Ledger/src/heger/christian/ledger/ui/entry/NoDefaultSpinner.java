package heger.christian.ledger.ui.entry;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class NoDefaultSpinner extends Spinner {
	private SpinnerAdapter adapter;
	
	public NoDefaultSpinner(Context context) {
		super(context);
	}

	public NoDefaultSpinner(Context context, int mode) {
		super(context, mode);
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs, int defStyle,
			int mode) {
		super(context, attrs, defStyle, mode);
	}

	
}
