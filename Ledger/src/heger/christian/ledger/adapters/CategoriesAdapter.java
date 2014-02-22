package heger.christian.ledger.adapters;

import heger.christian.ledger.R;
import heger.christian.ledger.control.CategoryEditorProxy;
import heger.christian.ledger.providers.CategoryContract;
import heger.christian.ledger.providers.SupercategoryContract;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;


public class CategoriesAdapter extends ResourceCursorTreeAdapter {

	private static final int TYPE_CHILD_DISPLAY = 0;
	private static final int TYPE_CHILD_HEADER = 1;
	private static final int TYPE_CHILD_EDITING = 2;
	private static final int TYPE_GROUP_DISPLAY = 0;
	private static final int TYPE_GROUP_EDITING = 1;

	private static final long SECTION_HEADER_ID = Long.MIN_VALUE;
	
	public static final long NOT_EDITING = ExpandableListView.INVALID_ROW_ID; 
	
	private Context context;

	private static class GroupViewHolder {
		TextView txtCaption;
		EditText editCaption;
		boolean inEditMode = false;
	}
	private static class ChildViewHolder extends GroupViewHolder {};
	
	private int groupIdColumn = -1;
	private int groupCaptionColumn = -1;
	
	private int childIdColumn = -1;
	private int childCaptionColumn = -1;
	
	private long editingCombinedId = NOT_EDITING;
	private CategoryEditorProxy proxy;
	
	public CategoriesAdapter(Context context, Cursor cursor, int groupLayout, int childLayout) {
		super(context, cursor, groupLayout, childLayout);
		if (cursor != null)
			indexGroupColumns(cursor);
		this.context = context;
	}

	private void indexGroupColumns(Cursor sample) {
		groupIdColumn = sample.getColumnIndex(SupercategoryContract._ID);
		groupCaptionColumn = sample.getColumnIndex(SupercategoryContract.COL_NAME_CAPTION);
	}
	
	private void indexChildColumns(Cursor sample) {
		childIdColumn = sample.getColumnIndex(CategoryContract._ID);
		childCaptionColumn = sample.getColumnIndex(CategoryContract.COL_NAME_CAPTION);
	}
	
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		long sc = groupCursor.getLong(groupIdColumn);
		Cursor cursor = context.getContentResolver().query(CategoryContract.CONTENT_URI, 
				null, 
				CategoryContract.COL_NAME_SUPERCATEGORY + "=" + sc, 
				null, 
				CategoryContract._ID);
		
		if (cursor != null)
			indexChildColumns(cursor);
		return cursor;			
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	/**
	 * This implementation uses the group cursor's _id column 
	 */
	@Override
	public long getGroupId(int groupPosition) {
		Cursor group = getGroup(groupPosition);
		return group.getLong(groupIdColumn);
	}
	
	public boolean isChildId(long combinedId) {
		return (0x8000000000000000l & combinedId) != 0;
	}
	
	/**
	 * Extracts the group id from the passed combined id. 
	 * This method will produce incorrect results on group ids that are longer than 31 bit. 
	 * @param combinedId - The combined id from which to extract the group id
	 * @return The extracted group id.
	 * @see BaseExpandableListAdapter#getCombinedGroupId(long)
	 */
	public long extractGroupId(long combinedId) {
		return (0x7FFFFFFF00000000l & combinedId) >> 32; 
	}
	
	/**
	 * This implementation uses the child cursor's _id column 
	 */
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		if (childPosition == 0) // Not calling getChildType here to avoid infinite loop
			return SECTION_HEADER_ID;
		
		Cursor child = getChild(groupPosition, childPosition);
		return child.getLong(childIdColumn);
	}
	
	/**
	 * Extracts the child id from the passed combined id. 
	 * This method will produce incorrect results on child ids that are longer than 32 bit. 
	 * @param combinedId - The combined id from which to extract the child id
	 * @return The extracted child id, or {@link ExpandableListView#INVALID_ROW_ID} if the
	 * passed combined id corresponds directly to a group.
	 * @see BaseExpandableListAdapter#getCombinedChildId(long)
	 */
	public long extractChildId(long combinedId) {
		if ((0x8000000000000000l & combinedId) == 0) {
			return ExpandableListView.INVALID_ROW_ID; // This was a group
		}
		return (0x00000000FFFFFFFFl & combinedId);
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return 1 + super.getChildrenCount(groupPosition);
	}
	
	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
		GroupViewHolder holder = (GroupViewHolder) view.getTag();		
		if (holder.txtCaption.getVisibility() == View.VISIBLE) {
			holder.txtCaption.setText(cursor.getString(groupCaptionColumn));
		}
		
		if (holder.editCaption != null && holder.editCaption.getVisibility() == View.VISIBLE) {
			holder.editCaption.setText(proxy != null && proxy.wantsOverride() ? proxy.getCaption() : cursor.getString(groupCaptionColumn));
		}	
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		ChildViewHolder holder = (ChildViewHolder) view.getTag();		
		if (holder.txtCaption.getVisibility() == View.VISIBLE) {
			holder.txtCaption.setText(cursor.getString(childCaptionColumn));
		}
		
		if (holder.editCaption != null && holder.editCaption.getVisibility() == View.VISIBLE) {
			holder.editCaption.setText(proxy != null && proxy.wantsOverride() ? proxy.getCaption() : cursor.getString(childCaptionColumn));
		}
				
		TextView text = (TextView) view.findViewById(R.id.txt_caption);
		text.setText(cursor.getString(childCaptionColumn));
	}
	
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		if (getChildType(groupPosition, childPosition) == TYPE_CHILD_HEADER) 
			return getSectionHeader();
			
		Cursor childCursor = getChild(groupPosition,childPosition);		
		
		ChildViewHolder holder;
		if (convertView == null) {
			convertView = newChildView(context, childCursor, isLastChild, parent);
			holder = new ChildViewHolder();
			holder.txtCaption = (TextView) convertView.findViewById(R.id.txt_caption);
			convertView.setTag(holder);
		} else
			holder = (ChildViewHolder) convertView.getTag();
		
		convertView.setPadding(context.getResources().getDimensionPixelOffset(R.dimen.expandable_list_child_indentation), 
					convertView.getPaddingTop(), convertView.getPaddingRight(), convertView.getPaddingBottom());
		
		switch (getChildType(groupPosition, childPosition)) {
			case TYPE_CHILD_DISPLAY: 
				if (holder.inEditMode) {
					holder.editCaption.setVisibility(View.GONE);
					holder.txtCaption.setVisibility(View.VISIBLE);
					holder.inEditMode = false;		
				}
				break;
			case TYPE_CHILD_EDITING: 
				if (!holder.inEditMode) {
					// Inflate view stubs if not already inflated
					if (holder.editCaption == null) 
						holder.editCaption = (EditText) ((ViewStub) convertView.findViewById(R.id.edit_caption)).inflate();						
					
//					editAligner.align(holder.editCaption, holder.txtCaption);
					holder.txtCaption.setVisibility(View.GONE);
					holder.editCaption.setVisibility(View.VISIBLE);
					holder.editCaption.requestFocus();
					holder.inEditMode = true;
				}
				break;				
		}
		bindChildView(convertView, context, childCursor, isLastChild);
		return convertView;		
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		Cursor groupCursor = getGroup(groupPosition);
		
		GroupViewHolder holder;
		if (convertView == null) {
			convertView = newGroupView(context, groupCursor, isExpanded, parent);
			holder = new GroupViewHolder();
			holder.txtCaption = (TextView) convertView.findViewById(R.id.txt_caption);
			convertView.setTag(holder);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}
		
		switch (getGroupType(groupPosition)) {
			case TYPE_GROUP_DISPLAY:
				if (holder.inEditMode) {
					holder.editCaption.setVisibility(View.GONE);
					holder.txtCaption.setVisibility(View.VISIBLE);
					holder.inEditMode = false;		
				}
				break;
			case TYPE_GROUP_EDITING:
				if (!holder.inEditMode) {
					// Inflate view stubs if not already inflated
					if (holder.editCaption == null) 
						holder.editCaption = (EditText) ((ViewStub) convertView.findViewById(R.id.edit_caption)).inflate();						
					
//					editAligner.align(holder.editCaption, holder.txtCaption);
					holder.txtCaption.setVisibility(View.GONE);
					holder.editCaption.setVisibility(View.VISIBLE);
					holder.editCaption.requestFocus();
					holder.inEditMode = true;
				}
				break;				
		}
		bindGroupView(convertView, context, groupCursor, isExpanded);
		return convertView;
	}
	
	private View getSectionHeader() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		View header = inflater.inflate(R.layout.add_category, null);
		header.setPadding(context.getResources().getDimensionPixelOffset(R.dimen.expandable_list_child_indentation), 
				header.getPaddingTop(), header.getPaddingRight(), header.getPaddingBottom());
		return header;
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		if (childPosition == 0)
			return TYPE_CHILD_HEADER;
		
		return getCombinedChildId(getGroupId(groupPosition), getChildId(groupPosition, childPosition)) == getEditingCombinedId() ?
				TYPE_CHILD_EDITING : TYPE_CHILD_DISPLAY;
	}
	
	@Override
	public int getChildTypeCount() {
		return 3;
	}
	
	@Override
	public int getGroupType(int groupPosition) {
		return getCombinedGroupId(getGroupId(groupPosition)) == getEditingCombinedId() ?
				TYPE_GROUP_EDITING : TYPE_GROUP_DISPLAY;
	}
	
	@Override 
	public int getGroupTypeCount() {
		return 2;
	}
	
	@Override
	public Cursor getChild(int groupPosition, int childPosition) {
		if (childPosition == 0)
			throw new IllegalArgumentException("Tried to get child cursor for section header");
		
		return super.getChild(groupPosition, childPosition - 1);
	}
	
	@Override
	public void setChildrenCursor(int groupPosition, Cursor childrenCursor) {
		if (childrenCursor != null)
			indexChildColumns(childrenCursor);
		super.setChildrenCursor(groupPosition, childrenCursor);
	}
	
	@Override
	public void setGroupCursor(Cursor cursor) {
		if (cursor != null)
			indexGroupColumns(cursor);
		super.setGroupCursor(cursor);
	}

	public long getEditingCombinedId() {
		return editingCombinedId;
	}

	public void setEditingCombinedId(long editingCombinedId) {
		this.editingCombinedId = editingCombinedId;
	}

	public void setProxy(CategoryEditorProxy proxy) {
		this.proxy = proxy;
	}
}
