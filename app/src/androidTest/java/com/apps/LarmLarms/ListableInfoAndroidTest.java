package com.apps.LarmLarms;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test for flattening and unflattening ListableInfo objects into parcels. Must execute
 * on an Android device to make use of the Parcel class.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ListableInfoAndroidTest {
	@Test
	public void parcelTest() {
		ListableInfo src = new ListableInfo();
		src.absIndex = -1;
		src.relIndex = -2;
		src.numIndents = 1;
		src.absParentIndex = 2;
		// can use InstrumentationRegistry.getTargetContext() if desired
		src.listable = new Alarm(null, "Hello");
		src.parent = new AlarmGroup("Goodbye");
		src.path = "computer/CPU/pin";

		Parcel p = Parcel.obtain();
		src.writeToParcel(p, 0);
		p.setDataPosition(0);		// start reading from the beginning of parcel

		ListableInfo dest = ListableInfo.CREATOR.createFromParcel(p);
		assertEquals(src.absIndex, dest.absIndex);
		assertEquals(src.relIndex, dest.relIndex);
		assertEquals(src.numIndents, dest.numIndents);
		assertEquals(src.absParentIndex, dest.absParentIndex);

		assert dest.listable != null;
		assertEquals("Hello", dest.listable.getListableName());
		assertEquals(true, dest.listable.isAlarm());

		assert dest.parent != null;
		assertEquals("Goodbye", dest.parent.getListableName());
		assertEquals(false, dest.parent.isAlarm());

		assertEquals(src.path, dest.path);
	}
}
