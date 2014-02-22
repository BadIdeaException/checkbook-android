package heger.christian.ledger.control;

import heger.christian.ledger.R;
import heger.christian.ledger.model.CategoryParameterObject;
import heger.christian.ledger.model.EntryParameterObject;
import heger.christian.ledger.model.SupercategoryParameterObject;
import heger.christian.ledger.ui.CurrencyValueFormatter;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Standard table row factory. It contains three sub factories for
 * supercategories, categories, and entries, respectively, to which it
 * defers the actual creation of table rows. <br>
 * <br>
 * Default implementations of these sub factories are available as nested
 * classes, but these can be switched at runtime by calling the appropriate
 * setFactory methods.
 * @author chris
 *
 */
public class TableRowFactory extends AbstractTableRowFactory {
	/**
	 * Default table row factory for creating table rows appropriate to 
	 * supercategory entries. It works by inflating the <code>tablerow_template_supercategory</code>.
	 * @author chris
	 */
	protected class DefaultSupercategoryTableRowFactory extends AbstractTableRowFactory {
		public DefaultSupercategoryTableRowFactory(Context context) { super(context); }
		@Override
		public TableRow createRow(Object params) {
 			SupercategoryParameterObject sc = (SupercategoryParameterObject) params;
			// Obtain the current layout inflater
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	// Inflate the row template for supercategories to the table row
			TableRow row = (TableRow) inflater.inflate(R.layout.tablerow_template_supercategory, null);
	    	// Find the caption/revenue/expense text views and set their respective texts
			((TextView) row.findViewById(R.id.txt_caption)).setText(sc.caption);
	    	return row;
		}
	}
	/**
	 * Default table row factory for creating table rows appropriate to 
	 * category entries. It works by inflating the <code>tablerow_template_category</code>.
	 * @author chris
	 */
	protected class DefaultCategoryTableRowFactory extends AbstractTableRowFactory {
		public DefaultCategoryTableRowFactory(Context context) { super(context); }
		@Override
		public TableRow createRow(Object params) {
 			CategoryParameterObject cat = (CategoryParameterObject) params;
			// Obtain the current layout inflater
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	// Inflate the row template for categories to the table row
			TableRow row = (TableRow) inflater.inflate(R.layout.tablerow_template_category, null);
	    	// Find the caption/revenue/expense text views and set their respective texts
			((TextView) row.findViewById(R.id.txt_caption)).setText(cat.caption);
	    	return row;
		}
	}
	/**
	 * Default table row factory for creating table rows appropriate to 
	 * entries. It works by inflating the <code>tablerow_template_entry</code>.
	 * @author chris
	 */
	protected class DefaultEntryTableRowFactory extends AbstractTableRowFactory {
		public DefaultEntryTableRowFactory(Context context) { super(context); }
		@Override
		public TableRow createRow(Object params) {
			EntryParameterObject entry = (EntryParameterObject) params;
 			// Obtain the current layout inflater
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	// Inflate the row template for entries to the table row
			TableRow row = (TableRow) inflater.inflate(R.layout.tablerow_template_entry, null);
	    	// Find the caption/revenue/expense text views and set their respective texts
			((TextView) row.findViewById(R.id.txt_caption)).setText(entry.caption);
	    	int revenue = (entry.value > 0 ? entry.value : 0);
	    	((TextView) row.findViewById(R.id.txt_revenue)).setText(revenue > 0 ? CurrencyValueFormatter.format(revenue) : "");
	    	int expense = (entry.value < 0 ? -entry.value : 0);
	    	((TextView) row.findViewById(R.id.txt_expense)).setText(expense > 0 ? CurrencyValueFormatter.format(expense) : "");
	    	return row;
		}
	}

	private AbstractTableRowFactory supercategoryTableRowFactory;
	private AbstractTableRowFactory categoryTableRowFactory;
	private AbstractTableRowFactory entryTableRowFactory;
	
	public TableRowFactory(Context context) {
		super(context);
		setSupercategoryTableRowFactory(new DefaultSupercategoryTableRowFactory(context));
		setCategoryTableRowFactory(new DefaultCategoryTableRowFactory(context));
		setEntryTableRowFactory(new DefaultEntryTableRowFactory(context));
	}
	
	/**
	 * Creates a <code>TableRow</code> for the passed parameter object.
	 * Based on the type of the passed parameter object, the call is 
	 * deferred to the appropriate sub factory.
	 * @param params - the parameters from which to create a <code>TableRow</code>
	 * Must be of type <code>SupercategoryParameterObject</code>, 
	 * <code>CategoryParameterObject</code> or <code>EntryParameterObject</code>.
	 * @return The created <code>TableRow</code>
	 * @throws IllegalArgumentException - If the parameter object has an 
	 * unrecognized type
	 */
	@Override
	public TableRow createRow(Object params) {
		if (params instanceof SupercategoryParameterObject)
			return supercategoryTableRowFactory.createRow(params);
		if (params instanceof CategoryParameterObject)
			return categoryTableRowFactory.createRow(params);
		if (params instanceof EntryParameterObject)
			return entryTableRowFactory.createRow(params);
		throw new IllegalArgumentException("Unknown parameter object type: " + params);
	}

	public AbstractTableRowFactory getSupercategoryTableRowFactory() {
		return supercategoryTableRowFactory;
	}

	public void setSupercategoryTableRowFactory(
			AbstractTableRowFactory supercategoryTableRowFactory) {
		this.supercategoryTableRowFactory = supercategoryTableRowFactory;
	}

	public AbstractTableRowFactory getCategoryTableRowFactory() {
		return categoryTableRowFactory;
	}

	public void setCategoryTableRowFactory(AbstractTableRowFactory categoryTableRowFactory) {
		this.categoryTableRowFactory = categoryTableRowFactory;
	}

	public AbstractTableRowFactory getEntryTableRowFactory() {
		return entryTableRowFactory;
	}

	public void setEntryTableRowFactory(AbstractTableRowFactory entryTableRowFactory) {
		this.entryTableRowFactory = entryTableRowFactory;
	}
}
