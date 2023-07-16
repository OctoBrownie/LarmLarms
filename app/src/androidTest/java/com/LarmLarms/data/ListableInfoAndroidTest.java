package com.larmlarms.data;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
		src.item = new Alarm(null, "Hello");
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

		assert dest.item != null;
		assertEquals("Hello", dest.item.getListableName());
		assertTrue(dest.item instanceof Alarm);

		assert dest.parent != null;
		assertEquals("Goodbye", dest.parent.getListableName());

		assertEquals(src.path, dest.path);
	}

	/**
	 * Tests the getListableInfo() method to ensure correctness of all ListableInfo fields.
	 */
	@Test
	public void getListableInfoTest() {
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

		ListableInfo info = folder.getListableInfo(-1, false);
		assertNull(info);

		info = folder.getListableInfo(0, false);
		assertEquals(0, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[0], info.item);
		assertNull(info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(1, false);
		assertEquals(1, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[1], info.item);
		assertNull(info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(2, false);
		assertEquals(2, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(innerFolder, info.item);
		assertNull(info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(3, false);
		assertEquals(3, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(alarms[2], info.item);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(4, false);
		assertEquals(4, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(alarms[3], info.item);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(5, false);
		assertEquals(5, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(1, info.numIndents);
		assertEquals(2, info.absParentIndex);
		assertEquals(doubleInner, info.item);
		assertEquals(innerFolder, info.parent);
		assertEquals("test/inner/", info.path);

		info = folder.getListableInfo(6, false);
		assertEquals(6, info.absIndex);
		assertEquals(0, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[4], info.item);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(7, false);
		assertEquals(7, info.absIndex);
		assertEquals(1, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[5], info.item);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(8, false);
		assertEquals(8, info.absIndex);
		assertEquals(2, info.relIndex);
		assertEquals(2, info.numIndents);
		assertEquals(5, info.absParentIndex);
		assertEquals(alarms[6], info.item);
		assertEquals(doubleInner, info.parent);
		assertEquals("test/inner/double double/", info.path);

		info = folder.getListableInfo(9, false);
		assertEquals(9, info.absIndex);
		assertEquals(3, info.relIndex);
		assertEquals(0, info.numIndents);
		assertEquals(-1, info.absParentIndex);
		assertEquals(alarms[7], info.item);
		assertNull(info.parent);
		assertEquals("test/", info.path);

		info = folder.getListableInfo(10, false);
		assertNull(info);
	}
}
