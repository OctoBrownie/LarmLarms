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
import java.util.List;

/**
 * Holds a list of Alarms and can mask Alarms.
 */
public class AlarmGroup extends Item {
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
	protected List<Item> items;

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
	 * @param name the new name of the folder
	 */
	public AlarmGroup(@Nullable String name) {
		this(name, new ArrayList<>(),
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Initializes a new AlarmGroup with a name and contents.
	 * @param name the new name of the folder
	 * @param children the new items within the folder
	 */
	public AlarmGroup(@Nullable String name, @NotNull List<Item> children) {
		this(name, children,
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Initializes a new AlarmGroup with a name and contents.
	 * @param name the new name of the folder
	 * @param children the new items within the folder
	 * @param id the id of the folder
	 */
	public AlarmGroup(@Nullable String name, @NotNull List<Item> children, int id) {
		super(name != null ? name : "new folder", id);

		items = new ArrayList<>();
		setItems(children);		// validating data
	}

	/* *******************************  Methods from item  ****************************** */

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
	public synchronized int size() { return items.size() + 1; }

	/**
	 * Determines whether other is equal to this AlarmGroup or not. Checks only for name and whether
	 * it's active or not.
	 * @param other the other object to compare to
	 * @return whether the two objects are equal or not
	 */
	@Contract("null -> false")
	public synchronized boolean equals(Object other) {
		if (!(other instanceof AlarmGroup)) return false;

		AlarmGroup that = (AlarmGroup) other;
		return this.name.equals(that.name) && this.isActive == that.isActive;
	}
	
	/**
	 * Compares this folder with the other object. Alarms are always considered "after" folders.
	 * Folders are compared with this precedence: name, then id (the lower id is first). It 
	 * shouldn't be necessary to compare anything further since their ids should always be unique. 
	 * @return this folder compared to the item (negative if this is first, positive if this is 
	 * second, 0 if they're equal).
	 */
	@Override
	public synchronized int compareTo(@NotNull Item other) {
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
	public synchronized String toEditString() { return "" + id + '\t' + name + '\t' + isActive; }

	/**
	 * Creates a string for storing the current folder. Stores folder with type identifier and
	 * recursively stores all its children as well.
	 * <br/>
	 * Current store string format (separated by tabs):
	 * [store identity string]
	 * 		[children store strings, separated by new lines]
	 */
	@NotNull @Override @Contract(pure = true)
	public synchronized String toStoreString() {
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
	 * Gets the items within the folder.
	 * @return the items, will not be null
	 */
	@Contract(pure = true) @NotNull
	synchronized List<Item> getItems() { return items; }

	/**
	 * Sets the items within the folder. If the new list is invalid (the list or any items
	 * within it are null), will not do anything.
	 * @param items a new list of items to use, can be null
	 */
	public synchronized void setItems(@Nullable List<Item> items) {
		if (items == null) {
			if (BuildConfig.DEBUG) Log.v(TAG, "New list of items is null.");
			return;
		}
		for (Item l : items) {
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "item in new list of items is null.");
				return;
			}
			l.setParent(this);
		}

		Collections.sort(items);
		this.items = items;
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

				dest.addItem(alarm);
			} else if (item.startsWith("f")) {
				// is an AlarmGroup with perhaps its own set of Alarms and AlarmGroups
				StringBuilder new_src = new StringBuilder(item);

				// look for the nested items and build another
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

				dest.addItem(curr_folder);
			}
			else {
				if (BuildConfig.DEBUG) Log.e(TAG, "Store string line has an unknown ID field.");
				return null;
			}
		}
		return dest;
	}

	/* ********************************  Manipulating Contents  ********************************* */

	/**
	 * Gets the item at the given relative index.
	 * @param index the index of the item required
	 * @return the item at the relative index, or null if not found
	 */
	@Nullable @Contract(pure = true)
	public synchronized Item getItem(final int index) {
		if (index < 0 || index >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't get item. Index is out of bounds.");
			return null;
		}
		return items.get(index);
	}

	/**
	 * Gets the AlarmGroup with the given relative path (should be the only one) in the current
	 * folder. Assumes the path includes the current folder.
	 * @param path the path to search for (includes the name of the folder), shouldn't be null
	 * @return the folder with the given name, or null if not found
	 */
	@Nullable @Contract(pure = true)
	private synchronized AlarmGroup getFolder(@NotNull final String path) {
		String[] folders = path.split("/");
		AlarmGroup currFolder = this, dummy = new AlarmGroup();
		for (int i = 1; i < folders.length - 1; i++) {
			dummy.name = folders[i];
			int index = Collections.binarySearch(currFolder.items, dummy, (Item i1, Item i2) -> {
				// ignore everything but type and name
				if (i1 instanceof Alarm && i2 instanceof AlarmGroup) return 1;
				else if (i1 instanceof AlarmGroup && i2 instanceof Alarm) return -1;
				return i1.name.compareTo(i2.name);
			});
			if (index == -1) {
				if (BuildConfig.DEBUG) Log.e(TAG, "getFolder: Couldn't find the specified path.");
				return null;
			}
			currFolder = (AlarmGroup) currFolder.getItem(index);
			if (currFolder == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "getFolder: Binary search lied.");
				return null;
			}
		}
		return currFolder;
	}

	/**
	 * Sets a item in the dataset using its relative index. The item may move within the list
	 * to maintain sorted order.
	 * @param relIndex the old relative index to set with
	 * @param item the new item to set relIndex to
	 */
	private synchronized void setItem(final int relIndex, @Nullable final Item item) {
		if (relIndex < 0 || relIndex >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set item. Index is out of bounds.");
			return;
		} else if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set item. Item is null.");
			return;
		}

		item.setParent(this);
		items.remove(relIndex);
		items.add(AlarmGroup.insertIndex(items, item), item);
	}

	/**
	 * Adds a item to the current folder.
	 * @param item the item to add to the folder
	 */
	synchronized void addItem(@Nullable final Item item) {
		if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't set item. Item is null.");
			return;
		}

		item.setParent(this);
		items.add(AlarmGroup.insertIndex(items, item), item);
	}

	/**
	 * Deletes a item at the specified index.
	 * @param index the index of the item to delete
	 * @return the item deleted, or null
	 */
	@Nullable
	public synchronized Item deleteItem(final int index) {
		if (index < 0 || index >= items.size()) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't delete item. Index is out of bounds.");
			return null;
		}

		return items.remove(index);
	}

	/* **************************  Manipulating Contents (by path)  *************************** */

	/**
	 * Sets item as the new item in the dataset at the absolute index. If the index doesn't exist,
	 * doesn't do anything.
	 * @param info the info of the new item (assumes path represents the old path, and the item's id
	 *             remained unchanged)
	 * @param item the new item to set it to
	 * @return the item that was deleted, or null if there was none
	 */
	@Nullable
	public synchronized Item setItem(@Nullable final ItemInfo info, final Item item) {
		if (info == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "setItem: Item was null.");
			return null;
		}



		if (i.parent == null) {
			setitem(i.relIndex, item);
		}
		else {
			i.parent.setitem(i.relIndex, item);
			refreshLookups();
		}
		return i.item;
	}

	/**
	 * Deletes the item at the specified absolute index. Always assumes it's for the visible
	 * items only.
	 * @param absIndex the absolute index of the item to delete
	 * @return the item that was deleted or null if there was an error
	 */
	@Nullable
	public synchronized Item deleteItemAbs(final int absIndex) {
		ItemInfo i = getitemInfo(absIndex, true);
		if (i == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Item at absolute index " + absIndex + " was not found.");
			return null;
		}

		Item l;
		if (i.parent == null) {
			l = deleteItem(i.relIndex);
		}
		else {
			l = i.parent.deleteitem(i.relIndex);
			refreshLookups();
		}
		return l;
	}

	/**
	 * Adds a item to this folder at the given path.
	 * @param item the info given about the item to add, should include the item and path
	 * @param path the path to add the item to
	 */
	public synchronized void addItem(@Nullable Item item, @Nullable String path) {
		if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "addItem: The item specified was null.");
			return;
		}
		if (path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "addItem: The path specified was null.");
			return;
		}

		AlarmGroup currFolder = getFolder(path);
		if (currFolder == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "addItem: Couldn't find the specified path.");
			return;
		}
		currFolder.additem(item);
	}

	/**
	 * Moves the item specified by index to the new path. Assumes that the id of the item
	 * remained the same.
	 * @param item the new item to replace with (will just use the old one if it's null)
	 * @param path the path to move the item to
	 * @param oldPath the old path the item was originally in
	 * @return the new absolute index of the item (visible index). Will return -1 if there was
	 * an error or if it's not visible
	 */
	public synchronized void moveItem(@Nullable Item item, @Nullable String path, @Nullable String oldPath) {
		if (path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "moveItem: The path specified was null.");
			return;
		}
		String[] folders = path.split("/");	// ignore the first one (root folder name)
		AlarmGroup currFolder = getFolder(path);
		if (currFolder == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "moveItemAbs: Couldn't find the specified path.");
			return;
		}

		Item newItem = item;
		if (newItem == null) {
			// replace with what's currently at the abs index
			newItem = deleteitemAbs(oldAbsIndex);
			if (newItem == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "moveItemAbs: Couldn't find the item to move.");
				return;
			}
		}
		else {
			// replace with the new item
			deleteitemAbs(oldAbsIndex);
		}

		currFolder.addItem(newItem);
	}

	/* ***********************************  Other Methods  ************************************** */

	/**
	 * Makes a new string that describes this folder. Use edit or store strings if a full
	 * representation is necessary. Returns simply tne name of the folder.
	 */
	@NotNull @Override @Contract(pure = true)
	public synchronized String toString() { return name; }

	/**
	 * Builds the line of the store string that represents the AlarmGroup itself.
	 * <br/>
	 * Current store identity string format (separated by tabs):
	 * f	[edit string]
	 */
	@NotNull @Contract(pure = true)
	private synchronized String getStoreStringSingle() {
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
	synchronized List<String> toPathList() { return toPathList("", this); }

	/**
	 * Helper function for toPathList().
	 * @param prefix the prefix that should be added to the current parent name and all of its
	 *               children. Shouldn't be null
	 * @param parent the current AlarmGroup we're on, shouldn't be null
	 * @return an array list of strings that stores all of the folders within the parent. All strings
	 * should have the prefix appended to them, have a trailing slash, and should contain the full
	 * path of each folder.
	 */
	@NotNull
	private static synchronized List<String> toPathList(@NotNull String prefix, @NotNull AlarmGroup parent) {
		ArrayList<String> pathList = new ArrayList<>();
		String storeString = parent.getName() + '/';
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
	 * Returns the correct index to insert the given item into the list at.
	 * @param items the list to insert into
	 * @param l the item to insert
	 * @return which index to insert the item into, or -1 if there was an error
	 */
	@Contract(pure = true)
	private static synchronized int insertIndex(@NotNull final List<Item> items, @NotNull final Item l) {
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
