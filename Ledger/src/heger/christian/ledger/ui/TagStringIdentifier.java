package heger.christian.ledger.ui;

import android.view.View;
import heger.christian.ledger.control.TreeEmulator.Identifier;

/** 
 * A simple <code>Identifier</code> that takes a view's tag
 * and converts it to a string using the tag's <code>toString()</code> method.
 * @author chris
 *
 */
public class TagStringIdentifier implements Identifier {

	public TagStringIdentifier() {
	}

	/**
	 * Returns the string representation of the passed view's tag as 
	 * determined by the tag's <code>toString()</code> method.
	 * If the view has no tag, this method returns <code>null</code>.
	 */
	public Object identify(View view) {
		Object tag = view.getTag();
		return tag != null ? tag.toString() : null;
	}

}
