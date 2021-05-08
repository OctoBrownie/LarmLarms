package com.apps.LarmLarms;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Unit tests for the AlarmGroup class.
 */
public class AlarmGroupUnitTest {
	/* **********************************  Searching Tests  ********************************* */
	@Test
	public void findOuterIndexTest() throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), -1, 5));
		assertEquals(0, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 0, 5));
		assertEquals(1, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 1, 5));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 2, 5));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 3, 5));
		assertEquals(3, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 4, 5));
		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getListablesLookup(), 5, 5));
	}

	@Test
	public void absIndexTest () throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		assertEquals(null, AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), -1));
		assertEquals("alarm 1", AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 0).getListableName());
		assertEquals("alarm 2", AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 1).getListableName());
		assertEquals("inner", AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 2).getListableName());
		assertEquals("alarm 3", AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 3).getListableName());
		assertEquals("alarm 4", AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 4).getListableName());
		assertEquals(null, AlarmGroup.getListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 5));
	}

	@Test
	public void findIndentsTest() throws Exception {
		// within folder, the lookup should be [0, 1, 2, 9] and length 11 (includes itself)
		// within innerFolder, the lookup should be [0, 1, 2] and length 7 (includes itself)
		// within doubleInner, the lookup should be [0, 1, 2] and length 4 (includes itself)
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));
		innerFolder.addListable(new Alarm(null, "alarm 4"));

		AlarmGroup doubleInner = new AlarmGroup(null, "double double");
		doubleInner.addListable(new Alarm(null, "alarm 5"));
		doubleInner.addListable(new Alarm(null, "alarm 6"));
		doubleInner.addListable(new Alarm(null, "alarm 7"));

		innerFolder.addListable(doubleInner);
		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 8"));

		assertEquals(-1, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), -1));
		assertEquals(0, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 0));
		assertEquals(0, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 1));
		assertEquals(0, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 2));
		assertEquals(1, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 3));
		assertEquals(1, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 4));
		assertEquals(1, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 5));
		assertEquals(2, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 6));
		assertEquals(2, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 7));
		assertEquals(2, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 8));
		assertEquals(0, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 9));
		assertEquals(-1, AlarmGroup.getNumIndents(folder.getListablesInside(),
				folder.getListablesLookup(), 10));
	}

	@Test
	public void getRelIndexTest () throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));		// index 0
		folder.addListable(new Alarm(null, "alarm 2"));		// index 1

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");	// index 2
		innerFolder.addListable(new Alarm(null, "alarm 3"));		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 4"));		// index 4

		AlarmGroup doubleInner = new AlarmGroup(null, "double double");	// index 5
		doubleInner.addListable(new Alarm(null, "alarm 5"));				// index 6
		doubleInner.addListable(new Alarm(null, "alarm 6"));				// index 7
		doubleInner.addListable(new Alarm(null, "alarm 7"));				// index 8

		innerFolder.addListable(doubleInner);
		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 8"));		// index 9

		assertEquals(-1, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), -1));
		assertEquals(0, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 0));
		assertEquals(1, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 1));
		assertEquals(2, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 2));
		assertEquals(0, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 3));
		assertEquals(1, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 4));
		assertEquals(2, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 5));
		assertEquals(0, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 6));
		assertEquals(1, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 7));
		assertEquals(2, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 8));
		assertEquals(3, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 9));
		assertEquals(-1, AlarmGroup.getListableIndexAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 10));
	}

	@Test
	public void getParentTest () throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));	// index 0
		folder.addListable(new Alarm(null, "alarm 2"));	// index 1

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");	// index 2
		innerFolder.addListable(new Alarm(null, "alarm 3"));		// index 3

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));	// index 4

		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), -1));
		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 0));
		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 1));
		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 2));
		assertEquals("inner", AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 3).getListableName());
		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 4));
		assertEquals(null, AlarmGroup.getParentListableAtAbsIndex(folder.getListablesInside(),
				folder.getListablesLookup(), 5));
	}

	/* ********************************  Basic Function Tests  ***************************** */

	/**
	 * Tests the add function in AlarmGroups. Ensures that lookup lists and total items are correct,
	 * as well as the resulting list of Listables.
	 */
	@Test
	public void alarmGroupAddTest() throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		ArrayList<Integer> lookupAnswer = new ArrayList<>();
		assertEquals(true, lookupAnswer.equals(folder.getListablesLookup()));

		folder.addListable(new Alarm(null, "alarm 1"));
		assertEquals(2, folder.getNumItems());
		lookupAnswer.add(0);
		assertEquals(true, lookupAnswer.equals(folder.getListablesLookup()));

		folder.addListable(new Alarm(null, "alarm 2"));
		assertEquals(3, folder.getNumItems());
		lookupAnswer.add(1);
		assertEquals(true, lookupAnswer.equals(folder.getListablesLookup()));

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));
		folder.addListable(innerFolder);
		assertEquals(5, folder.getNumItems());
		lookupAnswer.add(2);
		assertEquals(true, lookupAnswer.equals(folder.getListablesLookup()));

		folder.addListable(new Alarm(null, "alarm 4"));
		assertEquals(6, folder.getNumItems());
		lookupAnswer.add(4);
		assertEquals(true, lookupAnswer.equals(folder.getListablesLookup()));

		assertEquals("alarm 1", folder.getListable(0).getListableName());
		assertEquals("alarm 2", folder.getListable(1).getListableName());
		assertEquals("inner", folder.getListable(2).getListableName());
		assertEquals("alarm 4", folder.getListable(3).getListableName());
	}

	// tests whether the lookup tables are correct
	@Test
	public void alarmGroupLookupTest() throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		ArrayList<Integer> answer = new ArrayList<>();
		answer.add(0);
		answer.add(1);
		answer.add(2);
		answer.add(4);
		ArrayList<Integer> tester = folder.getListablesLookup();

		assertEquals(true, answer.equals(tester));
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for store strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void storeStringAlarmTest() throws Exception {
		Alarm a1 = new Alarm(null), a2 = new Alarm(null, "asdf"), a3 = new Alarm(null, " "), a4 = new Alarm(null, "alarm 4");
		a1.turnOff();
		a1.setRepeatType(Alarm.REPEAT_ONCE_REL);
		a2.setRepeatType(Alarm.REPEAT_DATE_MONTHLY);
		a3.turnOff();
		a3.setRepeatType(Alarm.REPEAT_DATE_YEARLY);

		String s1 = a1.toStoreString(), s2 = a2.toStoreString(), s3 = a3.toStoreString(),
			   s4 = a4.toStoreString();
		System.out.println("Testing strings: \n" + s1 + '\n' + s2 + '\n' + s3 + '\n' + s4);

		Alarm test1 = Alarm.fromStoreString(null, s1), test2 = Alarm.fromStoreString(null, s2),
			  test3 = Alarm.fromStoreString(null, s3), test4 = Alarm.fromStoreString(null, s4);
		assertEquals("dum dum", test1.getListableName());
		assertEquals("asdf", test2.getListableName());
		assertEquals(" ", test3.getListableName());
		assertEquals("alarm 4", test4.getListableName());
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for store strings, and stores Alarms and AlarmGroups.
	 */
	@Test
	public void storeStringTest() throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");		// index 0
		folder.addListable(new Alarm(null, "alarm 1"));		// index 1
		folder.addListable(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 3"));			// index 4

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));		// index 5
		folder.addListable(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toStoreString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromStoreString(null, s);
		if (tester == null) { throw new AssertionError(); }

		assertEquals("test", tester.getListableName());

		assertEquals("alarm 1", tester.getListable(0).getListableName());
		assertEquals("alarm 2", tester.getListable(1).getListableName());
		assertEquals("inner", tester.getListable(2).getListableName());
		assertEquals("alarm 3", ((AlarmGroup) tester.getListable(2)).getListable(0).getListableName());
		assertEquals("alarm 4", tester.getListable(3).getListableName());
		assertEquals("alarm 5", tester.getListable(4).getListableName());
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void editStringAlarmTest() throws Exception {
		Alarm a1 = new Alarm(null), a2 = new Alarm(null, "asdf"), a3 = new Alarm(null, " "), a4 = new Alarm(null, "alarm 4");
		a1.turnOff();
		a1.setRepeatType(Alarm.REPEAT_ONCE_REL);
		a2.setRepeatType(Alarm.REPEAT_DATE_MONTHLY);
		a3.turnOff();
		a3.setRepeatType(Alarm.REPEAT_DATE_YEARLY);

		String s1 = a1.toEditString(), s2 = a2.toEditString(), s3 = a3.toEditString(),
				s4 = a4.toEditString();
		System.out.println("Testing strings: \n" + s1 + '\n' + s2 + '\n' + s3 + '\n' + s4);

		Alarm test1 = Alarm.fromEditString(null, s1), test2 = Alarm.fromEditString(null, s2),
				test3 = Alarm.fromEditString(null, s3), test4 = Alarm.fromEditString(null, s4);
		assertEquals("dum dum", test1.getListableName());
		assertEquals("asdf", test2.getListableName());
		assertEquals(" ", test3.getListableName());
		assertEquals("alarm 4", test4.getListableName());
	}

	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, so no recursion and no storing of Alarms.
	 */
	@Test
	public void editStringTest() throws Exception {
		AlarmGroup folder = new AlarmGroup(null, "test");		// index 0
		folder.addListable(new Alarm(null, "alarm 1"));		// index 1
		folder.addListable(new Alarm(null, "alarm 2"));		// index 2

		AlarmGroup innerFolder = new AlarmGroup(null, "inner");		// index 3
		innerFolder.addListable(new Alarm(null, "alarm 3"));			// index 4

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));		// index 5
		folder.addListable(new Alarm(null, "alarm 5"));		// index 6

		String s = folder.toEditString();
		System.out.println("Testing string: \n" + s);

		AlarmGroup tester = AlarmGroup.fromEditString(null, s);
		if (tester == null) { throw new AssertionError(); }

		assertEquals("test", tester.getListableName());


	}
}