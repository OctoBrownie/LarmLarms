package com.LarmLarms.data;

import org.junit.Test;

import java.util.Arrays;

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

	/**
	 * Tests whether two separately created Alarms are considered identical (via the equals method)
	 * or not. Requires an instrumented test to test whether ringtone uri or context affect the
	 * equals method.
	 */
	@Test
	public void equalsTest() throws Exception {
		Alarm alarm1 = new Alarm(null, "Title");
		alarm1.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm1.setOffsetDays(10);
		alarm1.setOffsetHours(5);
		alarm1.setOffsetMins(2);
		alarm1.setActive(false);
		alarm1.setSoundOn(false);
		alarm1.setVibrateOn(false);

		Alarm alarm2 = new Alarm(null, "Title");
		alarm2.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm2.setOffsetDays(10);
		alarm2.setOffsetHours(5);
		alarm2.setOffsetMins(2);
		alarm2.setActive(false);
		alarm2.setSoundOn(false);
		alarm2.setVibrateOn(false);

		// differences between the alarms that shouldn't throw off the equals method
		alarm1.snooze();
		boolean[] days = alarm2.getRepeatDays();
		days[0] = false;
		days[2] = false;

		alarm1.updateRingTime();
		alarm2.updateRingTime();
		assertEquals(alarm1, alarm2);
	}

	/**
	 * Tests whether clones of an Alarm are considered identical (via the equals method) or not.
	 */
	@Test
	public void cloneIdentityTest() throws Exception {
		Alarm alarm = new Alarm(null, "Title");
		alarm.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm.setOffsetDays(10);
		alarm.setOffsetHours(5);
		alarm.setOffsetMins(2);
		alarm.setActive(false);
		alarm.setSoundOn(false);
		alarm.setVibrateOn(false);

		Object clone = alarm.clone();
		assertEquals(alarm, clone);
	}

	/**
	 * Tests whether clones of an Alarm change each others' fields or not. Requires an instrumented
	 * test to test for context or ringtone uri.
	 */
	@Test
	public void cloneMutableTest() throws Exception {
		Alarm alarm = new Alarm(null, "Title");
		alarm.setRepeatType(Alarm.REPEAT_DAY_WEEKLY);

		boolean[] days = alarm.getRepeatDays();
		days[0] = false;
		days[2] = false;
		days[3] = false;
		days[5] = false;

		alarm.setActive(false);
		alarm.setSoundOn(false);
		alarm.setVibrateOn(false);

		Alarm clone = (Alarm) alarm.clone();
		assert clone != null;

		clone.setListableName("Hello?");

		days = clone.getRepeatDays();
		days[0] = true;
		days[2] = true;
		days[3] = true;
		days[5] = true;

		clone.setAlarmTimeMillis(alarm.getAlarmTimeMillis() + 1);
		clone.setActive(true);
		clone.setSoundOn(true);

		assertEquals(false, alarm.getListableName().equals(clone.getListableName()));
		assertEquals(false, alarm.getAlarmTimeCalendar().equals(clone.getAlarmTimeCalendar()));
		assertEquals(false, Arrays.equals(alarm.getRepeatDays(), clone.getRepeatDays()));
		assertEquals(false, alarm.isActive());
		assertEquals(false, alarm.isSoundOn());
	}
}
