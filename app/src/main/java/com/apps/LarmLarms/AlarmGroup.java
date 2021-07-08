package com.apps.LarmLarms;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Holds a list of Alarms and can mask Alarms.
 */
public final class AlarmGroup implements Listable, Cloneable {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AlarmGroup";

	/**
	 * Number of fields in a single edit string (don't have one for store strings because they can
	 * span multiple lines and can have a different number of fields dependent on type)
	 */
	private static final int NUM_EDIT_FIELDS = 2;

	/**
	 * Stores the name of the folder. Only restricted character: tabs. If tabs are present, the
	 * class will log errors.
	 */
	@NotNull
	private String name;
	/**
	 * Stores the active state of the folder. Represents whether the alarms within it will ring or
	 * not, and takes precedence over a child alarm's active state.
	 */
	private boolean isActive;
	/**
	 * Stores whether the folder looks open or closed right now. Affects what size() and getLookup()
	 * return. Does NOT affect what getListables() returns.
	 */
	private boolean isOpen;

	/**
	 * Contains the child Alarms and AlarmGroups stored within this folder. Will not be null.
	 */
	@NotNull
	private ArrayList<Listable> listables;
	/**
	 * Should be the same length as field listables. Each entry represents the absolute index within
	 * this folder layer of each listable (first listable is always absolute index 0). Does not take
	 * into account outer layers of folders.
	 */
	@NotNull
	private ArrayList<Integer> lookup;

	private int totalNumItems;

	/* ***********************************  Constructors  *********************************** */

	/**
	 * Initializes a new AlarmGroup with all dummy data.
	 */
	public AlarmGroup() { this("default name", new ArrayList<Listable>()); }

	/**
	 * Initializes a new AlarmGroup with a name.
	 * @param title the new name of the folder
	 */
	public AlarmGroup(@NotNull String title) { this(title, new ArrayList<Listable>()); }

	/**
	 * Initializes a new AlarmGroup with a name and contents.
	 * @param title the new name of the folder
	 * @param children the new Listables within the folder
	 */
	public AlarmGroup(@NotNull String title, @NotNull ArrayList<Listable> children) {
		// TODO: init dummy data for AlarmGroup
		name = title;
		isActive = true;
		isOpen = true;
		listables = children;
		lookup = generateLookup(children);
		totalNumItems = 1;		// for the empty AlarmGroup
	}

	/* *******************************  Methods from Listable  ****************************** */

	/**
	 * Returns whether this is an alarm or not.
	 * @return always returns false
	 */
	@Override @Contract(pure = true)
	public boolean isAlarm() { return false; }

	/**
	 * Gets the name of the listable.
	 * @return the name, will not be null
	 */
	@Override @Contract(pure = true) @NotNull
	public String getListableName() { return name; }

	/**
	 * Sets the name of the folder. If the new name is invalid, will no do anything.
	 * @param newName the new name, can be null
	 */
	@Override
	public void setListableName(@Nullable String newName) {
		if (newName == null || newName.length() == 0 || newName.indexOf('\t') != -1) {
			Log.e(TAG, "New name is invalid.");
			return;
		}
		name = newName;
	}

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
	 * Returns whether the folder is active or not. Doesn't take into account parent folders.
	 */
	@Override @Contract(pure = true)
	public boolean isActive() { return isActive; }

	/**
	 * Sets whether this folder is active or not.
	 * @param isOn the new active state of the folder
	 */
	@Override
	public void setActive(boolean isOn) { isActive = isOn; }

	/**
	 * Changes the active state of the folder to on (active).
	 */
	@Override
	public void turnOn() { isActive = true; }

	/**
	 * Changes the active state of the folder to off (inactive).
	 */
	@Override
	public void turnOff() { isActive = false; }

	/**
	 * Toggles the active state of the folder (if it's on, turn it off; of it's off, turn it on).
	 */
	@Override
	public void toggleActive() { isActive = !isActive; }

	/**
	 * Gets the size of the folder. If the folder is open, will count its children in the size. If
	 * the folder is closed, it will count only itself (returns 1).
	 * @return the size of the folder, must be at least 1
	 */
	@Override @Contract(pure = true)
	public int size() {
		if (isOpen) {
			return totalNumItems;
		}
		return 1;
	}

	/**
	 * Clones the folder and returns the new folder.
	 * @return the new folder, a deep copy of the first
	 */
	@Nullable @Override @Contract(pure = true)
	public Listable clone() {
		AlarmGroup that = null;
		try {
			that = (AlarmGroup) super.clone();

			that.listables = new ArrayList<>();
			for (int i = 0; i < this.listables.size(); i++) {
				that.listables.add(this.listables.get(i).clone());
			}
			// TODO: generate a new lookup or copy it?
			that.lookup = generateLookup(that.listables);
			// TODO: deep copy any object fields (only copies the reference)
		} catch (CloneNotSupportedException e) { e.printStackTrace(); }

		return that;
	}

	/**
	 * Creates an edit string for the current folder.
	 * <br/>
	 * Current edit string format (separated by tabs):
	 * [name]	[isActive]
	 */
	@NotNull @Override @Contract(pure = true)
	public String toEditString() {
		// TODO: implement toString for AlarmGroup
		String res = name;
		res += '\t' + Boolean.toString(isActive);

		return res;
	}

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

		for (Listable l : listables) {
			// need to add a tab to all lines inside
			String[] lines = l.toStoreString().split("\n");		// recursive call with extra steps
			for (String line : lines) { res.append('\t').append(line).append('\n'); }
		}
		res.deleteCharAt(res.length() - 1);		// deleting the last \n

		return res.toString();
	}

	/* ***************************  Getter and Setter Methods  ****************************** */

	/**
	 * Gets whether the folder is open or closed.
	 */
	@Contract(pure = true)
	boolean getIsOpen() { return isOpen; }

	/**
	 * Toggles the folder open state (if it was open, close it; if it was closed, open it).
	 */
	void toggleOpen() { isOpen = !isOpen; }

	/**
	 * Gets the lookup of the folder. If the folder is open, will return the real lookup, but if it's
	 * closed, will return an empty lookup.
	 * @return the lookup of the folder, will not be null but can be empty
	 */
	@Contract(pure = true) @NotNull
	ArrayList<Integer> getLookup() {
		if (isOpen) {
			return lookup;
		}
		else {
			return new ArrayList<>();
		}
	}

	/**
	 * Gets the listables within the folder.
	 * @return the listables, will not be null
	 */
	@Contract(pure = true) @NotNull
	ArrayList<Listable> getListables() { return listables; }

	/**
	 * Sets the listables within the folder. If the new list is invalid (the list or any listables
	 * within it are null), will not do anything.
	 * @param listables a new list of listables to use, can be null
	 */
	void setListables(@Nullable ArrayList<Listable> listables) {
		if (listables == null) {
			Log.e(TAG, "New list of Listables is null.");
			return;
		}
		for (Listable l : listables) {
			if (l == null) {
				Log.e(TAG, "Listable in new list of Listables is null.");
				return;
			}
		}

		this.listables = listables;
		refreshLookup();
	}

	// there is no setter for totalNumItems

	/* ************************************  Static Methods  ********************************** */

	/**
	 * Takes edit strings to rebuild into a new AlarmGroup
	 * @param src the edit string to create from
	 */
	@Nullable @Contract(pure = true)
	static AlarmGroup fromEditString(String src) {
		if (src == null) {
			Log.e(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			Log.e(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != NUM_EDIT_FIELDS) {
			Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		AlarmGroup dest = new AlarmGroup(fields[0]);
		dest.setActive(Boolean.parseBoolean(fields[1]));

		return dest;
	}

	/**
	 * Takes store strings to rebuild into a new AlarmGroup
	 * @param currContext the current context
	 * @param src the store string to create from
	 */
	@Nullable @Contract(pure = true)
	static AlarmGroup fromStoreString(Context currContext, String src) {
		if (src == null) {
			Log.e(TAG, "Store string is null.");
			return null;
		} else if (src.length() == 0) {
			Log.e(TAG, "Store string is empty.");
			return null;
		}

		String[] lines = src.split("\n");		// line by line

		// first line for the AlarmGroup itself
		if (!lines[0].startsWith("f\t")) {
			Log.e(TAG, "Store string has an unknown ID field.");
			return null;
		}

		AlarmGroup dest = fromEditString(lines[0].substring(2));
		if (dest == null) {
			Log.e(TAG, "Store string has an invalid first line.");
			return null;
		}

		for (int i = 1; i < lines.length; i++) {
			// not a child of the current AlarmGroup
			if (!lines[i].startsWith("\t")) {
				Log.e(TAG, "Store string is formatted incorrectly (children aren't indented).");
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
					Log.e(TAG, "Child alarm store string was invalid.");
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
					new_src.append("\n").append(lines[j]);
				}

				// didn't trip on the break statement, so we want it to end after this loop (j =
				// lines.length), so we can set it to any int >= j - 1
				if (i != j - 1) { i = j; }

				// essentially a recursive call with some looping involved
				AlarmGroup curr_folder = fromStoreString(currContext, new_src.toString());
				if (curr_folder == null) {
					Log.e(TAG, "Child AlarmGroup store string was invalid.");
					return null;
				}

				dest.addListable(curr_folder);
			}
			else {
				Log.e(TAG, "Store string line has an unknown ID field.");
				return null;
			}
		}
		return dest;
	}

	/**
	 * Counts the number of Listables in the list. Does not include the parent object in the count.
	 * @param listables the list of Listables to count
	 * @return number of Listables in the list
	 */
	@Contract(pure = true)
	private static int getSizeOfList(ArrayList<Listable> listables) {
		int len = 0;
		for (Listable l : listables) { len += l.size(); }
		return len;
	}

	/**
	 * Generates a lookup list for a given set of Listables. Lookup list contains the first absolute
	 * index for each Listable in the list.
	 * @param data the list of Listables to read
	 * @return the lookup list, can be empty but never null
	 */
	@Contract(pure = true)
	private static ArrayList<Integer> generateLookup(final ArrayList<Listable> data) {
		if (data == null) {
			Log.e(TAG, "Input data to generateLookup was null.");
			return new ArrayList<>();
		}

		ArrayList<Integer> res = new ArrayList<>();
		int currIndex = 0;

		if (data.size() == 0)
			return res;

		res.add(currIndex);		// for first listable

		Listable l;

		// iterates over all Listables except the last one (we don't care how many it has)
		for (int i = 0; i < data.size() - 1; i++) {
			l = data.get(i);
			if (l == null) {
				Log.e(TAG, "Listable within given data was null.");
				return new ArrayList<>();
			}
			if (!l.isAlarm()) { ((AlarmGroup) l).refreshLookup(); }

			currIndex += l.size();
			res.add(currIndex);
		}

		return res;
	}

	/**
	 * Searches for a Listable at srcIndex and returns data within a ListableInfo about it.
	 * @param data The actual list of Listables to search through. Won't change the data within
	 *             the structure.
	 * @param lookup The lookup list for the data list. Won't change the data within the structure.
	 * @param srcIndex the absolute position of the Listable to search for
	 * @return a ListableInfo describing everything it can about the Listable at srcIndex, or null
	 * if the Listable was not found
	 */
	@Nullable @Contract(pure = true)
	private static ListableInfo getListableInfo(ArrayList<Listable> data, ArrayList<Integer> lookup,
										final int srcIndex) {
		ListableInfo info = new ListableInfo();
		int index = findOuterListableIndex(lookup, srcIndex, AlarmGroup.getSizeOfList(data));
		int indents = 0;

		// represents the absolute index of the Listable we're looking for in the current folder
		int absIndex = srcIndex;
		int currFolderIndex = index;
		AlarmGroup currFolder = null;

		while (index != -1) {
			// must be the element itself in the current folder
			if (lookup.get(index) == absIndex) {
				info.absIndex = srcIndex;
				info.relIndex = index;
				info.numIndents = indents;
				info.absParentIndex = currFolderIndex;
				info.listable = data.get(index);
				info.parent = currFolder;

				return info;
			}

			// listable is within an AlarmGroup, subtract 1 to take into account the folder itself
			absIndex = absIndex - 1 - index;
			currFolderIndex += index + 1;		// take into account the folder itself
			currFolder = (AlarmGroup) data.get(index);
			lookup = currFolder.getLookup();
			data = currFolder.getListables();
			index = findOuterListableIndex(lookup, absIndex, currFolder.size() - 1);
			indents++;
		}
		Log.e(TAG, "Could not find the specified Listable's info in the source data/lookup.");
		return null;
	}

	/**
	 * Searches for a Listable in a Listable lookup list. Gets the index of either the Listable
	 * itself or the Listable that contains it.
	 * @param lookup the lookup list corresponding to the current layer
	 * @param index The index within the current layer that we're looking for, must be less than
	 *              total item count. Logs an error if out of bounds and method will return -1
	 * @param total the total number of items, just within the current layer (includes children)
	 * @return the index of the containing Listable (or the Listable itself), or -1 if it wasn't
	 * found or datasetLookup is empty.
	 */
	@Contract(pure = true)
	static int findOuterListableIndex(
			final ArrayList<Integer> lookup, final int index, final int total) {
		int max = lookup.size();

		if (max == 0 || index >= total || index < 0) {
			Log.e(TAG, "Listable index is out of bounds!");
			Log.e(TAG, "max = " + max + ", index = " + index + ", total = " + total);
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
		Log.e(TAG, "Could not find the specified Listable's parent index within the source data/lookup.");
		return -1;
	}

	/* *********************  Convenience Methods for getListableInfo()  ************************ */

	/**
	 * Gets the ListableInfo for the listable at the specified index
	 * @param absIndex absolute index to search for
	 * @return the Listable or null if not found
	 */
	@Contract(pure = true)
	ListableInfo getListableInfo(final int absIndex) {
		return getListableInfo(listables, lookup, absIndex);
	}

	/**
	 * Gets the relative listable index at the specified absolute index
	 * @param absIndex absolute index to search for
	 * @return the index or -1 if not found
	 */
	@Contract(pure = true)
	int getListableIndexAtAbsIndex(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return -1;
		}

		return i.relIndex;
	}

	/**
	 * Gets the parent listable of the listable at the specified index
	 * @param absIndex absolute index to search for
	 * @return the parent Listable (can be the current Listable)
	 */
	@NotNull @Contract(pure = true)
	AlarmGroup getParentListableAtAbsIndex(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) return this;

		return i.parent;
	}

	/**
	 * Gets the number of indents of the listable at the specified index
	 * @param absIndex absolute index to search for
	 * @return the number of indents (0 means not nested), or -1 if not found
	 */
	@Contract(pure = true)
	int getNumIndents(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return -1;
		}

		return i.numIndents;
	}

	/* **************************  Manipulating Listables (Relative)  *************************** */

	/**
	 * Gets the Listable at the given relative index.
	 * @param relIndex the relative index of the Listable required
	 * @return returns the Listable at the relative index, or null if not found
	 */
	@Nullable @Contract(pure = true)
	Listable getListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			Log.e(TAG, "Couldn't get Listable. Index is out of bounds.");
			return null;
		}
		return listables.get(relIndex);
	}

	/**
	 * Sets a listable in the dataset using its relative index.
	 * @param relIndex the relative index to set with
	 * @param item the new Listable to set relIndex to
	 */
	private void setListable(final int relIndex, final Listable item) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			Log.e(TAG, "Couldn't set Listable. Index is out of bounds.");
			return;
		} else if (item == null) {
			Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		int indexChange = item.size() - listables.get(relIndex).size();
		listables.set(relIndex, item);

		// add index change to all lookup indices and totalNumItems
		for (int i = relIndex + 1; i < listables.size(); i++) {
			lookup.set(i, lookup.get(i) + indexChange);
		}
		totalNumItems += indexChange;
	}

	/**
	 * Adds a listable to the end of the current folder.
	 * @param item the Listable to add to the folder
	 */
	void addListable(final Listable item) {
		if (item == null) {
			Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		lookup.add(totalNumItems - 1);
		listables.add(item);
		totalNumItems += item.size();
	}

	/**
	 * Deletes a listable at the specified relative index.
	 * @param relIndex the relative index of the Listable to delete
	 */
	private void deleteListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			Log.e(TAG, "Couldn't delete Listable. Index is out of bounds.");
			return;
		}
		listables.remove(relIndex);
		refreshLookup();
	}

	/* **************************  Manipulating Listables (Absolute)  *************************** */

	/**
	 * Gets the listable at the specified absolute index
	 * @param absIndex absolute index to search for
	 * @return the Listable or null if not found
	 */
	@Nullable
	Listable getListableAbs(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return null;
		}

		return i.listable;
	}

	/**
	 * Sets item as the new Listable in the dataset at the absolute index. If the index doesn't exist,
	 * doesn't do anything.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	void setListableAbs(final int absIndex, final Listable item) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return;
		}

		if (i.parent == null) {
			setListable(i.relIndex, item);
		}
		else {
			i.parent.setListable(i.relIndex, item);
		}
		refreshLookup();
	}

	/**
	 * Deletes the listable at the specified absolute index.
	 * @param absIndex the absolute index of the Listable to delete
	 */
	void deleteListableAbs(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return;
		}

		if (i.parent == null) {
			deleteListable(i.relIndex);
		}
		else {
			i.parent.deleteListable(i.relIndex);
		}
		refreshLookup();
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
	 * Returns a string describing the AlarmGroup and any AlarmGroups within it
	 * @return a String that can be decoded with fromStoreString()
	 */
	@NotNull @Contract(pure = true)
	String toReducedString() {
		StringBuilder builder = new StringBuilder(getStoreStringSingle());
		builder.append('\n');
		for (Listable l : listables) {
			if (!l.isAlarm()) {
				// recursive call
				builder.append('\t').append( ((AlarmGroup)l).toReducedString()).append('\n');
			}
		}
		return builder.toString();
	}

	/**
	 * Updates the lookup list and the total number of items in the AlarmGroup.
	 */
	private void refreshLookup() {
		lookup = generateLookup(listables);
		totalNumItems = getSizeOfList(listables) + 1;
	}
}
