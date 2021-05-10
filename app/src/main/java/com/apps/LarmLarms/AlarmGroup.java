package com.apps.LarmLarms;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Holds a list of Alarms and can mask Alarms.
 */

public final class AlarmGroup implements Listable, Cloneable {
	private static final String TAG = "AlarmGroup";

	/**
	 * Number of fields in a single edit string (don't have one for store strings because they can
	 * span multiple lines and can have a different number of fields dependent on type)
	 */
	private static final int NUM_EDIT_FIELDS = 2;

	private Context context;

	/**
	 * Stores the name of the folder. Only restricted character: tabs. If tabs are present, the
	 * class will log errors.
	 */
	private String name;
	private boolean isActive;
	private boolean isOpen;

	// contains the child Alarms and AlarmGroups
	private ArrayList<Listable> listablesInside;
	private ArrayList<Integer> listablesLookup;

	private int totalNumItems;

	/* ***********************************  Constructors  *********************************** */

	// default constructor
	public AlarmGroup(Context currContext) {
		// TODO: init dummy data for AlarmGroup
		context = currContext;
		name = "default name";
		isActive = true;
		isOpen = true;
		listablesInside = new ArrayList<>();
		listablesLookup = new ArrayList<>();
		totalNumItems = 1;		// for the empty AlarmGroup
	}

	// gives a name to the group
	public AlarmGroup(Context currContext, String title) {
		this(currContext);
		setListableName(title);
	}

	/* *******************************  Methods from Listable  ****************************** */
	@Override
	public boolean isAlarm() { return false; }

	@Override
	public String getListableName() { return name; }

	@Override
	public void setListableName(String newName) {
		if (newName == null || newName.length() == 0 || newName.indexOf('\t') != -1) {
			Log.e(TAG, "New name is invalid.");
			return;
		}
		name = newName;
	}

	@Override
	public String getRepeatString() { return ""; }

	@Override
	public String getNextRingTime() { return ""; }

	@Override
	public boolean isActive() { return isActive; }

	@Override
	public int getNumItems() {
		if (isOpen) {
			return totalNumItems;
		}
		return 1;
	}

	@Override
	public Listable clone() {
		AlarmGroup that = null;
		try {
			that = (AlarmGroup) super.clone();

			that.listablesInside = new ArrayList<>();
			for (int i = 0; i < this.listablesInside.size(); i++) {
				that.listablesInside.add(this.listablesInside.get(i).clone());
			}
			// TODO: generate a new lookup or copy it?
			that.listablesLookup = generateLookup(that.listablesInside);
			// TODO: deep copy any object fields (only copies the reference)
		} catch (CloneNotSupportedException e) { e.printStackTrace(); }

		return that;
	}

	/**
	 * Creates an edit string for the current folder. Only stores the folder itself and doesn't have a
	 * type identifier
	 */
	@Override
	public String toEditString() {
		// TODO: implement toString for AlarmGroup
		String res = name;
		res += '\t' + Boolean.toString(isActive);

		return res;
	}

	/**
	 * Creates a string for storing the current folder. Stores folder with type identifier and
	 * recursively stores all its children as well
	 */
	@Override
	public String toStoreString() {
		StringBuilder res = new StringBuilder("f\t" + toEditString());
		res.append('\n');

		for (Listable l : listablesInside) {
			// need to add a tab to all lines inside
			String[] lines = l.toStoreString().split("\n");		// recursive call with extra steps
			for (String line : lines) { res.append('\t').append(line).append('\n'); }
		}

		return res.toString();
	}

	/* ***************************  Getter and Setter Methods  ****************************** */

	// the corresponding getter is replaced by isActive()
	public void setActive(boolean isOn) { isActive = isOn; }
	public void turnOn() { isActive = true; }
	public void turnOff() { isActive = false; }
	public void toggleActive() { isActive = !isActive; }

	public boolean getIsOpen() { return isOpen; }
	public void openFolder() {
		isOpen = true;
		refreshLookup();
	}
	public void closeFolder() {
		isOpen = false;
		refreshLookup();
	}
	public void toggleOpen() {
		isOpen = !isOpen;
		refreshLookup();
	}

	/**
	 * Gets the Listable at the given relative index.
	 * @param rel_index the relative index of the Listable required
	 * @return returns the Listable at the relative index, or null if not found
	 */
	public Listable getListable(int rel_index) {
		if (rel_index < 0 || rel_index >= listablesInside.size()) {
			Log.e(TAG, "Couldn't get Listable. Index is out of bounds.");
			return null;
		}
		return listablesInside.get(rel_index);
	}

	public void replaceListable(int rel_index, Listable item) {
		if (rel_index < 0 || rel_index >= listablesInside.size()) {
			Log.e(TAG, "Couldn't set Listable. Index is out of bounds.");
			return;
		} else if (item == null) {
			Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		int indexChange = item.getNumItems() - listablesInside.get(rel_index).getNumItems();
		listablesInside.set(rel_index, item);

		// add index change to all lookup indices and totalNumItems
		for (int i = rel_index + 1; i < listablesInside.size(); i++) {
			listablesLookup.set(i, listablesLookup.get(i) + indexChange);
		}
		totalNumItems += indexChange;
	}

	public void addListable(Listable item) {
		if (item == null) {
			Log.e(TAG, "Couldn't set Listable. Item is null.");
			return;
		}

		listablesLookup.add(totalNumItems - 1);
		listablesInside.add(item);
		totalNumItems += item.getNumItems();
	}

	// no setter for listableLookups
	public ArrayList<Integer> getListablesLookup() {
		if (isOpen) {
			return listablesLookup;
		}
		else {
			ArrayList<Integer> res = new ArrayList<>();
			res.add(0);
			return res;
		}
	}
	public int getListableLookup(final int index) { return listablesLookup.get(index); }
	public void refreshLookup() {
		if (isOpen) {
			listablesLookup = generateLookup(listablesInside);
			totalNumItems = getSizeOfList(listablesInside) + 1;
		}
		else {
			listablesLookup = new ArrayList<>();
			listablesLookup.add(0);
			totalNumItems = 1;
		}
	}

	// a shallow copy of listables within the folder
	public ArrayList<Listable> getListablesInside() { return listablesInside; }
	public void setListablesInside(ArrayList<Listable> listables) {
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

		listablesInside = listables;
		refreshLookup();
	}

	// there is no setter for totalNumItems

	/* ************************************  Static Methods  ********************************** */

	/**
	 * Takes edit strings to rebuild into a new AlarmGroup
	 * @param currContext the current context
	 * @param src the edit string to create from
	 */
	public static AlarmGroup fromEditString(Context currContext, String src) {
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

		AlarmGroup dest = new AlarmGroup(currContext, fields[0]);
		dest.setActive(Boolean.parseBoolean(fields[1]));

		return dest;
	}

	/**
	 * Takes store strings to rebuild into a new AlarmGroup
	 * @param currContext the current context
	 * @param src the store string to create from
	 */
	public static AlarmGroup fromStoreString(Context currContext, String src) {
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

		AlarmGroup dest = fromEditString(currContext, lines[0].substring(2));
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
	public static int getSizeOfList(ArrayList<Listable> listables) {
		int len = 0;
		for (Listable l : listables) { len += l.getNumItems(); }
		return len;
	}

	/**
	 * Generates a lookup list for a given set of Listables. Lookup list contains the first absolute
	 * index for each Listable in the list.
	 * @param data the list of Listables to read
	 * @return the lookup list, can be empty but never null
	 */
	public static ArrayList<Integer> generateLookup(final ArrayList<Listable> data) {
		if (data == null) {
			Log.e(TAG, "Input data to generateLookup was null.");
			return new ArrayList<>();
		}

		ArrayList<Integer> res = new ArrayList<>();
		int currIndex = 0;
		res.add(currIndex);

		Listable l;

		// iterates over all Listables except the last one (we don't care how many it has)
		for (int i = 0; i < data.size() - 1; i++) {
			l = data.get(i);
			if (l == null) {
				Log.e(TAG, "Listable within given data was null.");
				return new ArrayList<>();
			}
			if (!l.isAlarm()) { ((AlarmGroup) l).refreshLookup(); }

			currIndex += l.getNumItems();
			res.add(currIndex);
		}

		return res;
	}

	/**
	 * Searches for a specific Listable at the given src_index within the given dataset.
	 * @param data The actual list of Listables to search through. Won't change the data within
	 *             the structure.
	 * @param lookup The lookup list for the data list. Won't change the data within the structure.
	 * @param src_index The absolute index of the Listable we're looking for.
	 * @return returns the Listable at the specified index, or null if not found.
	 */
	public static Listable getListableAtAbsIndex(ArrayList<Listable> data,
												 ArrayList<Integer> lookup, final int src_index) {
		// represents the relative index of the Listable that is or contains the Listable we're looking for
		int index = findOuterListableIndex(lookup, src_index, AlarmGroup.getSizeOfList(data));

		// represents the absolute index of the Listable we're looking for in the current folder
		int abs_index = src_index;
		AlarmGroup curr_folder;

		Log.i(TAG, "src_index is: " + src_index + ", and index is " + index);
		Log.i(TAG, "data is: " + data + ", and lookup is: " + lookup);

		while (index != -1) {
			// must be the element itself in the current folder
			if (lookup.get(index) == abs_index) { return data.get(index); }

			// otherwise, is within an AlarmGroup
			// minus 1 to take into account the folder itself
			abs_index = abs_index - 1 - index;
			curr_folder = (AlarmGroup) data.get(index);
			lookup = curr_folder.getListablesLookup();
			data = curr_folder.getListablesInside();
			index = findOuterListableIndex(lookup, abs_index, curr_folder.getNumItems() - 1);
		}
		Log.e(TAG, "Could not find the specified Listable within the source data/lookup.");
		return null;
	}

	/**
	 * Searches for the Listable's relative index at the given src_index within the given dataset.
	 * @param data The actual list of Listables to search through. Won't change the data within
	 *             the structure.
	 * @param lookup The lookup list for the data list. Won't change the data within the structure.
	 * @param src_index The absolute index of the Listable whose parent we're looking for.
	 * @return returns the Listable's relative index at the specified absolute index, or -1 if not found.
	 */
	public static int getListableIndexAtAbsIndex(ArrayList<Listable> data,
												 ArrayList<Integer> lookup, final int src_index) {
		// represents the relative index of the Listable that is or contains the Listable we're looking for
		int index = findOuterListableIndex(lookup, src_index, AlarmGroup.getSizeOfList(data));

		// represents the absolute index of the Listable we're looking for in the current folder
		int abs_index = src_index;
		AlarmGroup curr_folder;

		while (index != -1) {
			// must be the element itself in the current folder
			if (lookup.get(index) == abs_index) { return index; }

			// otherwise, is within an AlarmGroup
			// minus 1 to take into account the folder itself
			abs_index = abs_index - 1 - index;
			curr_folder = (AlarmGroup) data.get(index);
			lookup = curr_folder.getListablesLookup();
			data = curr_folder.getListablesInside();
			index = findOuterListableIndex(lookup, abs_index, curr_folder.getNumItems() - 1);
		}
		// TODO: throw an exception?
		Log.e(TAG, "Could not find the specified Listable's absolute index within the source data/lookup.");
		return -1;
	}

	/**
	 * Searches for the parent of the Listable at the given src_index within the given dataset.
	 * @param data The actual list of Listables to search through. Won't change the data within
	 *             the structure.
	 * @param lookup The lookup list for the data list. Won't change the data within the structure.
	 * @param src_index The absolute index of the Listable whose parent we're looking for.
	 * @return returns the Listable at the specified index, or null if not found. If the original
	 * data list is the parent, will also return null.
	 */
	public static AlarmGroup getParentListableAtAbsIndex(ArrayList<Listable> data,
														 ArrayList<Integer> lookup, final int src_index) {
		// represents the relative index of the Listable that is or contains the Listable we're looking for
		int index = findOuterListableIndex(lookup, src_index, AlarmGroup.getSizeOfList(data));

		// represents the absolute index of the Listable we're looking for in the current folder
		int abs_index = src_index;
		AlarmGroup curr_folder = null;

		while (index != -1) {
			// must be the element itself in the current folder
			if (lookup.get(index) == abs_index) { return curr_folder; }

			// otherwise, is within an AlarmGroup
			// minus 1 to take into account the folder itself
			abs_index = abs_index - 1 - index;
			curr_folder = (AlarmGroup) data.get(index);
			lookup = curr_folder.getListablesLookup();
			data = curr_folder.getListablesInside();
			index = findOuterListableIndex(lookup, abs_index, curr_folder.getNumItems() - 1);
		}
		// TODO: throw an exception?
		Log.e(TAG, "Could not find the specified Listable's parent within the source data/lookup.");
		return null;
	}

	/**
	 * Finds how many AlarmGroups a specific Listable in the given dataset is nested under (not
	 * being nested is considered 0).
	 * @param src_index the absolute position of the Listable to search for
	 * @return the number of AlarmGroups the Listable is nested under, or -1 if the index is out of
	 * 		   bounds
	 */
	public static int getNumIndents(ArrayList<Listable> data, ArrayList<Integer> lookup, final int src_index) {
		// represents the relative index of the Listable that is or contains the Listable we're looking for
		int index = findOuterListableIndex(lookup, src_index, AlarmGroup.getSizeOfList(data));
		if (index == -1) { return -1; }

		int indents = 0;

		// represents the absolute index of the Listable we're looking for in the current folder
		int abs_index = src_index;
		AlarmGroup curr_folder;

		while (index != -1) {
			// must be the element itself in the current folder
			if (lookup.get(index) == abs_index) { return indents; }

			// otherwise, is within an AlarmGroup
			// minus 1 to take into account the folder itself
			abs_index = abs_index - 1 - index;
			curr_folder = (AlarmGroup) data.get(index);
			lookup = curr_folder.getListablesLookup();
			data = curr_folder.getListablesInside();
			index = findOuterListableIndex(lookup, abs_index, curr_folder.getNumItems() - 1);
			indents++;
		}
		Log.e(TAG, "Could not find the specified Listable's number of indents within the source data/lookup.");
		return -1;

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
	public static int findOuterListableIndex(
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

	/* ***********************************  Other Methods  ************************************** */

	@Override
	public String toString() { return name; }
}
