package com.larmlarms.data;

import android.content.Context;
import android.util.Log;

import com.larmlarms.BuildConfig;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * Holds a list of Alarms and can mask Alarms.
 */
public final class AlarmGroup extends Item implements Cloneable {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AlarmGroup";

	/**
	 * Number of fields in a single edit string (don't have one for store strings because they can
	 * span multiple lines and can have a different number of fields dependent on type)
	 */
	private static final int NUM_EDIT_FIELDS = 4;

	/**
	 * Contains the child Alarms and AlarmGroups stored within this folder. Should always be nonnull
	 * and sorted.
	 */
	@NotNull
	private ArrayList<Item> items;

	/* ***********************************  Constructors  *********************************** */

	/**
	 * Initializes a new AlarmGroup with all dummy data.
	 */
	public AlarmGroup() {
		this("", new ArrayList<>(),
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Initializes a new AlarmGroup with a name.
	 * @param title the new name of the folder
	 */
	public AlarmGroup(@Nullable String title) {
		this(title, new ArrayList<>(),
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	public AlarmGroup (@Nullable String title, @NotNull ArrayList<Item> children) {
		this(title, children,
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Initializes a new AlarmGroup with a name and contents.
	 * @param name the new name of the folder
	 * @param children the new Listables within the folder
	 */
	public AlarmGroup(@Nullable String name, @NotNull ArrayList<Item> children, int id) {
		super(name != null ? name : "new folder", id);

		items = new ArrayList<>();
		setListables(children);		// validating data
	}

	/* *******************************  Methods from Listable  ****************************** */

	/**
	 * Gets the repeat string of the folder.
	 * @return always returns an empty string
	 */
	@Override @NotNull @Contract(pure = true)
	public String getRepeatString() { return ""; }

	/**
	 * Gets the next ring time of the folder.
	 * @return always returns an empty string
	 */
	@Override @NotNull @Contract(pure = true)
	public String getNextRingTime() { return ""; }

	/**
	 * Gets the total number of items the folder represents (includes itself).
	 * @return the number of items in the folder, including itself (always at least 1)
	 */
	@Override @Contract(pure = true)
	public int size() { return items.size() + 1; }

	/**
	 * Determines whether other is equal to this AlarmGroup or not. Checks only for name and whether
	 * it's active or not.
	 * @param other the other object to compare to
	 * @return whether the two objects are equal or not
	 */
	@Contract("null -> false")
	public boolean equals(Object other) {
		if (!(other instanceof AlarmGroup)) return false;

		AlarmGroup that = (AlarmGroup) other;
		return this.name.equals(that.name) && this.isActive == that.isActive;
	}
	
	/**
	 * Compares this folder with the other object. Alarms are always considered "after" folders.
	 * Folders are compared with this precedence: name, then id (the lower id is first). It 
	 * shouldn't be necessary to compare anything further since their ids should always be unique. 
	 * @return this folder compared to the listable (negative if this is first, positive if this is 
	 * second, 0 if they're equal).
	 */
	@Override
	public int compareTo(@NotNull Item other) {
		if (other instanceof Alarm) return -1;
		
		AlarmGroup that = (AlarmGroup) other;
		int temp = this.name.compareTo(that.name);
		if (temp != 0) return temp;
		
		return this.id - that.id;
	}

	/**
	 * Creates an edit string for the current folder.
	 * <br/>
	 * Current edit string format (separated by tabs):
	 * [id] [name] [isActive] [isOpen]
	 */
	@NotNull @Override @Contract(pure = true)
	public String toEditString() { return "" + id + '\t' + name + '\t' + isActive; }

	/**
	 * Creates a string for storing the current folder. Stores folder with type identifier and
	 * recursively stores all its children as well.
	 * <br/>
	 * Current store string format (separated by tabs):
	 * [store identity string]
	 * 		[children store strings, separated by new lines]
	 */
	@NotNull @Override @Contract(pure = true)
	public String toStoreString() {
		StringBuilder res = new StringBuilder(getStoreStringSingle());
		res.append('\n');

		for (Item l : items) {
			// need to add a tab to all lines inside
			String[] lines = l.toStoreString().split("\n");		// recursive call with extra steps
			for (String line : lines) { res.append('\t').append(line).append('\n'); }
		}
		res.deleteCharAt(res.length() - 1);		// deleting the last \n

		return res.toString();
	}

	/* ***************************  Getter and Setter Methods  ****************************** */

	/**
	 * Gets the listables within the folder.
	 * @return the listables, will not be null
	 */
	@Contract(pure = true) @NotNull
	ArrayList<Item> getListables() { return items; }

	/**
	 * Sets the listables within the folder. If the new list is invalid (the list or any listables
	 * within it are null), will not do anything.
	 * @param items a new list of listables to use, can be null
	 */
	public void setListables(@Nullable ArrayList<Item> items) {
		if (items == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "New list of Listables is null.");
			return;
		}
		for (Item l : items) {
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Listable in new list of Listables is null.");
				return;
			}
		}

		Collections.sort(items);
		this.items = items;
		refreshLookups();
	}

	/* ************************************  Static Methods  ********************************** */

	/**
	 * Takes edit strings to rebuild into a new AlarmGroup
	 * @param src the edit string to create from
	 */
	@Nullable @Contract(pure = true)
	static AlarmGroup fromEditString(@Nullable String src) {
		if (src == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != NUM_EDIT_FIELDS) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		AlarmGroup dest;
		try {
			dest = new AlarmGroup(fields[1], new ArrayList<>(), Integer.parseInt(fields[0]));
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has an invalid id.");
			return null;
		}
		dest.setActive(Boolean.parseBoolean(fields[2]));

		return dest;
	}

	/**
	 * Takes store strings to rebuild into a new AlarmGroup
	 * @param currContext the current context
	 * @param src the store string to create from
	 */
	@Nullable @Contract(pure = true)
	static AlarmGroup fromStoreString(@Nullable Context currContext, @Nullable String src) {
		if (src == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Store string is null.");
			return null;
		} else if (src.length() == 0) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Store string is empty.");
			return null;
		}

		String[] lines = src.split("\n");		// line by line

		// first line for the AlarmGroup itself
		if (!lines[0].startsWith("f\t")) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Store string has an unknown ID field.");
			return null;
		}

		AlarmGroup dest = fromEditString(lines[0].substring(2));
		if (dest == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Store string has an invalid first line.");
			return null;
		}

		for (int i = 1; i < lines.length; i++) {
			// not a child of the current AlarmGroup
			if (!lines[i].startsWith("\t")) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Store string is formatted incorrectly (children aren't indented).");
				return null;
			}
			else { lines[i] = lines[i].substring(1); }			// removing the first indent
		}

		for (int i = 1; i < lines.length; i++) {
			String item = lines[i];
			if (item.startsWith("a")) {
				// is an Alarm
				Alarm alarm = Alarm.fromStoreString(currContext, item);
				if (alarm == null) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Child alarm store string was invalid.");
					return null;
				}

				dest.addListable(alarm);
			} else if (item.startsWith("f")) {
				// is an AlarmGroup with perhaps its own set of Alarms and AlarmGroups
				StringBuilder new_src = new StringBuilder(item);

				// look for the nested Listables and build another
				int j;
				for (j = i + 1; j < lines.length; j++) {
					if (!lines[j].startsWith("\t")) {		// when it's no longer nested
						// don't need to look through those already covered, but remember it adds 1
						// at the end of the loop
						i = j - 1;
						break;
					}
					new_src.append('\n').append(lines[j]);
				}

				// didn't trip on the break statement, so we want it to end after this loop (j =
				// lines.length), so we can set it to any int >= j - 1
				if (i != j - 1) { i = j; }

				// essentially a recursive call with some looping involved
				AlarmGroup curr_folder = fromStoreString(currContext, new_src.toString());
				if (curr_folder == null) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Child AlarmGroup store string was invalid.");
					return null;
				}

				dest.addListable(curr_folder);
			}
			else {
				if (BuildConfig.DEBUG) Log.e(TAG, "Store string line has an unknown ID field.");
				return null;
			}
		}
		return dest;
	}

	/**
	 * Counts the number of Listables in the list. Does not include the parent object in the count.
	 * @param items the list of Listables to count, can be null
	 * @return number of Listables in the list, or zero if list is null
	 */
	@Contract(pure = true)
	private static int getSizeOfList(@Nullable ArrayList<Item> items) {
		if (items == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "List of listables to get size of was null.");
			return 0;
		}

		int len = 0;
		for (Item l : items) {
			len += l.size();
		}
		return len;
	}

	/**
	 * Searches for a Listable at srcIndex and returns data within a ListableInfo about it. Fills all
	 * fields of the ListableInfo.
	 * @param root the folder to look through for this Listable. Will not change the data within it
	 * @param srcIndex the absolute position of the Listable to search for
	 * @param visible whether to use only what's visible or not (also determines type of index)
	 * @return a ListableInfo describing everything it can about the Listable at srcIndex, or null
	 * if the Listable was not found
	 */
	@Nullable @Contract(pure = true)
	private static ListableInfo getListableInfo(@Nullable AlarmGroup root, final int srcIndex,
												final boolean visible) {
		if (root == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Listable to look through is null.");
			return null;
		}

		ArrayList<Item> data = root.items;

		// the final ListableInfo to store stuff in
		ListableInfo info = new ListableInfo();

		ArrayList<Integer> lookup;
		int index;		// the relative index to the current folder

		if (visible) {
			lookup = root.getVisibleLookup();
			index = findOuterListableIndex(lookup, srcIndex, root.visibleSize());
		}
		else {
			lookup = root.getLookup();
			index = findOuterListableIndex(lookup, srcIndex, root.size());
		}

		// the number of indents for this listable
		int indents = 0;

		// represents the absolute index of the Listable we're looking for in the current folder
		int absIndex = srcIndex;

		// represents the absolute index of the current folder we're in, starts at -1 to account for
		// math and to show that there the listable's parent IS the current folder
		int currFolderIndex = -1;

		// the current folder we're in
		AlarmGroup currFolder = null;

		// the current path of the Listable
		@NotNull
		StringBuilder pathBuilder = new StringBuilder(root.getListableName());

		while (index != -1) {
			// builds the path, must run on ALL iterations, so must run before we check whether this
			// is the index we were searching for
			if (currFolder != null) pathBuilder.append(currFolder);
			pathBuilder.append('/');

			// must be the element itself in the current folder
			if (lookup.get(index) == absIndex) {
				info.absIndex = srcIndex;
				info.relIndex = index;
				info.numIndents = indents;
				info.absParentIndex = currFolderIndex;
				info.item = data.get(index);
				info.parent = currFolder;
				info.path = pathBuilder.toString();

				return info;
			}

			// listable is within an AlarmGroup, so must find the new absolute index within that folder
			// must subtract 1 to take into account the folder itself
			absIndex = absIndex - 1 - lookup.get(index);

			// must add 1 to take into account the previous outer folder (initializes with -1 so that
			// the root folder isn't actually accounted for, since index starts within the root folder)
			currFolderIndex += lookup.get(index) + 1;

			// new folder to look inside
			currFolder = (AlarmGroup) data.get(index);
			data = currFolder.getListables();

			indents++;

			// new index relative to the new folder
			if (visible) {
				lookup = currFolder.getVisibleLookup();
				index = findOuterListableIndex(lookup, absIndex, currFolder.visibleSize() - 1);
			}
			else {
				lookup = currFolder.getLookup();
				index = findOuterListableIndex(lookup, absIndex, currFolder.size() - 1);
			}
		}
		if (BuildConfig.DEBUG) Log.e(TAG, "Could not find the specified Listable's info in the source data/lookup.");
		return null;
	}

	/**
	 * Searches for a Listable in a Listable lookup list. Gets the index of either the Listable
	 * itself or the Listable that contains it.
	 * @param lookup the lookup list corresponding to the current layer
	 * @param index The index within the current layer that we're looking for, must be less than
	 *              total item count. Logs an error if out of bounds and method will return -1
	 * @param total the total number of items, just within the current layer (includes children)
	 * @return the relative index of the containing Listable (or the Listable itself), or -1 if it
	 * wasn't found
	 */
	@Contract(pure = true)
	static int findOuterListableIndex(@Nullable final ArrayList<Integer> lookup, final int index,
									  final int total) {
		if (lookup == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable lookup was null.");
			return -1;
		}

		int max = lookup.size();

		if (max == 0 || index >= total || index < 0) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable index is out of bounds!");
			return -1;
		}
		else if (max == 1) { return 0; }

		int lower = 0, upper = max - 1, mid;

		while (lower <= upper) {
			mid = (upper + lower) / 2;
			if (index >= lookup.get(mid) && (mid + 1 == max || index < lookup.get(mid + 1))) {
				return mid;
			}
			else if (index < lookup.get(mid)) { upper = mid - 1; }
			else { lower = mid + 1; }
		}
		if (BuildConfig.DEBUG) Log.e(TAG, "Could not find the specified Listable's parent index within the source data/lookup.");
		return -1;
	}

	/* **************************  Manipulating Listables (Relative)  *************************** */

	/**
	 * Gets the Listable at the given relative index.
	 * @param relIndex the relative index of the Listable required
	 * @return the Listable at the relative index, or null if not found
	 */
	@Nullable @Contract(pure = true)
	public Item getListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't get Listable. Index is out of bounds.");
			return null;
		}
		return items.get(relIndex);
	}

	/**
	 * Gets the first AlarmGroup with the given name (should be the only one) within the current
	 * folder.
	 * @param name the name to search for, shouldn't be null
	 * @return the folder with the given name, or null if not found
	 */
	@Nullable @Contract(pure = true)
	private AlarmGroup getFolder(@NotNull final String name) {
		for (Item l : items) {
			if (l instanceof AlarmGroup && l.getListableName().equals(name))
				return (AlarmGroup) l;
		}
		return null;
	}

	/**
	 * Gets the index of the listable within the current folder (a relative index)
	 * @param name the name of the Listable to look for, shouldn't be null
	 * @return the index of the first Listable with that name, or -1 if not found
	 */
	@Contract(pure = true)
	private int getListableIndex(@Nullable final String name) {
		if (name == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't look for the index. Name is null.");
			return -1;
		}

		int index = 0;
		for (Item l : items) {
			if (l.getListableName().equals(name)) return index;
			index++;
		}
		return -1;
	}

	/**
	 * Sets a listable in the dataset using its relative index. The listable may move within the list
	 * to maintain sorted order.
	 * @param relIndex the old relative index to set with
	 * @param item the new Listable to set relIndex to
	 */
	private void setListable(final int relIndex, @Nullable final Item item) {
		if (relIndex < 0 || relIndex >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Index is out of bounds.");
			return;
		} else if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		Item old = items.remove(relIndex);
		int di = 0, dv = 0;
		int newIndex = AlarmGroup.insertIndex(items, item);

		items.add(newIndex, item);

		// add index change to all lookup indices and size
		for (int i = Math.min(newIndex, relIndex) + 1; i < items.size(); i++) {
			di = (i > newIndex ? item.size() : 0) - (i > relIndex ? old.size() : 0);

			lookup.set(i, lookup.get(i) + di);
		}
		size += di;
		visibleSize += dv;
	}

	/**
	 * Adds a listable to the end of the current folder.
	 * @param item the Listable to add to the folder
	 */
	void addListable(@Nullable final Item item) {
		if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		int newIndex = AlarmGroup.insertIndex(items, item);
		int ds = item.size(), dv = item.visibleSize();
		items.add(newIndex, item);

		if (newIndex == lookup.size()) {
			lookup.add(size - 1);
			visibleLookup.add(visibleSize - 1);
		}
		else {
			lookup.add(newIndex, lookup.get(newIndex));
			visibleLookup.add(newIndex, visibleLookup.get(newIndex));
			for (int i = newIndex + 1; i < items.size(); i++) {
				lookup.set(i, lookup.get(i) + ds);
				visibleLookup.set(i, visibleLookup.get(i) + dv);
			}
		}

		size += ds;
		visibleSize += dv;
	}

	/**
	 * Deletes a listable at the specified relative index.
	 * @param relIndex the relative index of the Listable to delete
	 * @return the listable deleted, or null
	 */
	@Nullable
	private Item deleteListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't delete Listable. Index is out of bounds.");
			return null;
		}
		Item l = items.remove(relIndex);
		int ds = l.size(), dv = l.visibleSize();

		// add index change to all lookup indices and sizes
		for (int i = relIndex; i < items.size()-1; i++) {
			// updating and moving back an entry at the same time
			lookup.set(i, lookup.get(i+1) - ds);
			visibleLookup.set(i, visibleLookup.get(i+1) - dv);
		}
		lookup.remove(lookup.size() - 1);
		visibleLookup.remove(visibleLookup.size() - 1);

		size -= ds;
		visibleSize -= dv;
		return l;
	}

	/* **************************  Manipulating Listables (Absolute)  *************************** */

	/**
	 * Convenience method for getListableInfo(). Gets the ListableInfo for the listable at the
	 * specified absolute index.
	 * @param absIndex absolute index to search for
	 * @param visible whether to use only what's visible or not
	 * @return the Listable or null if not found
	 */
	@Contract(pure = true)
	public ListableInfo getListableInfo(final int absIndex, final boolean visible) {
		return getListableInfo(this, absIndex, visible);
	}

	/**
	 * Gets the listable at the specified absolute index
	 * @param absIndex absolute index to search for
	 * @param visible whether to use only what's visible or not
	 * @return the Listable or null if not found
	 */
	@Nullable
	public Item getListableAbs(final int absIndex, final boolean visible) {
		ListableInfo i = getListableInfo(absIndex, visible);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return null;
		}

		return i.item;
	}

	/**
	 * Sets item as the new Listable in the dataset at the absolute index. If the index doesn't exist,
	 * doesn't do anything. Assumes visible only.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 * @return the listable that was deleted, or null if there was none
	 */
	@Nullable
	public Item setListableAbs(final int absIndex, final Item item) {
		ListableInfo i = getListableInfo(absIndex, true);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return null;
		}

		if (i.parent == null) {
			setListable(i.relIndex, item);
		}
		else {
			i.parent.setListable(i.relIndex, item);
			refreshLookups();
		}
		return i.item;
	}

	/**
	 * Deletes the listable at the specified absolute index. Always assumes it's for the visible
	 * listables only. Assumes visible only.
	 * @param absIndex the absolute index of the Listable to delete
	 * @return the listable that was deleted or null if there was an error
	 */
	@Nullable
	public Item deleteListableAbs(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex, true);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return null;
		}

		Item l;
		if (i.parent == null) {
			l = deleteListable(i.relIndex);
		}
		else {
			l = i.parent.deleteListable(i.relIndex);
			refreshLookups();
		}
		return l;
	}

	/**
	 * Adds a listable to this folder at the given path. Assumes visible only.
	 * @param item the info given about the listable to add, should include the listable and path
	 * @param path the path to add the listable to
	 * @return the absolute index of the new listable (visible index). Will return -2 if there was
	 * an error or -1 if it's not visible
	 */
	public int addListableAbs(@Nullable Item item, @Nullable String path) {
		if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "addListableAbs: The listable specified was null.");
			return -2;
		}
		if (path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "addListableAbs: The path specified was null.");
			return -2;
		}
		String[] folders = path.split("/");	// ignore the first one (this folder)
		AlarmGroup currFolder = this;

		// absIndex of the new place of the listable
		// the -1 counteracts +1 for the root folder (would throw off calculations)
		int index = -1;
		boolean isVisible = true;
		ArrayList<Integer> lookup = currFolder.getLookup();

		for (int i = 1; i < folders.length; i++) {
			isVisible &= currFolder.getIsOpen();
			index += lookup.get(currFolder.getListableIndex(folders[i])) + 1;
			currFolder = currFolder.getFolder(folders[i]);		// doesn't care about open status
			if (currFolder == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "addListableAbs: Couldn't find the specified path.");
				return -2;
			}
			lookup = currFolder.getLookup();
		}
		index += currFolder.visibleSize();

		currFolder.addListable(item);
		refreshLookups();

		if (!isVisible) return -1;
		return index;
	}

	/**
	 * Moves the listable specified by index to the new path. Assumes visible only.
	 * @param item the new listable to replace with (will just use the old one if it's null)
	 * @param path the path to move the listable to
	 * @param oldAbsIndex the previous absolute index of the listable (visible index)
	 * @return the new absolute index of the listable (visible index). Will return -1 if there was
	 * an error or if it's not visible
	 */
	public int moveListableAbs(@Nullable Item item, @Nullable String path, final int oldAbsIndex) {
		if (path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "moveListableAbs: The path specified was null.");
			return -1;
		}
		String[] folders = path.split("/");	// ignore the first one (root folder name)
		AlarmGroup currFolder = this;

		// absIndex of the new place of the listable
		// the -1 counteracts +1 for the root folder (would throw off calculations)
		int index = -1;
		boolean isVisible = true;
		ArrayList<Integer> lookup = currFolder.getVisibleLookup();

		for (int i = 1; i < folders.length; i++) {
			isVisible &= currFolder.getIsOpen();
			index += lookup.get(currFolder.getListableIndex(folders[i])) + 1;
			currFolder = currFolder.getFolder(folders[i]);		// doesn't care about open status
			if (currFolder == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "moveListableAbs: Couldn't find the specified path.");
				return -1;
			}
		}
		index += currFolder.size();

		Item newItem = item;
		if (newItem == null) {
			// replace with what's currently at the abs index
			newItem = deleteListableAbs(oldAbsIndex);
			if (newItem == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "moveListableAbs: Couldn't find the listable to move.");
				return -1;
			}
		}
		else {
			// replace with the new listable
			deleteListableAbs(oldAbsIndex);
		}

		currFolder.addListable(newItem);

		return index;
	}

	/* ***********************************  Other Methods  ************************************** */

	/**
	 * Makes a new string that describes this folder. Use edit or store strings if a full
	 * representation is necessary. Returns simply tne name of the folder.
	 */
	@NotNull @Override @Contract(pure = true)
	public String toString() { return name; }

	/**
	 * Builds the line of the store string that represents the AlarmGroup itself.
	 * <br/>
	 * Current store identity string format (separated by tabs):
	 * f	[edit string]
	 */
	@NotNull @Contract(pure = true)
	private String getStoreStringSingle() {
		return "f\t" + toEditString();
	}

	/**
	 * Returns a list of strings describing the AlarmGroup and any AlarmGroups within it
	 * @return an array list of strings that stores all of the names of the folders within the group,
	 * including the folder itself. The first item in the list will be the name of the folder itself.
	 * Each string shows the full path of the child, separated by slashes. Each string has a trailing
	 * slash.
	 */
	@NotNull @Contract(pure = true)
	ArrayList<String> toPathList() { return toPathList("", this); }

	/**
	 * Helper function for toPathList().
	 * @param prefix the prefix that should be added to the current parent name and all of its
	 *               children. Shouldn't be null
	 * @param parent the current AlarmGroup we're on, shouldn't be null
	 * @return an array list of strings that stores all of the folders within the parent. All strings
	 * should have the prefix appended to them, have a trailing slash, and should contain the full
	 * path of each folder.
	 */
	private static ArrayList<String> toPathList(@NotNull String prefix, @NotNull AlarmGroup parent) {
		ArrayList<String> pathList = new ArrayList<>();
		String storeString = parent.getListableName() + '/';
		pathList.add(prefix + storeString);
		prefix += storeString;

		for (Item child : parent.items) {
			if (child instanceof AlarmGroup) {
				pathList.addAll(toPathList(prefix, (AlarmGroup) child));
			}
		}
		return pathList;
	}

	/**
	 * Returns the correct index to insert the given listable into the list at.
	 * @param items the list to insert into
	 * @param l the item to insert
	 * @return which index to insert the item into, or -1 if there was an error
	 */
	@Contract(pure = true)
	private static int insertIndex(@NotNull final ArrayList<Item> items, @NotNull final Item l) {
		if (items.size() == 0) return 0;
		int left = 0, right = items.size() - 1, mid, comp;

		while (right >= left) {
			mid = (left + right) / 2;
			comp = l.compareTo(items.get(mid));
			if (comp == 0) return mid;
			else if (comp < 0) right = mid - 1;
			else left = mid + 1;
		}

		// they "cross over" and represent an inverted but correct range that l sits between
		// this is the usual case (we don't usually find l)
		return left;
	}
}
