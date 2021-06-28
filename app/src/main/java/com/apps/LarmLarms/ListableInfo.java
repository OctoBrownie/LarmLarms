package com.apps.LarmLarms;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A struct containing information about a listable within a nested list of Listables
 */

class ListableInfo implements Parcelable {
	int absIndex, relIndex, numIndents, absParentIndex;
	/**
	 * Represents a Listable, and implies the other fields are describing this listable. Not
	 * guaranteed to be a handle to the original Listable (if, for example, it was recreated from a
	 * Parcel).
	 */
	Listable listable;
	/**
	 * Represents the parent folder to the field listable. Not guaranteed to be a handle to the
	 * original AlarmGroup (if, for example, it was recreated from a Parcel).
	 */
	AlarmGroup parent;

	ListableInfo() {
		relIndex = 0;
		absIndex = 0;
		numIndents = 0;
		absParentIndex = 0;
		listable = null;
		parent = null;
	}

	private ListableInfo(Parcel in) {
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
	}

	/* *******************************  Parcelable Things  ********************************** */

	public static final Parcelable.Creator<ListableInfo> CREATOR =
		new Parcelable.Creator<ListableInfo>() {
			@NotNull
			public ListableInfo createFromParcel(Parcel in) {
				return new ListableInfo(in);
			}
			@NotNull @Contract(pure = true)
			public ListableInfo[] newArray(int size) {
				return new ListableInfo[size];
			}
		};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(absIndex);
		dest.writeInt(relIndex);
		dest.writeInt(numIndents);

		if (listable == null) {
			dest.writeString(null);						// listable
			dest.writeString(Boolean.toString(false));	// isAlarm
		}
		else {
			dest.writeString(listable.toEditString());
			dest.writeString(Boolean.toString(listable.isAlarm()));
		}

		if (parent == null)
			dest.writeString(null);
		else
			dest.writeString(parent.toEditString());
	}
}
