package com.larmlarms.data;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the AlarmGroup class. Requires DEBUG flag to be false when run.
 */
public class AlarmGroupUnitTest {
	/* **********************************  Searching Tests  ********************************* */
	@Test
	public void findOuterIndexTest() {
		/*
		 * FOLDER STRUCTURE:
		 * test
		 * 	alarm 1
		 * 	alarm 2
		 * 	inner
		 * 		alarm 3
		 * 	alarm 4
		 * 	another
		 * 		alarm 5
		 */
		AlarmGroup folder = new AlarmGroup("test");
		folder.addItem(new Alarm(null, "alarm 1"));
		folder.addItem(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addItem(new Alarm(null, "alarm 3"));

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));

		AlarmGroup anotherFolder = new AlarmGroup("another");
		anotherFolder.addItem(new Alarm(null, "alarm 5"));
		folder.addItem(anotherFolder);

		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), -1, folder.visibleSize() - 1));
		assertEquals(0, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 0, folder.visibleSize() - 1));
		assertEquals(1, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 1, folder.visibleSize() - 1));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 2, folder.visibleSize() - 1));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 3, folder.visibleSize() - 1));
		assertEquals(3, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 4, folder.visibleSize() - 1));
		assertEquals(4, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 5, folder.visibleSize() - 1));
		assertEquals(4, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 6, folder.visibleSize() - 1));
		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getVisibleLookup(), 7, folder.visibleSize() - 1));
	}

	@Test
	public void absIndexTest() {
		/*
		 * FOLDER STRUCTURE:
		 * test
		 * 	alarm 1
		 * 	alarm 2
		 * 	inner
		 * 		alarm 3
		 * 	alarm 4
		 */
		AlarmGroup folder = new AlarmGroup("test");
		folder.addItem(new Alarm(null, "alarm 1"));
		folder.addItem(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addItem(new Alarm(null, "alarm 3"));

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));

		Item testItem = folder.getListableAbs(-1, false);
		assert testItem != null;

		testItem = folder.getListableAbs(0, false);
		assert testItem != null;
		assertEquals("alarm 1", testItem.getName());

		testItem = folder.getListableAbs(1, false);
		assert testItem != null;
		assertEquals("alarm 2", testItem.getName());

		testItem = folder.getListableAbs(2, false);
		assert testItem != null;
		assertEquals("inner", testItem.getName());

		testItem = folder.getListableAbs(3, false);
		assert testItem != null;
		assertEquals("alarm 3", testItem.getName());

		testItem = folder.getListableAbs(4, false);
		assert testItem != null;
		assertEquals("alarm 4", testItem.getName());

		testItem = folder.getListableAbs(5, false);
		assert testItem != null;
	}

	/* ********************************  Basic Function Tests  ***************************** */

	/**
	 * Tests the add function in AlarmGroups. Ensures that lookup lists and total items are correct,
	 * as well as the resulting list of Listables.
	 */
	@Test
	public void addTest() {
		AlarmGroup folder = new AlarmGroup("test");
		ArrayList<Integer> lookupAnswer = new ArrayList<>();
		assertEquals(lookupAnswer, folder.getVisibleLookup());

		folder.addItem(new Alarm(null, "alarm 1"));
		assertEquals(2, folder.visibleSize());
		lookupAnswer.add(0);
		assertEquals(lookupAnswer, folder.getVisibleLookup());

		folder.addItem(new Alarm(null, "alarm 2"));
		assertEquals(3, folder.visibleSize());
		lookupAnswer.add(1);
		assertEquals(lookupAnswer, folder.getVisibleLookup());

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addItem(new Alarm(null, "alarm 3"));
		folder.addItem(innerFolder);
		assertEquals(5, folder.visibleSize());
		lookupAnswer.add(2);
		assertEquals(lookupAnswer, folder.getVisibleLookup());

		folder.addItem(new Alarm(null, "alarm 4"));
		assertEquals(6, folder.visibleSize());
		lookupAnswer.add(4);
		assertEquals(lookupAnswer, folder.getVisibleLookup());

		Item testItem = folder.getItem(0);
		assert testItem != null;
		assertEquals("alarm 1", testItem.getName());

		testItem = folder.getItem(1);
		assert testItem != null;
		assertEquals("alarm 2", testItem.getName());

		testItem = folder.getItem(2);
		assert testItem != null;
		assertEquals("inner", testItem.getName());

		testItem = folder.getItem(3);
		assert testItem != null;
		assertEquals("alarm 4", testItem.getName());
	}

	/**
	 * Tests whether the lookup tables are correct.
	 */
	@Test
	public void lookupTest() {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addItem(new Alarm(null, "alarm 1"));
		folder.addItem(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addItem(new Alarm(null, "alarm 3"));

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));

		ArrayList<Integer> answer = new ArrayList<>();
		answer.add(0);
		answer.add(1);
		answer.add(2);
		answer.add(4);
		ArrayList<Integer> tester = folder.getVisibleLookup();

		assertEquals(answer, tester);
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for store strings, and stores Alarms and AlarmGroups.
	 */
	@Test
	public void storeStringTest() {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addItem(new Alarm(null, "alarm 1"));		// index 1
		folder.addItem(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addItem(new Alarm(null, "alarm 3"));			// index 4

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));		// index 5
		folder.addItem(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toStoreString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromStoreString(null, s);
		assert tester != null;

		assertEquals("test", tester.getName());

		Item testItem = tester.getItem(0);
		assert testItem != null;
		assertEquals("alarm 1", testItem.getName());

		testItem = tester.getItem(1);
		assert testItem != null;
		assertEquals("alarm 2", testItem.getName());

		testItem = tester.getItem(2);
		assert testItem != null;
		assertEquals("inner", testItem.getName());

		testItem = ((AlarmGroup) testItem).getItem(0);
		assert testItem != null;
		assertEquals("alarm 3", testItem.getName());

		testItem = tester.getItem(3);
		assert testItem != null;
		assertEquals("alarm 4", testItem.getName());

		testItem = tester.getItem(4);
		assert testItem != null;
		assertEquals("alarm 5", testItem.getName());
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, so no recursion and no storing of Alarms.
	 */
	@Test
	public void editStringTest() {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addItem(new Alarm(null, "alarm 1"));		// index 1
		folder.addItem(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addItem(new Alarm(null, "alarm 3"));			// index 4

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));		// index 5
		folder.addItem(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toEditString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromEditString(s);
		assert tester != null;

		assertEquals("test", tester.getName());
	}

	/**
	 * Tests the conversion into path lists.
	 */
	@Test
	public void pathListTest() {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addItem(new Alarm(null, "alarm 1"));		// index 1
		folder.addItem(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addItem(new Alarm(null, "alarm 3"));			// index 4

		AlarmGroup doubleInner = new AlarmGroup("inner 2");		// index 5
		doubleInner.addItem(new Alarm(null, "alarm 4"));			// index 6
		innerFolder.addItem(doubleInner);

		folder.addItem(innerFolder);
		folder.addItem(new Alarm(null, "alarm 4"));		// index 7
		folder.addItem(new Alarm(null, "alarm 5"));		// index 8

		ArrayList<String> paths = folder.toPathList();

		assert paths.size() == 3;
		assertEquals("test/", paths.get(0));
		assertEquals("test/inner/", paths.get(1));
		assertEquals("test/inner/inner 2/", paths.get(2));
	}

	/**
	 * Tests whether two separately created AlarmGroups are considered identical (via the equals
	 * method) or not.
	 */
	@Test
	public void equalsTest() {
		AlarmGroup folder1 = new AlarmGroup("Title");
		folder1.setActive(false);

		AlarmGroup folder2 = new AlarmGroup("Title");
		folder2.setActive(false);

		// differences between the alarms that shouldn't throw off the equals method
		folder1.addItem(new AlarmGroup());
		folder2.addItem(new Alarm(null));
		folder2.addItem(new AlarmGroup());

		assertEquals(folder1, folder2);
	}

	/**
	 * Tests whether clones of an AlarmGroup are considered identical (via the equals method) or not.
	 */
	@Test
	public void cloneIdentityTest() {
		AlarmGroup folder = new AlarmGroup("Title");
		folder.setActive(false);

		Object clone = folder.clone();
		assertEquals(folder, clone);
	}

	/**
	 * Tests whether clones of an Alarm change each others' fields or not. Requires an instrumented
	 * test to test for context or ringtone uri.
	 */
	@Test
	public void cloneMutableTest() {
		AlarmGroup folder = new AlarmGroup("Title");
		folder.setActive(false);
		folder.addItem(new AlarmGroup("Folder"));
		folder.addItem(new Alarm(null, "Alarm"));

		AlarmGroup clone = (AlarmGroup) folder.clone();

		clone.setName("Talk");

		Item cloneL = clone.getItem(0);
		assert cloneL != null;
		cloneL.setName("Not a folder");

		cloneL = clone.getItem(1);
		assert cloneL != null;
		cloneL.setActive(false);

		assertNotEquals(folder.getName(), clone.getName());

		Item folderL = folder.getItem(0);
		cloneL = clone.getItem(0);
		assert folderL != null;
		assert cloneL != null;
		assertNotEquals(folderL.getName(), cloneL.getName());

		folderL = folder.getItem(1);
		cloneL = clone.getItem(1);
		assert folderL != null;
		assert cloneL != null;
		assertNotEquals(folderL.isActive(), cloneL.isActive());
	}

	/**
	 * Tests the capabilities of compareTo
	 */
	@Test
	public void testCompareTo() {
		AlarmGroup a = new AlarmGroup("Test");

		Item b = a.clone();
		assertEquals(0, a.compareTo(b));

		b = new Alarm(null, "AAA");
		assertTrue(a.compareTo(b) < 0);

		b = new AlarmGroup("Zest");
		assertTrue(a.compareTo(b) < 0);

		b = new AlarmGroup("Test");
		assertTrue(a.compareTo(b) < 0);
	}
}