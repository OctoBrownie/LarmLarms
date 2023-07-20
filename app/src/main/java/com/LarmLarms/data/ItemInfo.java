package com.larmlarms.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A struct containing information about a listable within a nested list of Listables
 */
public class ItemInfo implements Parcelable {
	/**
	 * Represents a Listable, and implies the other fields are describing this listable. Not
	 * guaranteed to be a handle to the original Listable (if, for example, it was recreated from a
	 * Parcel).
	 */
	@Nullable
	public Item item;

	/**
	 * Represents the path to this listable but doesn't include the listable itself. It is in the
	 * same form as the strings returned from AlarmGroup.toPathList().
	 */
	@Nullable
	public String path;

	/**
	 * Initializes dummy data in the struct variables, all invalid data in case it isn't filled out.
	 */
	public ItemInfo() {
		item = null;
		path = null;
	}

	/**
	 * Creates a new ListableInfo from a parcel.
	 * @param in the parcel to initialize from, cannot be null
	 */
	private ItemInfo(@NotNull Parcel in) {
		String item = in.readString();	// item
		String isAlarm = in.readString();

		if (item == null) this.item = null;
		else {
			if (Boolean.parseBoolean(isAlarm)) this.item = Alarm.fromEditString(null, item);
			else this.item = AlarmGroup.fromEditString(item);
		}

		path = in.readString();
	}

	// *******************************  Parcelable Things  **********************************

	/**
	 * Creator that creates parcels of ItemInfo objects.
	 */
	@SuppressWarnings("WeakerAccess")
	public static final Parcelable.Creator<ItemInfo> CREATOR =
		new Parcelable.Creator<ItemInfo>() {
			@NotNull @Contract(pure = true)
			public ItemInfo createFromParcel(@NotNull Parcel in) {
				return new ItemInfo(in);
			}
			@NotNull @Contract(pure = true)
			public ItemInfo[] newArray(int size) {
				return new ItemInfo[size];
			}
		};

	/**
	 * Describes the contents of the object.
	 * @return always returns 0 (no file descriptors in the object)
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Write the object to the given parcel
	 * @param dest the parcel to write to, shouldn't be null
	 * @param flags any flags to use
	 */
	@Override
	public void writeToParcel(@NotNull Parcel dest, int flags) {
		// listable then isAlarm
		if (item == null) {
			dest.writeString(null);
			dest.writeString(Boolean.toString(false));
		}
		else {
			dest.writeString(item.toEditString());
			dest.writeString(Boolean.toString(item instanceof Alarm));
		}

		dest.writeString(path);
	}
}
