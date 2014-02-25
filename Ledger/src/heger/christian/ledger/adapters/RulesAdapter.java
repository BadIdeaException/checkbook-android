package heger.christian.ledger.adapters;

import heger.christian.ledger.BuildConfig;
import heger.christian.ledger.R;
import heger.christian.ledger.control.RuleEditorProxy;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.RulesContract;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class RulesAdapter  extends ResourceCursorAdapter { 
	private Context context;
	
	/* View type constant for an item that isn't in edit mode (text fields showing) */
	private static final int TYPE_DISPLAY = 0;
	/* View type constant for an item in edit mode (EditText and Spinner are showing instead of text fields) */
	private static final int TYPE_EDITING = 1;
	
	public static final long NOT_EDITING = AdapterView.INVALID_ROW_ID; 
	
	/**
	 * id of the currently editing item
	 */
	private long editing = NOT_EDITING;
	private RuleEditorProxy proxy;
	
	private static class ViewHolder {
		TextView txtCaption;
		TextView txtCategory;
		EditText editCaption;
		Spinner spinCategory;
		boolean inEditMode = false;
	}
	
	private class CategoriesHelper {
		private final Cursor categories;
		private int idColumn;
		private int captionColumn;
		public CategoriesHelper(Cursor categories) {
			this.categories = categories;
			if (categories != null) {
				idColumn = categories.getColumnIndex(CategoryContract._ID);
				captionColumn = categories.getColumnIndex(CategoryContract.COL_NAME_CAPTION);
			}
		}
		public int moveToId(int id) {
			if (categories != null) {
				categories.moveToPosition(-1);
				while (categories.moveToNext())
					if (categories.getInt(idColumn) == id)
						return categories.getPosition();
			}
			return -1; // id not found
		}
		public String getCaption() {
			return categories != null ? categories.getString(captionColumn) : "";
		}
	}
	private CategoriesHelper categoriesHelper = new CategoriesHelper(null);
	private int idColumn;
	private int captionColumn;
	private int categoryColumn;
	private CursorAdapter categoriesAdapter;	
	private DataSetObserver categoriesObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			categoriesHelper = new CategoriesHelper(categoriesAdapter.getCursor());
		}
		@Override
		public void onInvalidated() {
			categoriesHelper = new CategoriesHelper(null);
		}
	};
	
	/**
	 * Helper class to align an arbitrary target view in such a way that its shown texts 
	 * coincides exactly with the passed source view. 
	 * The source must be a text view. The target must be either a <code>TextView</code> (or a derived class
	 * such as an <code>EditText</code>) or a composite view containing a <code>TextView</code> 
	 * with id <code>android.R.id.text1</code>.
	 * @author chris
	 *
	 */
	private static class ViewAligner {
		int marginLeft, marginTop, marginRight, marginBottom;
		boolean marginsValid = false;
		public void align(final View target, final TextView source) {
			// If margins have never been calculated before, do it now, otherwise use stored values
			if (!marginsValid) { 
				// Defer calculation until first layout. Otherwise, sizes won't be available
				final MarginLayoutParams sourceParams = (MarginLayoutParams) source.getLayoutParams();				
				target.addOnLayoutChangeListener(new OnLayoutChangeListener() {					
					@Override
					public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
						// Try to find textview in target.
						// This can be either the target itself (also works if target is an EditText), or
						// a child of the target with id android.R.id.text1
						try {
							TextView txtTarget = (TextView) target.findViewById(android.R.id.text1);						
							if (txtTarget == null) 
								try {
									txtTarget = (TextView) target;
								} catch (ClassCastException x) {
									throw new IllegalArgumentException("Could not find alignable text field in view " + target + ". Target must either be a TextView, EditText or contain a TextView with id android.R.id.text1",x);
								}

							int paddingLeft = txtTarget.getPaddingLeft() + (target != txtTarget ? target.getPaddingLeft() : 0);
							int paddingTop = txtTarget.getPaddingTop() + (target != txtTarget ? target.getPaddingTop() : 0);
							int paddingRight = txtTarget.getPaddingRight() + (target != txtTarget ? target.getPaddingRight() : 0);
							int paddingBottom = txtTarget.getPaddingBottom() + (target != txtTarget ? target.getPaddingBottom() : 0);
							
							marginLeft = sourceParams.leftMargin - paddingLeft;
							marginTop = sourceParams.topMargin - paddingTop;
							marginRight = sourceParams.rightMargin - paddingRight;
							marginBottom = sourceParams.bottomMargin - paddingBottom;
							marginsValid = true;
							align(target,source);
						} finally {
							target.removeOnLayoutChangeListener(this);
						}
					}
				});
			} else {
				MarginLayoutParams targetParams = (MarginLayoutParams) target.getLayoutParams();
				targetParams.leftMargin = marginLeft;
				targetParams.topMargin = marginTop;
				targetParams.rightMargin = marginRight;
				targetParams.bottomMargin = marginBottom;
				target.requestLayout();
			}
		}
	}
	private ViewAligner editAligner = new ViewAligner();
	private ViewAligner spinnerAligner = new ViewAligner();
	
	public RulesAdapter(Context context, int layout, Cursor c, int flags) {
		super(context, layout, c, flags);
		this.context = context;
		indexColumns(c); 
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {		
		ViewHolder holder = (ViewHolder) view.getTag();		
		if (holder.txtCaption.getVisibility() == View.VISIBLE) {
			holder.txtCaption.setText(cursor.getString(captionColumn));
		}
		
		if (holder.editCaption != null 
				&& holder.editCaption.getVisibility() == View.VISIBLE) {
			holder.editCaption.setText(proxy != null && proxy.wantsOverride() ? proxy.getCaption() : cursor.getString(captionColumn));
		}
			
		int position = categoriesHelper.moveToId(cursor.getInt(categoryColumn));
		if (holder.txtCategory.getVisibility() == View.VISIBLE) {
			holder.txtCategory.setText(categoriesHelper.getCaption());
		}
		
		if (holder.spinCategory != null && holder.spinCategory.getVisibility() == View.VISIBLE) {
			if (proxy != null && proxy.wantsOverride())
				position = categoriesHelper.moveToId(proxy.getCategory());
			holder.spinCategory.setSelection(position);
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String log;
		if (BuildConfig.DEBUG)
			log = "Getting view for position " + position 
				+ " of type " + (getItemViewType(position) == TYPE_EDITING ? "TYPE_EDITING" : "TYPE_DISPLAY") 
				+ " with convert view " + (convertView == null ? "null" : convertView);
		
		if (convertView == null) {
			convertView = newView(context, getCursor(), parent);
		}

		ViewHolder holder;
		if (convertView.getTag() == null) {
			holder = new ViewHolder();
			holder.txtCaption = (TextView) convertView.findViewById(R.id.txt_caption);
			holder.txtCategory = (TextView) convertView.findViewById(R.id.txt_category);
			convertView.setTag(holder);
		} else
			holder = (ViewHolder) convertView.getTag();

		switch (getItemViewType(position)) {
			case TYPE_EDITING:
				// Has the view already been put into edit mode?
				if (!holder.inEditMode) {
					// Inflate view stubs if not already inflated
					if (holder.editCaption == null) 
						holder.editCaption = (EditText) ((ViewStub) convertView.findViewById(R.id.edit_caption)).inflate();						
					if (holder.spinCategory == null) {
						holder.spinCategory = (Spinner) ((ViewStub) convertView.findViewById(R.id.spin_category)).inflate();
						holder.spinCategory.setAdapter(categoriesAdapter);
					}
					editAligner.align(holder.editCaption, holder.txtCaption);
					spinnerAligner.align(holder.spinCategory, holder.txtCategory);
					holder.txtCaption.setVisibility(View.GONE);
					holder.txtCategory.setVisibility(View.GONE);
					holder.editCaption.setVisibility(View.VISIBLE);
					holder.editCaption.requestFocus();
					holder.spinCategory.setVisibility(View.VISIBLE);
					holder.inEditMode = true;
				}
				break;
			case TYPE_DISPLAY:
				if (holder.inEditMode) {
					holder.editCaption.setVisibility(View.GONE);
					holder.spinCategory.setVisibility(View.GONE);
					holder.txtCaption.setVisibility(View.VISIBLE);
					holder.txtCategory.setVisibility(View.VISIBLE);
					holder.inEditMode = false;		
				}
				break;
		}
		
		Cursor cursor = getCursor();
		cursor.moveToPosition(position);
		bindView(convertView, context, cursor);
		
		if (BuildConfig.DEBUG) Log.d("RulesAdapter", log + ": " + convertView);
		return convertView;
	}
	
	@Override
	public int getItemViewType(int position) {
		long id = getItemId(position);
		return id != editing ? TYPE_DISPLAY : TYPE_EDITING; 
	}
	
	@Override
	public int getViewTypeCount() { 
		return 2;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	private void indexColumns(Cursor cursor) {
		if (cursor != null) {
			idColumn = cursor.getColumnIndex(RulesContract._ID);
			captionColumn = cursor.getColumnIndex(RulesContract.COL_NAME_ANTECEDENT);
			categoryColumn = cursor.getColumnIndex(RulesContract.COL_NAME_CONSEQUENT);
		}		
	}
	
	@Override
	public Cursor swapCursor(Cursor cursor) {
		Cursor result = super.swapCursor(cursor);
		indexColumns(cursor);
		return result;	
	}
	
	@Override
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		indexColumns(cursor);
	}
	
	public void setCategoryAdapter(CursorAdapter categories) {
		if (categories != null) {
			categoriesHelper = new CategoriesHelper(categories.getCursor());
			categories.registerDataSetObserver(categoriesObserver);
		} else {
			if (categoriesAdapter != null)
				categoriesAdapter.unregisterDataSetObserver(categoriesObserver);
			categoriesHelper = new CategoriesHelper(null);
		}
		categoriesAdapter = categories;		
	}
	
	public long getEditingId() {
		return editing;
	}
	
	public void setEditingId(long id) {
		editing = id;
	}
	
	@Override
	public long getItemId(int position) {
		Cursor cursor = getCursor();
		cursor.moveToPosition(position);
		return cursor.getLong(idColumn);
	}

	/**
	 * Returns the position within the adapter for the item associated with the passed id.
	 * @return The item position in the adapter, or -1 if the id doesn't correspond to any
	 * item in the adapter. If the return result is greater than zero, the following holds:
	 * <code>getItemId(getItemPosition(id)) == id</code>
	 */
	public int getItemPosition(long id) {
		for (int position = 0; position < getCount(); position++) {
			if (getItemId(position) == id)
				return position;			
		}
		return -1;
	}
	
	public RuleEditorProxy getProxy() {
		return proxy;
	}

	public void setProxy(RuleEditorProxy proxy) {
		this.proxy = proxy;
	}
}
