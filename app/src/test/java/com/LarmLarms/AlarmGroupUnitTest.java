package com.LarmLarms;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the AlarmGroup class. Requires DEBUG flag to be false when run.
 */
public class AlarmGroupUnitTest {
	/* **********************************  Searching Tests  ********************************* */
	@Test
	public void findOuterIndexTest() throws Exception {
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
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		AlarmGroup anotherFolder = new AlarmGroup("another");
		anotherFolder.addListable(new Alarm(null, "alarm 5"));
		folder.addListable(anotherFolder);

		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getLookup(), -1, folder.size() - 1));
		assertEquals(0, AlarmGroup.findOuterListableIndex(folder.getLookup(), 0, folder.size() - 1));
		assertEquals(1, AlarmGroup.findOuterListableIndex(folder.getLookup(), 1, folder.size() - 1));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getLookup(), 2, folder.size() - 1));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getLookup(), 3, folder.size() - 1));
		assertEquals(3, AlarmGroup.findOuterListableIndex(folder.getLookup(), 4, folder.size() - 1));
		assertEquals(4, AlarmGroup.findOuterListableIndex(folder.getLookup(), 5, folder.size() - 1));
		assertEquals(4, AlarmGroup.findOuterListableIndex(folder.getLookup(), 6, folder.size() - 1));
		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getLookup(), 7, folder.size() - 1));
	}

	@Test
	public void absIndexTest() throws Exception {
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
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		Listable testListable = folder.getListableAbs(-1);
		assert testListable != null;
		assertEquals(null, testListable);

		testListable = folder.getListableAbs(0);
		assert testListable != null;
		assertEquals("alarm 1", testListable.getListableName());

		testListable = folder.getListableAbs(1);
		assert testListable != null;
		assertEquals("alarm 2", testListable.getListableName());

		testListable = folder.getListableAbs(2);
		assert testListable != null;
		assertEquals("inner", testListable.getListableName());

		testListable = folder.getListableAbs(3);
		assert testListable != null;
		assertEquals("alarm 3", testListable.getListableName());

		testListable = folder.getListableAbs(4);
		assert testListable != null;
		assertEquals("alarm 4", testListable.getListableName());

		testListable = folder.getListableAbs(5);
		assert testListable != null;
		assertEquals(null, testListable);
	}

	@Test
	public void findIndentsTest() throws Exception {
		/*
		 * FOLDER STRUCTURE:
		 * test
		 * 	alarm 1
		 * 	alarm 2
		 * 	inner
		 * 		alarm 3
		 * 		alarm 4
		 * 		double double
		 * 			alarm 5
		 *	 		alarm 6
		 * 			alarm 7
		 * 	alarm 8
		 */
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));
		innerFolder.addListable(new Alarm(null, "alarm 4"));

		AlarmGroup doubleInner = new AlarmGroup("double double");
		doubleInner.addListable(new Alarm(null, "alarm 5"));
		doubleInner.addListable(new Alarm(null, "alarm 6"));
		doubleInner.addListable(new Alarm(null, "alarm 7"));

		innerFolder.addListable(doubleInner);
		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 8"));

		assertEquals(-1, folder.getNumIndents(-1));
		assertEquals(0, folder.getNumIndents(0));
		assertEquals(0, folder.getNumIndents(1));
		assertEquals(0, folder.getNumIndents(2));
		assertEquals(1, folder.getNumIndents(3));
		assertEquals(1, folder.getNumIndents(4));
		assertEquals(1, folder.getNumIndents(5));
		assertEquals(2, folder.getNumIndents(6));
		assertEquals(2, folder.getNumIndents(7));
		assertEquals(2, folder.getNumIndents(8));
		assertEquals(0, folder.getNumIndents(9));
		assertEquals(-1, folder.getNumIndents(10));
	}

	@Test
	public void getRelIndexTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));		// index 0
		folder.addListable(new Alarm(null, "alarm 2"));		// index 1

		AlarmGroup innerFolder = new AlarmGroup("inner");	// index 2
		innerFolder.addListable(new Alarm(null, "alarm 3"));		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 4"));		// index 4

		AlarmGroup doubleInner = new AlarmGroup("double double");	// index 5
		doubleInner.addListable(new Alarm(null, "alarm 5"));				// index 6
		doubleInner.addListable(new Alarm(null, "alarm 6"));				// index 7
		doubleInner.addListable(new Alarm(null, "alarm 7"));				// index 8

		innerFolder.addListable(doubleInner);
		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 8"));		// index 9

		assertEquals(-1, folder.getListableIndexAtAbsIndex(-1));
		assertEquals(0, folder.getListableIndexAtAbsIndex(0));
		assertEquals(1, folder.getListableIndexAtAbsIndex(1));
		assertEquals(2, folder.getListableIndexAtAbsIndex(2));
		assertEquals(0, folder.getListableIndexAtAbsIndex(3));
		assertEquals(1, folder.getListableIndexAtAbsIndex(4));
		assertEquals(2, folder.getListableIndexAtAbsIndex(5));
		assertEquals(0, folder.getListableIndexAtAbsIndex(6));
		assertEquals(1, folder.getListableIndexAtAbsIndex(7));
		assertEquals(2, folder.getListableIndexAtAbsIndex(8));
		assertEquals(3, folder.getListableIndexAtAbsIndex(9));
		assertEquals(-1, folder.getListableIndexAtAbsIndex(10));
	}

	/* ********************************  Basic Function Tests  ***************************** */

	/**
	 * Tests the add function in AlarmGroups. Ensures that lookup lists and total items are correct,
	 * as well as the resulting list of Listables.
	 */
	@Test
	public void addTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		ArrayList<Integer> lookupAnswer = new ArrayList<>();
		assertEquals(true, lookupAnswer.equals(folder.getLookup()));

		folder.addListable(new Alarm(null, "alarm 1"));
		assertEquals(2, folder.size());
		lookupAnswer.add(0);
		assertEquals(true, lookupAnswer.equals(folder.getLookup()));

		folder.addListable(new Alarm(null, "alarm 2"));
		assertEquals(3, folder.size());
		lookupAnswer.add(1);
		assertEquals(true, lookupAnswer.equals(folder.getLookup()));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));
		folder.addListable(innerFolder);
		assertEquals(5, folder.size());
		lookupAnswer.add(2);
		assertEquals(true, lookupAnswer.equals(folder.getLookup()));

		folder.addListable(new Alarm(null, "alarm 4"));
		assertEquals(6, folder.size());
		lookupAnswer.add(4);
		assertEquals(true, lookupAnswer.equals(folder.getLookup()));

		Listable testListable = folder.getListable(0);
		assert testListable != null;
		assertEquals("alarm 1", testListable.getListableName());

		testListable = folder.getListable(1);
		assert testListable != null;
		assertEquals("alarm 2", testListable.getListableName());

		testListable = folder.getListable(2);
		assert testListable != null;
		assertEquals("inner", testListable.getListableName());

		testListable = folder.getListable(3);
		assert testListable != null;
		assertEquals("alarm 4", testListable.getListableName());
	}

	/**
	 * Tests whether the lookup tables are correct.
	 */
	@Test
	public void lookupTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		ArrayList<Integer> answer = new ArrayList<>();
		answer.add(0);
		answer.add(1);
		answer.add(2);
		answer.add(4);
		ArrayList<Integer> tester = folder.getLookup();

		assertEquals(true, answer.equals(tester));
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for store strings, and stores Alarms and AlarmGroups.
	 */
	@Test
	public void storeStringTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addListable(new Alarm(null, "alarm 1"));		// index 1
		folder.addListable(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 3"));			// index 4

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));		// index 5
		folder.addListable(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toStoreString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromStoreString(null, s);
		assert tester != null;

		assertEquals("test", tester.getListableName());

		Listable testListable = tester.getListable(0);
		assert testListable != null;
		assertEquals("alarm 1", testListable.getListableName());

		testListable = tester.getListable(1);
		assert testListable != null;
		assertEquals("alarm 2", testListable.getListableName());

		testListable = tester.getListable(2);
		assert testListable != null;
		assertEquals("inner", testListable.getListableName());

		testListable = ((AlarmGroup) testListable).getListable(0);
		assert testListable != null;
		assertEquals("alarm 3", testListable.getListableName());

		testListable = tester.getListable(3);
		assert testListable != null;
		assertEquals("alarm 4", testListable.getListableName());

		testListable = tester.getListable(4);
		assert testListable != null;
		assertEquals("alarm 5", testListable.getListableName());
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, so no recursion and no storing of Alarms.
	 */
	@Test
	public void editStringTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addListable(new Alarm(null, "alarm 1"));		// index 1
		folder.addListable(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 3"));			// index 4

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));		// index 5
		folder.addListable(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toEditString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromEditString(s);
		assert tester != null;

		assertEquals("test", tester.getListableName());
	}

	/**
	 * Tests the conversion into path lists.
	 */
	@Test
	public void pathListTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");		// index 0
		folder.addListable(new Alarm(null, "alarm 1"));		// index 1
		folder.addListable(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup("inner");		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 3"));			// index 4

		AlarmGroup doubleInner = new AlarmGroup("inner 2");		// index 5
		doubleInner.addListable(new Alarm(null, "alarm 4"));			// index 6
		innerFolder.addListable(doubleInner);

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));		// index 7
		folder.addListable(new Alarm(null, "alarm 5"));		// index 8

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
	public void equalsTest() throws Exception {
		AlarmGroup folder1 = new AlarmGroup("Title");
		folder1.setActive(false);

		AlarmGroup folder2 = new AlarmGroup("Title");
		folder2.setActive(false);

		// differences between the alarms that shouldn't throw off the equals method
		folder1.addListable(new AlarmGroup());
		folder2.addListable(new Alarm());
		folder2.addListable(new AlarmGroup());

		assertEquals(folder1, folder2);
	}

	/**
	 * Tests whether clones of an AlarmGroup are considered identical (via the equals method) or not.
	 */
	@Test
	public void cloneIdentityTest() throws Exception {
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
	public void cloneMutableTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("Title");
		folder.setActive(false);
		folder.addListable(new AlarmGroup("Folder"));
		folder.addListable(new Alarm(null, "Alarm"));

		AlarmGroup clone = (AlarmGroup) folder.clone();
		assert clone != null;

		clone.setListableName("Talk");

		Listable cloneL = clone.getListable(0);
		assert cloneL != null;
		cloneL.setListableName("Not a folder");

		cloneL = clone.getListable(1);
		assert cloneL != null;
		cloneL.setActive(false);

		assertEquals(false, folder.getListableName().equals(clone.getListableName()));

		Listable folderL = folder.getListable(0);
		cloneL = clone.getListable(0);
		assert folderL != null;
		assert cloneL != null;
		assertEquals(false, folderL.getListableName().equals(cloneL.getListableName()));

		folderL = folder.getListable(1);
		cloneL = clone.getListable(1);
		assert folderL != null;
		assert cloneL != null;
		assertEquals(false, folderL.isActive() == cloneL.isActive());
	}
}