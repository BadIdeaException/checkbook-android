package heger.christian.ledger.adapters;

import android.view.View;

public interface Editorizer {
	public interface EditorizerListener {
		public void onEditorize(View view);
		public void onUneditorize(View view);
	}
	
	public abstract void editorize(View view);
	public abstract void setEditorData(View view);
	public abstract void uneditorize(View view);
	public abstract void setDisplayData(View view);
}
