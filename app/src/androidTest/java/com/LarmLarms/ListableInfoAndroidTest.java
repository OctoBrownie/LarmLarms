package com.LarmLarms;

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

	/**
	 * Tests the getListableInfo() method to ensure correctness of all ListableInfo fields.
	 */
	@Test
	public void getListableInfoTest() throws Exception {
		/*
		 * FOLDER STRUCTURE:
		 * test
		 * 	alarm 0
		 * 	alarm 1
		 * 	inner
		 * 		alarm 2
		 * 		alarm 3
		 * 		double double
		 * 			alarm 4
		 *	 		alarm 5
		 * 			alarm 6
		 * 	alarm 7
		 */

		AlarmGroup folder = new AlarmGroup("test");
		Alarm[] alarms = new Alarm[8];
		for (int i = 0; i < alarms.length; i++) alarms[i] = new Alarm(null, "alarm " + i);

		folder.addListable(alarms[0]);
		folder.addListable(alarms[1]);

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(alarms[2]);
		innerFolder.addListable(alarms[3]);

		AlarmGroup doubleInner = new AlarmGroup("double double");
		doubleInner.addListable(alarms[4]);
		doubleInner.addListable(alarms[5]);
		doubleInner.addListable(alarms[6]);

		innerFolder.addListable(doubleInner);
		folder.addListable(innerFolder);
		folder.addListable(alarms[7]);

		ListableInfo info = folder.getListableInfo(-1);
		assertEquals(null, info);

		info = folder.getListableInfo(0);
		assertEquals(0, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[0], info.listable);
		assertEquals(null, info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(1);
		assertEquals(1, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[1], info.listable);
		assertEquals(null, info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(2);
		assertEquals(2, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(innerFolder, info.listable);
		assertEquals(null, info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(3);
		assertEquals(3, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(alarms[2], info.listable);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(4);
		assertEquals(4, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(alarms[3], info.listable);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(5);
		assertEquals(5, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(doubleInner, info.listable);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(6);
		assertEquals(6, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[4], info.listable);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(7);
		assertEquals(7, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[5], info.listable);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(8);
		assertEquals(8, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[6], info.listable);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(9);
		assertEquals(9, info.absIndex);
		assertEquals(3, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[7], info.listable);
		assertEquals(null, info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(10);
		assertEquals(null, info);
	}
}
