package com.LarmLarms;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A struct containing information about a listable within a nested list of Listables
 */
class ListableInfo implements Parcelable {
	/**
	 * The absolute index of a Listable.
	 */
	int absIndex;

	/**
	 * The relative index of a Listable.
	 */
	int relIndex;

	/**
	 * The number of indents for a Listable.
	 */
	int numIndents;

	/**
	 * The absolute index of a Listable's parent. Should be -1 if there is no parent.
	 */
	int absParentIndex;

	/**
	 * Represents a Listable, and implies the other fields are describing this listable. Not
	 * guaranteed to be a handle to the original Listable (if, for example, it was recreated from a
	 * Parcel).
	 */
	@Nullable
	Listable listable;

	/**
	 * Represents the parent folder to the field listable. Not guaranteed to be a handle to the
	 * original AlarmGroup (if, for example, it was recreated from a Parcel).
	 */
	@Nullable
	AlarmGroup parent;

	/**
	 * Represents the path to this listable but doesn't include the listable itself. It is in the
	 * same form as the strings returned from AlarmGroup.toPathList().
	 */
	@Nullable
	String path;

	/**
	 * Initializes dummy data in the struct variables, all invalid data in case it isn't filled out.
	 */
	ListableInfo() {
		relIndex = -1;
		absIndex = -1;
		numIndents = -1;
		absParentIndex = -1;
		listable = null;
		parent = null;
		path = null;
	}

	/**
	 * Creates a new ListableInfo from a parcel.
	 * @param in the parcel to initialize from, cannot be null
	 */
	private ListableInfo(@NotNull Parcel in) {
		absIndex = in.readInt();
		relIndex = in.readInt();
		numIndents = in.readInt();
		absParentIndex = in.readInt();

		String l = in.readString();	// listable
		String isAlarm = in.readString();
		if (l == null) listable = null;
		else {
			if (Boolean.parseBoolean(isAlarm)) listable = Alarm.fromEditString(null, l);
			else listable = AlarmGroup.fromEditString(l);
		}

		l = in.readString();	// parent
		if (l == null) parent = null;
		else {
			parent = AlarmGroup.fromEditString(l);
		}

		path = in.readString();
	}

	/* *******************************  Parcelable Things  ********************************** */

	/**
	 * Creator that creates parcels of ListableInfo objects.
	 */
	public static final Parcelable.Creator<ListableInfo> CREATOR =
		new Parcelable.Creator<ListableInfo>() {
			@NotNull @Contract(pure = true)
			public ListableInfo createFromParcel(@NotNull Parcel in) {
				return new ListableInfo(in);
			}
			@NotNull @Contract(pure = true)
			public ListableInfo[] newArray(int size) {
				return new ListableInfo[size];
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
		dest.writeInt(absIndex);
		dest.writeInt(relIndex);
		dest.writeInt(numIndents);
		dest.writeInt(absParentIndex);

		// listable then isAlarm
		if (listable == null) {
			dest.writeString(null);
			dest.writeString(Boolean.toString(false));
		}
		else {
			dest.writeString(listable.toEditString());
			dest.writeString(Boolean.toString(listable.isAlarm()));
		}

		// parent
		if (parent == null)
			dest.writeString(null);
		else
			dest.writeString(parent.toEditString());

		dest.writeString(path);
	}
}
