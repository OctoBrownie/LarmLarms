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
	 * Stores the name of the folder. Restricted characters: tabs and backslashes. If the user tries
	 * to set them as part of the name, they will be automatically stripped out.
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
	public AlarmGroup() { this("", new ArrayList<Listable>()); }

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
		// invalid data for now
		name = "";
		listables = new ArrayList<>();
		lookup = new ArrayList<>();

		isActive = true;
		isOpen = true;

		// validating data passed through constructor
		setListableName(title);
		setListables(children);		// also automatically refreshes lookup/size
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
	 * Sets the name of the folder. If an error is encountered, return code will be nonzero, based
	 * on which error is encountered. See Listable documentation for return codes.
	 * @param newName the new name, can be null
	 * @return 0 (no error) or an error code specified in Listable documentation
	 */
	@Override
	public int setListableName(@Nullable String newName) {
		if (newName == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New name is null.");
			return 1;
		}

		if (newName.indexOf('\t') != -1 || newName.indexOf('/') != -1) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New name has tabs in it.");
			return 2;
		}

		name = newName;
		return 0;
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
			for (Listable l : this.listables) {
				that.listables.add(l.clone());
			}
			that.lookup = new ArrayList<>();
			that.lookup.addAll(this.lookup);
			// TODO: deep copy any object fields (only copies the reference)
		} catch (CloneNotSupportedException e) { e.printStackTrace(); }

		return that;
	}

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
	 * Creates an edit string for the current folder.
	 * <br/>
	 * Current edit string format (separated by tabs):
	 * [name]	[isActive]
	 */
	@NotNull @Override @Contract(pure = true)
	public String toEditString() {
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
			if (BuildConfig.DEBUG) Log.v(TAG, "New list of Listables is null.");
			return;
		}
		for (Listable l : listables) {
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Listable in new list of Listables is null.");
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
	 * @param listables the list of Listables to count, can be null
	 * @return number of Listables in the list, or zero if list is null
	 */
	@Contract(pure = true)
	private static int getSizeOfList(@Nullable ArrayList<Listable> listables) {
		if (listables == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "List of listables to get size of was null.");
			return 0;
		}

		int len = 0;
		for (Listable l : listables) { len += l.size(); }
		return len;
	}

	/**
	 * Generates a lookup list for a given set of Listables. Lookup list contains the first absolute
	 * index for each Listable in the list. Also updates all listables within the group.
	 * @param data the list of Listables to read, can be null
	 * @return the lookup list, can be empty but never null
	 */
	@Contract(pure = true)
	private static ArrayList<Integer> generateLookup(@Nullable final ArrayList<Listable> data) {
		if (data == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Input data to generateLookup was null.");
			return new ArrayList<>();
		}

		ArrayList<Integer> res = new ArrayList<>();
		int currIndex = 0;

		if (data.size() == 0)
			return res;

		Listable l;

		// iterates over all Listables except the last one (we don't care how many it has)
		for (int i = 0; i < data.size(); i++) {
			res.add(currIndex);

			l = data.get(i);
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Listable within given data was null.");
				return new ArrayList<>();
			}
			if (!l.isAlarm()) { ((AlarmGroup) l).refreshLookup(); }

			currIndex += l.size();
		}

		return res;
	}

	/**
	 * Searches for a Listable at srcIndex and returns data within a ListableInfo about it. Fills all
	 * fields of the ListableInfo.
	 * @param root the folder to look through for this Listable. Will not change the data within it
	 * @param srcIndex the absolute position of the Listable to search for
	 * @return a ListableInfo describing everything it can about the Listable at srcIndex, or null
	 * if the Listable was not found
	 */
	@Nullable @Contract(pure = true)
	private static ListableInfo getListableInfo(@Nullable AlarmGroup root, final int srcIndex) {
		if (root == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Listable to look through is null.");
			return null;
		}

		ArrayList<Listable> data = root.listables;
		ArrayList<Integer> lookup = root.lookup;

		// the final ListableInfo to store stuff in
		ListableInfo info = new ListableInfo();

		// the relative index to the current folder
		int index = findOuterListableIndex(lookup, srcIndex, AlarmGroup.getSizeOfList(data));

		// the number of indents for this listable
		int indents = 0;

		// represents the absolute index of the Listable we're looking for in the current folder
		int absIndex = srcIndex;

		// represents the absolute index of the current folder we're in, starts at -1 to account for
		// something later and to show that there the listable doesn't have a parent (besides the
		// current folder)
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
				info.listable = data.get(index);
				info.parent = currFolder;

				// if only within the root folder
				if (pathBuilder.length() == 0) pathBuilder.append(root.getListableName());
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
			lookup = currFolder.getLookup();
			data = currFolder.getListables();

			indents++;

			// new index relative to the new folder
			index = findOuterListableIndex(lookup, absIndex, currFolder.size() - 1);
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
	 * @return the index of the containing Listable (or the Listable itself), or -1 if it wasn't
	 * found or datasetLookup is empty.
	 */
	@Contract(pure = true)
	static int findOuterListableIndex(@Nullable final ArrayList<Integer> lookup, final int index,
									  final int total) {
		if (lookup == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "Listable lookup was null.");
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

	/* *********************  Convenience Methods for getListableInfo()  ************************ */

	/**
	 * Gets the ListableInfo for the listable at the specified index
	 * @param absIndex absolute index to search for
	 * @return the Listable or null if not found
	 */
	@Contract(pure = true)
	ListableInfo getListableInfo(final int absIndex) { return getListableInfo(this, absIndex); }

	/**
	 * Gets the relative listable index at the specified absolute index
	 * @param absIndex absolute index to search for
	 * @return the index or -1 if not found
	 */
	@Contract(pure = true)
	int getListableIndexAtAbsIndex(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return -1;
		}

		return i.relIndex;
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return -1;
		}

		return i.numIndents;
	}

	/* **************************  Manipulating Listables (Relative)  *************************** */

	/**
	 * Gets the Listable at the given relative index.
	 * @param relIndex the relative index of the Listable required
	 * @return the Listable at the relative index, or null if not found
	 */
	@Nullable @Contract(pure = true)
	Listable getListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't get Listable. Index is out of bounds.");
			return null;
		}
		return listables.get(relIndex);
	}

	/**
	 * Gets the first AlarmGroup with the given name (should be the only one) within the current
	 * folder.
	 * @param name the name to search for, shouldn't be null
	 * @return the folder with the given name, or null if not found
	 */
	@Nullable @Contract(pure = true)
	AlarmGroup getFolder(@NotNull final String name) {
		for (Listable l : listables) {
			if (l instanceof AlarmGroup && l.getListableName().equals(name))
				return (AlarmGroup) l;
		}
		return null;
	}

	/**
	 * Sets a listable in the dataset using its relative index.
	 * @param relIndex the relative index to set with
	 * @param item the new Listable to set relIndex to
	 */
	private void setListable(final int relIndex, final Listable item) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Index is out of bounds.");
			return;
		} else if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Item is null.");
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		lookup.add(totalNumItems - 1);
		listables.add(item);
		totalNumItems += item.size();
	}

	/**
	 * Deletes a listable at the specified relative index.
	 * @param relIndex the relative index of the Listable to delete
	 * @return the listable deleted, or null
	 */
	@Nullable
	private Listable deleteListable(final int relIndex) {
		if (relIndex < 0 || relIndex >= listables.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't delete Listable. Index is out of bounds.");
			return null;
		}
		Listable l = listables.remove(relIndex);
		refreshLookup();
		return l;
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
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
	 * @return the listable that was deleted or null if there was an error
	 */
	@Nullable
	Listable deleteListableAbs(final int absIndex) {
		ListableInfo i = getListableInfo(absIndex);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Listable at absolute index " + absIndex + " was not found.");
			return null;
		}

		Listable l;
		if (i.parent == null) {
			l = deleteListable(i.relIndex);
		}
		else {
			l = i.parent.deleteListable(i.relIndex);
		}
		refreshLookup();
		return l;
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

		for (Listable child : parent.listables) {
			if (!child.isAlarm()) {
				pathList.addAll(toPathList(prefix, (AlarmGroup) child));
			}
		}
		return pathList;
	}

	/**
	 * Updates the lookup list and the total number of items in the AlarmGroup.
	 */
	void refreshLookup() {
		lookup = generateLookup(listables);
		totalNumItems = getSizeOfList(listables) + 1;
	}
}
