package heger.christian.ledger.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class TreeEmulator implements Parcelable {
	/**
	 * Interface to provide unique, non-changing id's for the views passed into it.
	 * <p>
	 * The actual view instances can change during the activity lifecycle due to 
	 * destruction and re-creation of the activity (e.g. due to the device being flipped),
	 * so the view's object id's cannot be used for this.
	 * @author chris
	 */
	public interface Identifier {
		/**
		 * Return a unique and non-changing id for the passed view. 
		 * The returned id needs to be independent of the specific view instance, since this
		 * may change during the activity lifecycle due to configuration changes.
		 * @param view - The view to generate the id for
		 * @return A unique id for the view
		 */
		public Object identify(View view);
	}
	private Identifier identifier;
	
	// It might be better to use weak references stored in a WeakHashMap
	// here. This would automatically remove child rows from the childMap
	// when they are deleted from the table (or more specifically, there are
	// no more strong references to them, i.e. the reference in the childMap is 
	// the last reference), as well as removing the entire mapping when
	// a key row is removed. 
	// However, simply storing in a Set<WeakReference<TableRow>> will not
	// work, as it is unclear of what will happen when iterating over the
	// set when the table row has been collected by the garbage collector.
	// At http://www.java2s.com/Code/Java/Collections-Data-Structure/WeakHashSet.htm
	// an implementation of a WeakHashSet is available that looks quite 
	// promising.
	private Map<View, Set<View>> childMap = new HashMap<View, Set<View>>();
	// We'll use the content URIs here instead of the actual rows, so
	// that the expansion state can be reserved across fragment retentions
	// without leaking the fragment's/activity's resources.
	// (The android API documentation specifically warns against 
	// preserving views when retaining fragments.)
	private Map<Object, Boolean> expansionState = new HashMap<Object, Boolean>();

	public void addChild(View parent, View child) {
		Set<View> children = childMap.get(parent);
		// If this is the first child for that parent, then
		// a new mapping needs to be created first
		if (children == null) {
			children = new HashSet<View>();			
			childMap.put(parent, children);
		}
		children.add(child);
	}	
	public void removeChild(View parent, View child) {
		Set<View> children = childMap.get(parent);
		// Guard against the case where no mappings for the parent row exist
		if (children != null) {
			children.remove(child);
			// If this was the last child row, remove the whole mapping 
			// and set the expansion to "not expanded"
			if (children.isEmpty()) {
				childMap.remove(parent);
				setExpansion(parent, false);
			}
		}
	}
	
	public Set<View> getChildren(View parent) {
		Set<View> children = childMap.get(parent);
		if (children == null)
			// If no mappings exist, return an empty set
			// Not using Collections.EMPTY_SET here as that one 
			// is immutable
			return new HashSet<View>();
		else
			// Otherwise, return a copy of the set of children 
			// for that parent row
			return new HashSet<View>(childMap.get(parent));
	}
	
	public void setExpansion(View row, boolean expanded) {
		expansionState.put(getIdentifier().identify(row), expanded);
	}
	
	public boolean isExpanded(View row) {
		Boolean expanded = expansionState.get(getIdentifier().identify(row));
		if (expanded == null)
			expanded = false;
		return expanded;
	}
	
	public void clear() {
		childMap.clear();		
	}
	
	/**
	 * Returns the <code>Identifier</code> used for tracking expansion state.
	 */
	public Identifier getIdentifier() {
		return identifier;
	}
	
	/**
	 * Sets the identifier used for tracking expansion state.
	 * 
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public static final Parcelable.Creator<TreeEmulator> CREATOR = new Parcelable.Creator<TreeEmulator>() {
		@Override
		public TreeEmulator createFromParcel(Parcel source) {
			TreeEmulator treeEmulator = new TreeEmulator();
			int len = source.readInt();
			for (int i=0; i < len; i++) {
				treeEmulator.expansionState.put(source.readValue(null), source.readByte() == 0 ? true : false);
			}
			return treeEmulator;
		}
		@Override
		public TreeEmulator[] newArray(int size) {
			return new TreeEmulator[size];
		}
		
	};
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Save number of mappings
		dest.writeInt(expansionState.size());
		try {
			for (Entry<Object, Boolean> entry: expansionState.entrySet()) {
				dest.writeValue(entry.getKey());
				dest.writeByte((byte) (entry.getValue().booleanValue() ? 0 : 1));
			}
		} catch (RuntimeException x) {
			throw new RuntimeException("Your identifier must return parcelable id\'s if you want to write the tree emulator to a parcel",x);
		}
	}
}
