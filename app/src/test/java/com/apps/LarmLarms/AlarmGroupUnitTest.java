package com.apps.LarmLarms;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the AlarmGroup class.
 */
public class AlarmGroupUnitTest {
	/* **********************************  Searching Tests  ********************************* */
	@Test
	public void findOuterIndexTest() throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getLookup(), -1, 5));
		assertEquals(0, AlarmGroup.findOuterListableIndex(folder.getLookup(), 0, 5));
		assertEquals(1, AlarmGroup.findOuterListableIndex(folder.getLookup(), 1, 5));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getLookup(), 2, 5));
		assertEquals(2, AlarmGroup.findOuterListableIndex(folder.getLookup(), 3, 5));
		assertEquals(3, AlarmGroup.findOuterListableIndex(folder.getLookup(), 4, 5));
		assertEquals(-1, AlarmGroup.findOuterListableIndex(folder.getLookup(), 5, 5));
	}

	@Test
	public void absIndexTest () throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));
		folder.addListable(new Alarm(null, "alarm 2"));

		AlarmGroup innerFolder = new AlarmGroup("inner");
		innerFolder.addListable(new Alarm(null, "alarm 3"));

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));

		Listable testListable = folder.getListableAbs(1);
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
		// within folder, the lookup should be [0, 1, 2, 9] and length 11 (includes itself)
		// within innerFolder, the lookup should be [0, 1, 2] and length 7 (includes itself)
		// within doubleInner, the lookup should be [0, 1, 2] and length 4 (includes itself)
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
	public void getRelIndexTest () throws Exception {
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

	@Test
	public void getParentTest () throws Exception {
		AlarmGroup folder = new AlarmGroup("test");
		folder.addListable(new Alarm(null, "alarm 1"));	// index 0
		folder.addListable(new Alarm(null, "alarm 2"));	// index 1

		AlarmGroup innerFolder = new AlarmGroup("inner");	// index 2
		innerFolder.addListable(new Alarm(null, "alarm 3"));		// index 3

		folder.addListable(innerFolder);
		folder.addListable(new Alarm(null, "alarm 4"));	// index 4

		assertEquals(null, folder.getParentListableAtAbsIndex(-1));
		assertEquals(null, folder.getParentListableAtAbsIndex(0));
		assertEquals(null, folder.getParentListableAtAbsIndex(1));
		assertEquals(null, folder.getParentListableAtAbsIndex(2));
		assertEquals("inner", folder.getParentListableAtAbsIndex(3).getListableName());
		assertEquals(null, folder.getParentListableAtAbsIndex(4));
		assertEquals(null, folder.getParentListableAtAbsIndex(5));
	}

	/* ********************************  Basic Function Tests  ***************************** */

	/**
	 * Tests the add function in AlarmGroups. Ensures that lookup lists and total items are correct,
	 * as well as the resulting list of Listables.
	 */
	@Test
	public void alarmGroupAddTest() throws Exception {
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

	// tests whether the lookup tables are correct
	@Test
	public void alarmGroupLookupTest() throws Exception {
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
	 * Tests specifically for store strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void storeStringAlarmTest() throws Exception {
		System.out.println("Testing strings:");

		Alarm initAlarm = new Alarm(null);
		initAlarm.turnOff();

		String storeString = initAlarm.toStoreString();
		System.out.println(storeString);

		Alarm testAlarm = Alarm.fromStoreString(null, storeString);
		assert testAlarm != null;
		assertEquals(initAlarm.getListableName(), testAlarm.getListableName());


		initAlarm = new Alarm(null, "asdf");
		initAlarm.setRepeatType(Alarm.REPEAT_DATE_MONTHLY);

		storeString = initAlarm.toStoreString();
		System.out.println(storeString);

		testAlarm = Alarm.fromStoreString(null, storeString);
		assert testAlarm != null;
		assertEquals(initAlarm.getListableName(), testAlarm.getListableName());


		initAlarm = new Alarm(null, " ");
		initAlarm.turnOff();

		storeString = initAlarm.toStoreString();
		System.out.println(storeString);

		testAlarm = Alarm.fromStoreString(null, storeString);
		assert testAlarm != null;
		assertEquals(initAlarm.getListableName(), testAlarm.getListableName());


		initAlarm = new Alarm(null, "alarm 4");
		initAlarm.setRepeatType(Alarm.REPEAT_DATE_YEARLY);

		storeString = initAlarm.toStoreString();
		System.out.println(storeString);

		testAlarm = Alarm.fromStoreString(null, storeString);
		assert testAlarm != null;
		assertEquals(initAlarm.getListableName(), testAlarm.getListableName());
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
	 * Tests specifically for edit strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void editStringAlarmTest() throws Exception {
		Alarm a1 = new Alarm(null), a2 = new Alarm(null, "asdf"), a3 = new Alarm(null, " "),
				a4 = new Alarm(null, "alarm 4");
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
		assert test1 != null;
		assertEquals("dum dum", test1.getListableName());

		assert test2 != null;
		assertEquals("asdf", test2.getListableName());

		assert test3 != null;
		assertEquals(" ", test3.getListableName());

		assert test4 != null;
		assertEquals("alarm 4", test4.getListableName());
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
		assertEquals("test", paths.get(0));
		assertEquals("test/inner", paths.get(1));
		assertEquals("test/inner/inner 2", paths.get(2));
	}
}