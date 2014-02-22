package heger.christian.ledger.control;

import android.content.Context;
import android.widget.TableRow;

public abstract class AbstractTableRowFactory {
	/* TODO Should this be a weak reference? Going with a strong one is clearly
	 * the safe choice, but is there any scenario where it could keep the 
	 * android system from doing whatever it does when contexts are no longer
	 * useful?  
	 */
	private final Context context;

	/**
	 * Creates a new table factory that will use the passed context to
	 * create any views.
	 * @param context - The context to use when creating views.
	 * This parameter must not be <code>null</code>, otherwise, an
	 * <code>IllegalArgumentException</code> will be thrown.
	 */
	public AbstractTableRowFactory(Context context) {
		if (context == null) 
			throw new IllegalArgumentException("Cannot create table row factory with a null context.");
		this.context = context;
	}

	/** 
	 * Creates a table row appropriate in style to the passed
	 * entry and populates it with the entry's data.
	 * @param params - The parameter object holding the data 
	 * with which to create a row. Must be of type <code>SupercategoryParameterObject</code>,
	 * <code>CategoryParameterObject</code> or <code>EntryParameterObject</code>
	 * @return The created table row.
	 * @throws IllegalArgumentException - If the parameter object is of an
	 * unrecognized type
	 */
	public abstract TableRow createRow(Object params);

	/**
	 * Gets the context this factory uses to create its table rows.
	 * @return The context for creating views.
	 */
	public Context getContext() {
		return context;
	}
}
