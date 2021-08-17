package com.apps.LarmLarms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for Alarm methods. Requires DEBUG flag to be false to run.
 */
public class AlarmUnitTest {
	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void editStringTest() throws Exception {
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
	 * Tests specifically for store strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void storeStringTest() throws Exception {
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
}
