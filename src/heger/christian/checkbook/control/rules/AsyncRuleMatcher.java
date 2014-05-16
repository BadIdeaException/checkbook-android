package heger.christian.checkbook.control.rules;


import java.util.Map;

import android.content.ContentResolver;
import android.os.AsyncTask;

/**
 * Simple extension of {@link RuleMatcher} that performs the actual matching in an <code>AsyncTask</code> and posts
 * the results through a {@link OnRuleMatchingCompleteListener} callback interface.
 * @author chris
 *
 */
public class AsyncRuleMatcher extends RuleMatcher {
	public AsyncRuleMatcher(ContentResolver resolver) {
		super(resolver);
	}

	public interface OnRuleMatchingCompleteListener {
		public void onRuleMatchingComplete(Map<Integer,Integer> matchResult);
	}
	private OnRuleMatchingCompleteListener listener;
	
	/**
	 * Begins an asynchronous strict match and returns <code>null</code>. If an 
	 * <code>OnRuleMatchingCompleteListener</code> is set, it will be called when
	 * matching is completed.
	 * @return <code>null</code>
	 */
	@Override
	public Map<Integer,Integer> matchStrict(String phrase) {
		if (phrase != null && phrase != "") {
			new AsyncTask<String,Object,Map<Integer,Integer>>() {
				@Override
				protected Map<Integer,Integer> doInBackground(String... params) {
					return AsyncRuleMatcher.super.matchStrict(params[0]);
				}
				@Override
				protected void onPostExecute(Map<Integer,Integer> matchResult) {
					if (getOnRuleMatchingCompleteListener() != null)
						getOnRuleMatchingCompleteListener().onRuleMatchingComplete(matchResult);
				}
			}.execute(phrase);
		}
		return null;
	}

	public OnRuleMatchingCompleteListener getOnRuleMatchingCompleteListener() {
		return listener;
	}

	public void setOnRuleMatchingCompleteListener(OnRuleMatchingCompleteListener listener) {
		this.listener = listener;
	}
}
