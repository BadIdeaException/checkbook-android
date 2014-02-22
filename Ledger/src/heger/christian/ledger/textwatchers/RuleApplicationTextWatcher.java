package heger.christian.ledger.textwatchers;

import heger.christian.ledger.control.rules.RuleMatcher;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;

public class RuleApplicationTextWatcher implements TextWatcher {
	// Delay start of rule matching by 1500ms to avoid constantly changing categories as the user types
	protected static final int START_DELAY = 1500; 
	
	private RuleMatcher matcher;
	
	protected class MatchCallback implements Runnable {
		String phrase;
		public void run() {
			if (getRuleMatcher() != null)
				getRuleMatcher().matchStrict(phrase);
		}
	}
	private MatchCallback callback = new MatchCallback();
	
	private Handler handler = new Handler();
	
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	public void afterTextChanged(Editable s) {
		handler.removeCallbacks(callback);
		callback.phrase = s.toString();
		handler.postDelayed(callback, START_DELAY);
	}

	public RuleMatcher getRuleMatcher() {
		return matcher;
	}

	public void setRuleMatcher(RuleMatcher matcher) {
		this.matcher = matcher;
	}

}
