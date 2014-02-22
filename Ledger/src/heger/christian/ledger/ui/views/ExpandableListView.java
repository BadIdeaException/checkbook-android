package heger.christian.ledger.ui.views;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class ExpandableListView extends ViewGroup  {
	private ExpandableListAdapter adapter;
	
	private ScrollView scrollview;
	private LinearLayout layout;

	/**
	 * Helper method to make consistent use of adapter IDs easier. <code>ExpandableListAdapter</code> actually
	 * defines four different id types:
	 * <ol>
	 * <li> {@link ExpandableListAdapter#getGroupId(int)}
	 * <li> {@link ExpandableListAdapter#getChildId(int, int)}
	 * <li> {@link ExpandableListAdapter#getCombinedGroupId(long)}
	 * <li> {@link ExpandableListAdapter#getCombinedChildId(long, long))}
	 * </ol>
	 * This method serves as a single point of access to adapter IDs to make keeping the use of IDs consistent easier. 
	 * @param groupPosition - The position of the group to get the ID for
	 * @param childPosition - The position of the child within the group to get the ID for. Maybe -1, in which case
	 * the ID for the group will be returned.
	 * @return The appropriate combined ID depending on whether the passed position corresponds to a group or a child.
	 */
	private long getID(int groupPosition, int childPosition) {
		long groupID = adapter.getGroupId(groupPosition);
		if (childPosition == -1) 
			return adapter.getCombinedGroupId(groupID);
		else {
			long childID = adapter.getChildId(groupPosition, childPosition);
			return adapter.getCombinedChildId(groupID, childID);
		}
	}
	
	/**
	 * Convenience for <code>getID(groupPosition, -1)</code>.
	 */
	private long getID(int groupPosition) {
		return getID(groupPosition, -1);
	}
	
	/** 
	 * This class provides methods to calculate the index corresponding to a given
	 * combination of group and child positions and vice versa.
	 * <p>
	 * As such calculations are expected to frequently be done from within a loop,
	 * it preserves the results of the last known such combination, i.e. after
	 * each calculation, it remembers the index, group and child position as a 
	 * basis for the next calculation.
	 * @author chris
	 *
	 */
	private class HierarchyHelper {
		int lastIndex;
		int lastGroupPosition;
		int lastChildPosition;
		boolean valid = false;
		
		public void moveToBeginning() {
			lastIndex = getGroupHeaderCount();
			lastGroupPosition = 0;
			lastChildPosition = -1;
			valid = true;
		}
		
		public void moveToEnd() {
			lastGroupPosition = adapter.getGroupCount() - 1;
			lastIndex = layout.getChildCount() - 1 - getGroupFooterCount();
			if (isCreated(getID(lastGroupPosition))) {
				lastChildPosition = adapter.getChildrenCount(lastGroupPosition) - 1;
				lastIndex -= getChildFooterCount();
			} else {
				lastChildPosition = -1;
			}
			valid = true;
		}
		
		/**
		 * Marks the remembered combination of index, group and child position
		 * as invalid. The next calculation will not use those remembered values,
		 * instead starting from the beginning. 
		 */
		public void invalidate() {
			valid = false;
		}
		
		/**
		 * Translates the passed group and child
		 * positions into the index under which the view corresponding
		 * to those positions may be accessed in the list.
		 * <p>
		 * For performance reasons, no checks are performed as to whether
		 * the passed positions are actually valid positions within the 
		 * adapter. 
		 * @param groupPosition - The position of the group 
		 * @param childPosition - The position of the child. May be
		 * -1, in which case the result will correspond to the 
		 * view corresponding to the passed group. 
		 * @return The index of the view 
		 */
		public int calculateIndex(final int groupPosition, final int childPosition) {
			/* Conceptually, the calculation of the index corresponding with the requested  
			 * positions is divided into three parts:
			 * 1. Leaving the subtree we're in at the time of the call and going in the 
			 *    prescribed direction to the next group node
			 * 2. Traveling on the group level until we reach the requested group
			 * 3. Entering this group and going to the requested child
			 */

			if (!valid)
				moveToBeginning();
			
			// <0 if going backward, 0 if requested positions match remembered positions, >0 if going forward
			int direction = groupPosition - lastGroupPosition; 
			if (direction == 0)
				direction = childPosition - lastChildPosition;

			int H = getChildHeaderCount();
			int F = getChildFooterCount();
			
			if (direction > 0) {
				while (lastGroupPosition != groupPosition || lastChildPosition != childPosition) {
					// If we've reached the requested group, go to the requested child and break the loop
					if (lastGroupPosition == groupPosition) {
						lastIndex += childPosition - lastChildPosition;
						if (lastChildPosition == -1)
							lastIndex += H;
						lastChildPosition = childPosition;
						break;
					}
					// If we're not on the group level, skip ahead to the next group row					
					if (lastChildPosition != -1) {
						lastIndex += adapter.getChildrenCount(lastGroupPosition) - lastChildPosition + F;
						lastGroupPosition++;
						lastChildPosition = -1;
						// We've skipped ahead to the next group, continue because this might be the one
						continue;
					}
					// Go one step on the group level in the right direction,
					// accounting for children as necessary
					if (isCreated(getID(lastGroupPosition))) {
						lastIndex += adapter.getChildrenCount(lastGroupPosition) + H + F;
					}
					lastIndex++;
					lastGroupPosition++;
				}
			} else if (direction < 0) {
				while (lastGroupPosition != groupPosition || lastChildPosition != childPosition) {
					// Can only be the case when requested and remembered child are within the same group
					if (lastGroupPosition == groupPosition) {
						lastIndex -= lastChildPosition - childPosition;
						if (childPosition == -1)
							lastIndex -= H;
						lastChildPosition = childPosition;
						break;
					}
					// If we're not on the group level, go backwards to the last group row.
					// This is always the group that the current child belongs to
					if (lastChildPosition != -1) {
						lastIndex -= lastChildPosition + 1 + H;
						lastChildPosition = -1;
						continue; 
					}
					// If the next group is the requested one, go to the requested child and break the loop
					if (lastGroupPosition == groupPosition + 1) {
						lastGroupPosition--;
						int childrenCount = isCreated(getID(lastGroupPosition)) ? adapter.getChildrenCount(lastGroupPosition) : 0;
						lastIndex -= childrenCount > 0 ? childrenCount - childPosition + F : 1; 
						if (childPosition == -1)
							lastIndex -= H;
						lastChildPosition = childPosition;
						break;
					}
					// Go one step on the group level in the right direction,
					// accounting for children as necessary
					lastGroupPosition--;
					lastIndex--;
					if (isCreated(getID(lastGroupPosition))) {
						lastIndex -= adapter.getChildrenCount(lastGroupPosition) + H + F;
					}
				}
			}	
			return lastIndex;
		}

		/**
		 * Translates the passed group and child
		 * positions into the index under which the view corresponding
		 * to those positions may be accessed in the list.
		 * <p>
		 * For performance reasons, no checks are performed as to whether
		 * the passed positions are actually valid positions within the 
		 * adapter. 
		 * @param groupPosition - The position of the group 
		 * @param childPosition - The position of the child. May be
		 * -1, in which case the result will correspond to the 
		 * view corresponding to the passed group. 
		 * @return The index of the view 
		 */
		public int[] calculatePositions(final int index) {
			// Check if requested index falls within the group header or footer rows
			if (index < getGroupHeaderCount()) {
				moveToBeginning();
				return null;
			} else if (index >= layout.getChildCount() - getGroupFooterCount()) {
				moveToEnd();
				return null;
			}	

			if (!valid)
				moveToBeginning();

			int delta = index - lastIndex;
			int H = getChildHeaderCount();
			int F = getChildFooterCount();
			
						
			int childrenCount;
			boolean created = isCreated(getID(lastGroupPosition));
			if (created)
				childrenCount = adapter.getChildrenCount(lastGroupPosition);
			else
				childrenCount = 0;
			while (delta > 0) {
				// Case 1: Within subgroup, leave subgroup
				if (lastChildPosition != -1 && delta > childrenCount - (lastChildPosition + 1) + F) {
					delta -= childrenCount - lastChildPosition + F; 
					lastChildPosition = -1;
					lastGroupPosition++;
					created = isCreated(getID(lastGroupPosition));
					childrenCount = created ? adapter.getChildrenCount(lastGroupPosition) : 0;
				}
				// Case 2: Travel on the group level
				if (lastChildPosition == -1 && delta > (created ? childrenCount + H + F : 0)) {
					delta--;
					lastGroupPosition++;
					if (created)
						delta -= childrenCount + H + F;
					created = isCreated(getID(lastGroupPosition));
					childrenCount = created ? adapter.getChildrenCount(lastGroupPosition) : 0;
				}
				// Case 3: Enter subgroup to the first actual child if one exists. 
				if (lastChildPosition == -1 && delta <= (created ? childrenCount + H + F : 0)) {
					if (delta <= H) {
						// Requested index is a child header, or delta was already 0
						break;
					}
					if (childrenCount == 0) {
						// We're in the correct subgroup, but it has no children => 
						// requested index was a footer
						break;
					}
					delta -= H + 1;
					lastChildPosition = 0;
				}
				// Case 4: We're within the right subgroup, go to the requested index
				if (lastChildPosition != -1 && delta <= childrenCount - (lastChildPosition + 1) + F) {
					if (delta <= childrenCount - (lastChildPosition + 1)) {
						lastChildPosition += delta;
						delta = 0;
					}
					// At this point either delta = 0 or childrenCount < delta => requested index is a footer					
					break;
				}
			}

			if (lastGroupPosition > 0) {
				created = isCreated(getID(lastGroupPosition - 1));
				childrenCount = created ? adapter.getChildrenCount(lastGroupPosition - 1) : 0;
			} else
					// lastGroupPosition == 1 => either we're on the very first group row, or we're on some child
					// within the first group and the requested index falls within the first group as 
					// well. If the former is the case, delta equals zero by now and the loop will exit
					// on the case 3 condition (see below)
					// In the latter case, setting childrenCount to Integer.MAX_VALUE - H - F ensures
					// that the two following cases - traveling on the group level and 
					// entering subgroup (we're already in the subgroup) - fall through and we go into the last
					// remaining case: travel in subgroup.
					childrenCount = Integer.MAX_VALUE - H - F;			
			
			while (delta < 0) {
				// Case 1: Within subgroup, leave subgroup
				if (lastChildPosition != -1 && delta < -(lastChildPosition + H)) {
					delta += lastChildPosition + 1 + H;
					lastChildPosition = -1;
				}
				// Case 2: Travel on the group level
				if (lastChildPosition == -1 && delta < (created ? -(childrenCount + H + F) : 0)) {
					delta++;
					lastGroupPosition--;
					if (created)
						delta += childrenCount + H + F;		
					if (lastGroupPosition > 0) {
						created = isCreated(getID(lastGroupPosition - 1));
						childrenCount =  created ? adapter.getChildrenCount(lastGroupPosition - 1) : 0;				
					} else
						// At this point, this can only mean we're on the very first group row
						// Either delta is 0 by now, then the loop needs to terminate anyway, or it isn't, then
						// we need to terminate to return null and move lastIndex to a valid position
						break;
				}
				// Case 3: Enter subgroup
				if (lastChildPosition == -1 && delta >= (created ? -(childrenCount + H + F) : 0)) {
					if (delta >= -F)
						// Requested index is a footer, or delta was already 0
						break;
					if (childrenCount == 0)
						// We're in the correct subgroup, but it has no children =>
						// requested index was a header
						break;
					delta += F + 1;
					lastGroupPosition--;
					lastChildPosition = childrenCount - 1;
				}
				// Case 4: We're in the right subgroup, go to the requested index
				if (lastChildPosition != -1 && delta >= -(lastChildPosition + H)) {
					if (delta >= -lastChildPosition) {
						lastChildPosition += delta;
						delta = 0;						
					}
					// At this point either delta = 0 or lastChildPosition < |delta| => requested index is a header
					break;
				}
			}
			// Make sure lastIndex ends up on a valid row (not a header or footer)
			lastIndex = index - delta;
			if (delta != 0) {
				return null;
			}
			return new int[] { lastGroupPosition, lastChildPosition };
		}

		/**
		 * Convenience method for <code>calculateIndex(groupPosition,-1)</code>.
		 */
		@SuppressWarnings("unused")
		private int calculateIndex(int groupPosition) {
			return calculateIndex(groupPosition,-1);
		}
	}
	private HierarchyHelper hierarchyHelper = new HierarchyHelper();
	
	private int calculateIndex(int groupPosition, int childPosition) { return hierarchyHelper.calculateIndex(groupPosition, childPosition);	}
	private int calculateIndex(int groupPosition) {	return calculateIndex(groupPosition, -1); }
	private int[] calculatePositions(int index) { return hierarchyHelper.calculatePositions(index); }
	
	private List<Long> knownIDs = new LinkedList<Long>();
	private int learnID(long id, int groupPosition, int childPosition) {
		int idIndex = 0;		
		for (int i = 0; i < groupPosition; i++) {
			idIndex++;
			if (isCreated(getID(i)))
				idIndex += adapter.getChildrenCount(i);
		}
		idIndex += childPosition + 1;
		knownIDs.add(idIndex, id);
		return idIndex;
	}

	protected class StableIdDataObserver extends DataSetObserver {
		private class ListTraverser {
			private int index;
			private Long currentId;
			Iterator<Long> listIterator;
			public ListTraverser() {
				index = getGroupHeaderCount();
				listIterator = knownIDs.iterator();
				if (listIterator.hasNext())
					currentId = listIterator.next();
			}
			/*
			 * Advance if this is still possible. 
			 * The last position this traverser will go to is 
			 * right behind the last data row in the list
			 */
			public void advance() {
				if (currentId != null) { 
					index++;
					currentId = listIterator.hasNext() ? listIterator.next() : null;				
					while (index < layout.getChildCount() - getGroupFooterCount() && calculatePositions(index) == null) {
						index++;
					}
				}
			}
			public Long provide() {
				return currentId;
			}
			public boolean hasMore() {
				return currentId != null;
			}
			public int getIndex() { return index; }
		}
		class AdapterTraverser {
			private final ExpandableListAdapter adapter;
			private int groupPosition = 0;
			private int childPosition = -1;
			private boolean skipChildren;
			public AdapterTraverser(ExpandableListAdapter adapter) { this.adapter = adapter; }
			public void advance() {
				if (skipChildren) {
					groupPosition++;
					childPosition = -1;
					return;
				}

				childPosition++;
				if (childPosition >= adapter.getChildrenCount(groupPosition)) {
					groupPosition++;
					childPosition = -1;
				}		
			}
			public Long provide() {
				if (!hasMore())
					return null;
				if (isGroup())
					return getID(groupPosition);	
				else
					return getID(groupPosition, childPosition);
			}
			public boolean hasMore() {				
				return groupPosition < adapter.getGroupCount();// - 1 ||
//						(groupPosition == adapter.getGroupCount() - 1 && skipChildren && childPosition == -1) || // Avoid calling adapter.getChildrenCount when skipping children
//						(groupPosition == adapter.getGroupCount() - 1 && !skipChildren && childPosition < adapter.getChildrenCount(groupPosition));					
			}				
			public int getGroupPosition() { return groupPosition; }
			public int getChildPosition() { return childPosition; }
			public boolean isGroup() { return childPosition == -1; }
			public void setSkipChildren(boolean skipChildren) { this.skipChildren = skipChildren; }
		}
		private class RemoveOperation implements Runnable {
			private int index;
			private long id;
			public RemoveOperation(int index, long id) { this.index = index; this.id = id; }
			public void run() {
				layout.removeViewAt(index);			
				created.remove(id);
				expanded.remove(id);
			}
		}
		private class AddOperation implements Runnable {
			private View view;
			private int index;
			public AddOperation(View view, int index, long id) { 
				this.view = view; this.index = index;
			}
			public void run() {
				layout.addView(view, index);
			}
		}
	
		public void onChanged() {	
			/* 	
			 * Since no information is provided as to where the data change occurred, 
			 * it might be anywhere, possibly even be several places at once. Therefore,
			 * we have no other choice but to walk both the entire list and the adapter
			 * and handle every single line. 
			 * Since we have stable IDs, we can compare adapter IDs against the list 
			 * of known IDs, by enabling us to detect corresponding rows in the list and
			 * the adapter - and by, extension, additions and removals - this will allow us 
			 * to avoid shifting children rows around if an addition or removal occurred in 
			 * the middle of the list.
			 * On each step of traversal, list and adapter id are compared, at which point
			 * one of three things can happen:
			 * 	1.	The two are equal. Then this row was there before, it just needs to have 
			 * 		its contents updated
			 * 	2.	The two are different, and the adapter id wasn't known before.
			 * 		This means there is a new row in the adapter that needs to be inserted in 
			 * 		the list.
			 * 	3.	The two are different, but the adapter id was known before. 
			 * 		This means the row used to exist in the adapter but is gone now, therefore
			 * 		it needs to be removed from the list.
			 * To separate the traversal logic from the actual comparison algorithm, it
			 * is removed into two separate traverser classes. While walking the two trees
			 * (the implied one of the list and the actual one of the adapter), these two 
			 * will be kept in sync, that is, if there is a discrepancy between the two due
			 * to an addition or removal in the data (resulting in one traverser getting 
			 * ahead of the other), the more advanced one will wait for the other one to
			 * catch up.
			 * To avoid indexing issues when adding or deleting rows, these operations will
			 * be deferred until after traversal is complete.
			 */	
			
			// A stack that will be holding the addition and removal operations determined
			// by this algorithm
			Deque<Runnable> operations = new LinkedList<Runnable>();
			
			// The new list of known IDs
			List<Long> newIDs = new LinkedList<Long>();
			
			// Traverser for the list
			ListTraverser listTraverser = new ListTraverser();
			// Traverser for the adapter
			AdapterTraverser adapterTraverser = new AdapterTraverser(adapter);
					

			Deque<RemoveOperation> deferredRemovals = new LinkedList<RemoveOperation>();
			// While either traverser has more elements to offer
			while (listTraverser.hasMore() || adapterTraverser.hasMore()) {
				Long listID = listTraverser.provide();
				Long adapterID = adapterTraverser.provide();
				
				if (listID != null && adapterID != null && listID.equals(adapterID)) { // New and old id match - this is a simple update operation
					// If there are any deferred removals available from a previous group removal,
					// push them all onto the stack now
					if (!deferredRemovals.isEmpty()) {
						for (RemoveOperation removal: deferredRemovals)
							operations.push(removal);
						deferredRemovals.clear();
					}
					
					View current = layout.getChildAt(listTraverser.getIndex());
					View converted = adapterTraverser.isGroup() ?
							makeGroupView(adapterTraverser.getGroupPosition(), 
									expanded.contains(listID), 
									adapterTraverser.getGroupPosition() == adapter.getGroupCount() - 1,
									current, layout) :
							makeChildView(adapterTraverser.getGroupPosition(), 
									adapterTraverser.getChildPosition(), 
									adapterTraverser.getChildPosition() == adapter.getChildrenCount(adapterTraverser.getGroupPosition()) - 1, 
									current, layout);
					// If it was not possible to convert the passed view, the adapter is allowed to create a new one
					// instead.
					// So check that the passed view was actually converted, otherwise swap it in the list.
					// This we can actually do directly (non-deferred) because it won't mess with the index
					if (converted != current) {
						layout.removeViewAt(listTraverser.getIndex());
						layout.addView(converted, listTraverser.getIndex());
					}
					newIDs.add(adapterID);
					// Update means we're still in sync - advance both providers
					listTraverser.advance();
					adapterTraverser.setSkipChildren(!isCreated(getID(adapterTraverser.getGroupPosition())));						
					adapterTraverser.advance();
				} else { // adapter and list id are different, possibly because one or the other is null
					if (knownIDs.contains(adapterID) || adapterID == null) { // (DELETION)
						// The adapter id was known before, or the adapter has run out of items
						// to supply, so this must be a deletion
						
						// Push remove operation for current row onto the operations stack		
						operations.push(new RemoveOperation(listTraverser.getIndex(), listID));
						
						boolean isGroup = calculatePositions(listTraverser.getIndex())[1] == -1;
						if (isGroup && isCreated(listID)) {
							// Delete all child header rows by pushing remove operations onto the
							// operations stack
							int index = listTraverser.getIndex(); // Index of the group row
							for (int i = 1; i < getChildHeaderCount() + 1; i++) {
								operations.push(new RemoveOperation(index + i, listID));
							}
						}
						
						if (isGroup && isCreated(listID)) {
							// Deleting a group. Delete all child footer rows by pushing deferred remove operations 
							int index = listTraverser.getIndex(); // Index of the group row
							for (int i = getChildHeaderCount() + 1; i < getChildFooterCount() + getChildHeaderCount() + 1; i++) {
								deferredRemovals.add(new RemoveOperation(index + i, listID));
							}
						} else
							// If still within group removal, update index of all deferred removals to accomodate
							// for the currently handled row
							// If not within a group removal, deferredRemovals will be empty
							for (RemoveOperation removal: deferredRemovals) {
								removal.index++;
							}
						
						// Only advance the list traverser, keep the adapter traverser 
						// waiting
						listTraverser.advance();
					} else { // (ADDITION)
						// The adapter id was not known before, so this is an addition
						// If there are any deferred removals available from a previous group removal,
						// push them all onto the stack now
						if (!deferredRemovals.isEmpty()) {
							for (RemoveOperation removal: deferredRemovals)
								operations.push(removal);
							deferredRemovals.clear();
						}
						
						// Create a view
						View view;
						if (adapterTraverser.isGroup()) {
							view = makeGroupView(adapterTraverser.getGroupPosition(), false, adapterTraverser.getGroupPosition() == adapter.getGroupCount() - 1, null, layout);
							// On a newly created group, we're not interested in the children 
							// since it can't have been expanded yet.
							// Therefore keep the traverser from asking the adapter for children
							adapterTraverser.setSkipChildren(true);
						} else {
							view = makeChildView(adapterTraverser.getGroupPosition(), 
									adapterTraverser.getChildPosition(), 
									adapterTraverser.getChildPosition() == adapter.getChildrenCount(adapterTraverser.getGroupPosition()) - 1,
									null, 
									layout);
						}
						// Push a corresponding addition operation onto the operations stack
						operations.push(new AddOperation(view, listTraverser.getIndex(), adapterID));
						newIDs.add(adapterID);
						// Only advance the adapter traverser
						adapterTraverser.advance();
					}
				}
			}

			// Comparison is finished, now execute the deferred operations that have
			// accumulated on the operations stack
			// Make sure that hierarchy calculations are done from the rear, or we
			// will have issues if the data change involved adding or deleting rows			
			while (!operations.isEmpty()) {
				operations.pop().run();
			}
			hierarchyHelper.invalidate();
			knownIDs = newIDs;
		}		
	}
	private DataSetObserver datasetObserver;
	
	/**
	 * Stores the group IDs of all groups for which
	 * children have already been created, even if they may
	 * not be shown at the moment.
	 * <p>
	 * <i>Note: the group id might be different from the group position</i>
	 */
	private Set<Long> created = new HashSet<Long>();
	/**
	 * Stores the group positions of all expanded groups.
	 */
	private Set<Long> expanded = new HashSet<Long>();
	
	public interface OnGroupClickListener {		
		 public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id);
	}
	private OnGroupClickListener onGroupClickListener;
	public OnGroupClickListener getOnGroupClickListener() {
		return onGroupClickListener;
	}
	public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
		this.onGroupClickListener = onGroupClickListener;
	}

	public interface OnChildClickListener {
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id);
	}
	private OnChildClickListener onChildClickListener;
	public OnChildClickListener getOnChildClickListener() {
		return onChildClickListener;
	}
	public void setOnChildClickListener(OnChildClickListener onChildClickListener) {
		this.onChildClickListener = onChildClickListener;
	}

	public interface OnGroupCollapseListener {
		public void onGroupCollapse(int groupPosition);
	}
	private OnGroupCollapseListener onGroupCollapseListener;
	public OnGroupCollapseListener getOnGroupCollapseListener() {
		return onGroupCollapseListener;
	}
	public void setOnGroupCollapseListener(OnGroupCollapseListener onGroupCollapseListener) {
		this.onGroupCollapseListener = onGroupCollapseListener;
	}

	public interface OnGroupExpandListener {
		public void onGroupExpand(int groupPosition);
	}
	private OnGroupExpandListener onGroupExpandListener;
	public OnGroupExpandListener getOnGroupExpandListener() {
		return onGroupExpandListener;
	}
	public void setOnGroupExpandListener(OnGroupExpandListener onGroupExpandListener) {
		this.onGroupExpandListener = onGroupExpandListener;
	}

	public interface OnChildHeaderClickListener {
		public void onChildHeaderClick(ExpandableListView parent, View v, int groupPosition);
	}
	private OnChildHeaderClickListener onChildHeaderClickListener;
	public OnChildHeaderClickListener getOnChildHeaderClickListener() {
		return onChildHeaderClickListener;
	}
	public void setOnChildHeaderClickListener(OnChildHeaderClickListener onChildHeaderClickListener) {
		this.onChildHeaderClickListener = onChildHeaderClickListener;
	}

	public interface OnChildFooterClickListener {
		public void onChildFooterClick(ExpandableListView parent, View v, int groupPosition);
	}
	private OnChildFooterClickListener onChildFooterClickListener;
	public OnChildFooterClickListener getOnChildFooterClickListener() {
		return onChildFooterClickListener;
	}
	public void setOnChildFooterClickListener(OnChildFooterClickListener onChildFooterClickListener) {
		this.onChildFooterClickListener = onChildFooterClickListener;
	}

	private OnClickListener internalChildClickListener = new OnClickListener() {
		public void onClick(View v) { onChildClick(v); }
	};
	private OnClickListener internalGroupClickListener = new OnClickListener() {		
		public void onClick(View v) { onGroupClick(v); }
	};
	private OnClickListener internalChildHeaderClickListener = new OnClickListener() {
		public void onClick(View v) { onChildHeaderClick(v); }
	};
	private OnClickListener internalChildFooterClickListener = new OnClickListener() {
		public void onClick(View v) { onChildFooterClick(v); }
	};
	
	private List<View> groupheaders = new LinkedList<View>();
	private List<View> groupfooters = new LinkedList<View>();
	public interface ChildHeaderFooterFactory {
		public int getCount();
		public View[] createViews();
	}
	private ChildHeaderFooterFactory childHeaderFactory;
	private ChildHeaderFooterFactory childFooterFactory;
	
	public ExpandableListView(Context context) {
		super(context);
		createViews();
	}

	public ExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createViews();
	}

	public ExpandableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createViews();
	}

	protected void createViews() {
		scrollview = new ScrollView(getContext());
		scrollview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// Set the scrollviews layout to reflect this view's
		scrollview.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
		
		layout = new LinearLayout(getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.listDivider } );
		layout.setDividerDrawable(array.getDrawable(0));
		array.recycle();
		layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		
		scrollview.addView(layout);
		addView(scrollview);
	}
	
	/**
	 * Returns the number of group header views
	 */
	public int getGroupHeaderCount() {
		return groupheaders.size();
	}
	
	public int getChildHeaderCount() {
		return childHeaderFactory != null ? childHeaderFactory.getCount() : 0;
	}
	
	public int getGroupFooterCount() {
		return groupfooters.size();
	}
	
	public int getChildFooterCount() {
		return childFooterFactory != null ? childFooterFactory.getCount() : 0;
	}	
	
	/**
	 * Returns whether the children items for the passed group have already been created.
	 * This does not necessarily mean that they are currently visible.
	 * @param groupId - The id of the group to check. IDs can be obtained from the 
	 * adapter by calling {@link this#getId()}
	 * @return True if the children rows for the specified group have already been created.
	 */
	private boolean isCreated(long groupId) {
		return created.contains(groupId);
	}
	
	/**
	 * Returns the expansion state of the specified group
	 * @param groupId - The id of the group for which to return the expansion state. IDs can be obtained from the 
	 * adapter by calling {@link this#getId()}
	 * @return <code>True</code> if the specified group is currently expanded, false otherwise
	 */
	public boolean isExpanded(long groupId) {
		return expanded.contains(groupId);
	}
	
	/**
	 * Wraps the call to the adapter's <code>getGroupView</code> method to allow additional preparations of
	 * the returned view.
	 */
	protected View makeGroupView(int groupPosition, boolean isExpanded, boolean isLastGroup, View convertView, ViewGroup parent) {
		View group = adapter.getGroupView(groupPosition, isExpanded, convertView, parent);
		TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
		group.setBackground(array.getDrawable(0));
		array.recycle();
		group.setOnClickListener(internalGroupClickListener);
		return group;
	}
	
	protected View makeChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View child = adapter.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
		TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.expandableListPreferredChildPaddingLeft });
		int paddingLeft = array.getDimensionPixelOffset(0,0);
		// Only adjust padding if the adapter created a new child
		if (child != convertView)
			child.setPadding(paddingLeft + child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), child.getPaddingBottom());
		array.recycle();
		
		child.setOnClickListener(internalChildClickListener);
		return child;
	}

	private void addGroupViewAt(int groupPosition, View view) {	
		int index = calculateIndex(groupPosition);
		layout.addView(view, index);
		learnID(getID(groupPosition), groupPosition, -1);
	}
	
	private void addChildViewAt(int groupPosition, int childPosition, View view) {
		long id = getID(groupPosition);
		created.add(id);
		int index = calculateIndex(groupPosition, childPosition);
		layout.addView(view, index);
		learnID(getID(groupPosition, childPosition),groupPosition,childPosition);
	}
	
	private void addViewAt(int groupPosition, int childPosition, View view) {
		if (childPosition == -1)
			addGroupViewAt(groupPosition, view);
		else
			addChildViewAt(groupPosition, childPosition, view);	
	}
		
	private void addChildHeaderViews(int groupPosition) {
		if (childHeaderFactory != null) {
			View[] headers = childHeaderFactory.createViews();
			// Hierarchy calculations depend on the getCount() method of the factory returning the correct
			// number of items, so fail gracefully at this point if the expected and the actual number differ
			// rather than having unexpected and user-visible behavior when expanding and collapsing groups.
			if (headers.length != childHeaderFactory.getCount()) {
				throw new IllegalStateException("The number of items returned by the child header factory was different from the expected count.");
			}
			int index = calculateIndex(groupPosition) + 1;
			for (int i = 0; i < childHeaderFactory.getCount(); i++) {
				headers[i].setOnClickListener(internalChildHeaderClickListener);
				layout.addView(headers[i], index + i);				
			}
		}
	}
	
	private void addChildFooterViews(int groupPosition) {
		if (childFooterFactory != null) {
			View[] footers = childFooterFactory.createViews();
			// Hierarchy calculations depend on the getCount() method of the factory returning the correct
			// number of items, so fail gracefully at this point if the expected and the actual number differ
			// rather than having unexpected and user-visible behavior when expanding and collapsing groups.
			if (footers.length != childFooterFactory.getCount()) {
				throw new IllegalStateException("The number of items returned by the child footer factory was different from the expected count.");
			}
			int index = calculateIndex(groupPosition, adapter.getChildrenCount(groupPosition) - 1) + 1;
			for (int i = 0; i < childFooterFactory.getCount(); i++) {
				footers[i].setOnClickListener(internalChildFooterClickListener);
				layout.addView(footers[i], index + i);				
			}
		}
	}
	
	/**
	 * Expands the indicated group, showing its children views. 
	 * @param groupPosition - The group to expand
	 * @return True if the group was expanded. If the group was already expanded at the
	 * time of the call, this will return false.
	 */
	public boolean expandGroup(int groupPosition) {
		long groupId = getID(groupPosition);
		// Already expanded - no need to do anything
		if (isExpanded(groupId))
			return false;
		
		int childrenCount = adapter.getChildrenCount(groupPosition);			
		// If the children of this group haven't been created yet (that is, aren't just hidden), 
		// the child rows need to be created. Also child headers and footers. 
		if (!isCreated(groupId)) {
			created.add(groupId);
			addChildHeaderViews(groupPosition);
			for (int i = 0; i < childrenCount; i++) {				
				View child = makeChildView(groupPosition, i, i == childrenCount - 1, null, layout);
				addChildViewAt(groupPosition, i, child);
			}
			addChildFooterViews(groupPosition);
		} else {
			// The children are already created, they're just hidden.
			// So un-hide them... ;)
			
			// Inlining this instead of calling getChildViewAt(int,int) will give a slight performance boost
			int index = calculateIndex(groupPosition, -1);
			for (int i = 0; i < childrenCount + getChildHeaderCount() + getChildFooterCount(); i++) {
				layout.getChildAt(++index).setVisibility(View.VISIBLE);
			}
		}
		expanded.add(groupId);			
		
		if (getOnGroupExpandListener() != null) 
			getOnGroupExpandListener().onGroupExpand(groupPosition);
		
		return true;
	}
	
	/**
	 * Collapses the indicated group, hiding its children views. 
	 * @param groupPosition - The group to collapse
	 * @return True if the group was collapsed. If the group was already collapsed at the
	 * time of the call, this will return false.
	 */
	public boolean collapseGroup(int groupPosition) {
		long groupId = getID(groupPosition);
		// Already collapsed - no need to do anything
		if (!isExpanded(groupId))
			return false;
		
		int childrenCount = adapter.getChildrenCount(groupPosition);
		// Inlining this instead of using getChildViewAt(int,int) will give a slight performance boost
		int index = calculateIndex(groupPosition, -1) + 1;
		for (int i = 0; i < childrenCount + getChildHeaderCount(); i++) {
			layout.getChildAt(index++).setVisibility(View.GONE);
		}
		expanded.remove(groupId);		
		
		if (getOnGroupCollapseListener() != null) {
			getOnGroupCollapseListener().onGroupCollapse(groupPosition);
		}
		return true;
	}
	
	protected void onChildClick(View view) {
		if (getOnChildClickListener() == null)
			return;
		
		int[] positions = calculatePositions(layout.indexOfChild(view));
		long childId = adapter.getChildId(positions[0], positions[1]);
		onChildClickListener.onChildClick(this, view, positions[0], positions[1], childId);
	}
	
	protected void onGroupClick(View view) {
		int groupPosition = calculatePositions(layout.indexOfChild(view))[0];
		long groupId = getID(groupPosition);
		
		if (getOnGroupClickListener() != null && getOnGroupClickListener().onGroupClick(this, view, groupPosition, groupId))
			// The listener has handled the click, so don't do anything
			return;
		
		if (isExpanded(groupId)) 
			collapseGroup(groupPosition);
		else
			expandGroup(groupPosition);
	}
	
	protected void onChildHeaderClick(View view) {
		if (getOnChildHeaderClickListener() == null) 
			return;
		
		int index = layout.indexOfChild(view) - 1;
		// Go backward until we hit the group row
		int[] positions;
		while ((positions = calculatePositions(index)) == null)
			index--;
		getOnChildHeaderClickListener().onChildHeaderClick(this, view, positions[0]);
	}
	
	protected void onChildFooterClick(View view) {
		if (getOnChildFooterClickListener() == null)
			return;
		
		int index = layout.indexOfChild(view) - 1;
		// Go backward until we hit the last child row, or the group row if no children exist
		int[] positions;
		while ((positions = calculatePositions(index)) == null)
			index--;
		getOnChildFooterClickListener().onChildFooterClick(this, view, positions[0]);	
	}
	
	protected void reset() {
		created.clear();
		expanded.clear();
		hierarchyHelper.invalidate();
		layout.removeViews(getGroupHeaderCount(), layout.getChildCount() - getGroupHeaderCount());
	}
	
	public void setAdapter(ExpandableListAdapter adapter) {
		// Don't do anything if this adapter is already being used
		if (this.adapter == adapter)
			return;
				
		if (this.adapter != null)
			this.adapter.unregisterDataSetObserver(datasetObserver);
		
		this.adapter = adapter;
		// If adapter has changed, reset ourselves,
		// discarding all previous state so we can
		// rebuild with the new adapter
		reset();
		// If there is no new adapter, we're done at this point
		if (adapter == null)
			return;
		
		// Rebuild the list with the new adapter
		int groupCount = adapter.getGroupCount();
		for (int i = 0; i < groupCount; i++) {
			View group = makeGroupView(i, false, i == groupCount - 1, null, layout);
			addGroupViewAt(i, group);
		}

		// Register dataset observer to reflect data changes in the UI
		if (adapter.hasStableIds())
			datasetObserver = new StableIdDataObserver();
		
		adapter.registerDataSetObserver(datasetObserver);
	}
	
	public void addGroupHeaderView(View v) {
		groupheaders.add(v);
		layout.addView(v,groupheaders.size() - 1);
		hierarchyHelper.invalidate();
	}
	
	public void setChildHeaderFactory(ChildHeaderFooterFactory factory) {
		if (factory == childHeaderFactory)
			return;
		
		int oldHeaderCount = getChildHeaderCount();
		this.childHeaderFactory = factory;
		
		if (adapter != null) { 
			for (int i = 0; i < adapter.getGroupCount(); i++) {
				if (isCreated(getID(i))) {
					int index = calculateIndex(i) + 1;
					for (int j = 0; j < oldHeaderCount; j++) {
						layout.removeViewAt(index + j);
					}					
					addChildHeaderViews(i);
				}
			}
		}
	}
	
	public void setChildFooterFactory(ChildHeaderFooterFactory factory) {
		if (factory == childFooterFactory)
			return;
		
		int oldFooterCount = getChildFooterCount();
		this.childFooterFactory = factory;
		
		if (adapter != null) { 
			for (int i = 0; i < adapter.getGroupCount(); i++) {
				if (isCreated(getID(i))) {
					int index = calculateIndex(i, adapter.getChildrenCount(i) - 1) + 1;
					for (int j = 0; j < oldFooterCount; j++) {
						layout.removeViewAt(index + j);
					}					
					addChildFooterViews(i);
				}
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		// Layout all children 
		// (that should really only be the scrollview, but just to play this by the book here, let's iterate 
		for (int i = 0; i < getChildCount(); i++)
			getChildAt(i).layout(0, 0, right - left, bottom -top);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// Children width must be exactly the width and height of this view (i.e. fill it completely)		
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
		// Measure all children 
		// (that should really only be the scrollview, but just to play this by the book here, let's iterate 
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
	}
	
	public void scrollTo(int groupPosition, int childPosition) {
		// Calculate index doesn't do any scope checking, so we need to do this here
		// If the requested child view is not currently showing, go to its group 
		// instead
		if (!isExpanded(getID(groupPosition)))
			childPosition = -1;
		
		int index = calculateIndex(groupPosition, childPosition);		
		int y = layout.getChildAt(index).getBottom();
		if (y < scrollview.getScrollY()) // Scrolling up
			y = layout.getChildAt(index).getTop(); 
		scrollview.smoothScrollTo(0, y);
	}
	
	/**
	 * Scrolls so the specified you becomes visible
	 * @param view - The view to scroll to. If this is not actually contained
	 * in the list, no scrolling is done.
	 */
	public void scrollTo(View view) {
		if (layout.indexOfChild(view) == -1)
			return;
		
		int y = view.getBottom();
		if (y < scrollview.getScrollY()) // Scrolling up
			y = view.getTop(); 
		scrollview.smoothScrollTo(0, y);	
	}
	
	private static class SavedState extends BaseSavedState {
		private Set<Long> expanded;
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<ExpandableListView.SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		public SavedState(Parcelable superState) {
			super(superState);
		}
		public SavedState(Parcel parcel) {
			super(parcel);
			Long[] expandedIDs = (Long[]) parcel.readArray(ClassLoader.getSystemClassLoader());
			expanded = new HashSet<Long>(expandedIDs.length);
			Collections.addAll(expanded, expandedIDs);					
		}		
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeArray(expanded.toArray());
		}
	}
	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ownState = new SavedState(superState);
		ownState.expanded = expanded;
		return ownState;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SavedState ownState = (SavedState) state;
		super.onRestoreInstanceState(ownState.getSuperState());
		
		Set<Long> expanded = ownState.expanded;
		if (adapter != null)
			for (int i = 0; i < adapter.getGroupCount(); i++) {
				if (expanded.contains(getID(i)))
					expandGroup(i);
			}
	}
}