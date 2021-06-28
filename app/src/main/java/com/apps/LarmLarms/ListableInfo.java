package com.apps.LarmLarms;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A struct containing information about a listable within a nested list of Listables
 */

class ListableInfo implements Parcelable {
	int absIndex, relIndex, numIndents;
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
	AlarmGroup parentListable;

	ListableInfo() {
		relIndex = 0;
		absIndex = 0;
		numIndents = 0;
		listable = null;
		parentListable = null;
	}

	private ListableInfo(Parcel in) {
		absIndex = in.readInt();
		relIndex = in.readInt();
		numIndents = in.readInt();
	}

	/* *******************************  Parcelable Things  ********************************** */

	public static final Parcelable.Creator<ListableInfo> CREATOR =
		new Parcelable.Creator<ListableInfo>() {
			public ListableInfo createFromParcel(Parcel in) {
				return new ListableInfo(in);
			}
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

		if (listable == null)
			dest.writeString(null);
		else
			dest.writeString(listable.toEditString());

		if (parentListable == null)
			dest.writeString(null);
		else
			dest.writeString(parentListable.toEditString());
	}
}
