package com.larmlarms.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Alarm methods. Requires DEBUG flag to be false to run.
 */
public class AlarmUnitTest {
	/**
	 * Tests both the compressing and decompressing capabilities, and that they can work together.
	 * Tests specifically for edit strings, and stores only Alarms, no recursion.
	 */
	@Test
	public void editStringTest() {
		Alarm a1 = new Alarm(null), a2 = new Alarm(null, "a"), a3 = new Alarm(null, " "),
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
		assertEquals("a", test2.getListableName());

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
	public void storeStringTest() {
		System.out.println("Testing strings:");

		Alarm initAlarm = new Alarm(null);
		initAlarm.turnOff();

		String storeString = initAlarm.toStoreString();
		System.out.println(storeString);

		Alarm testAlarm = Alarm.fromStoreString(null, storeString);
		assert testAlarm != null;
		assertEquals(initAlarm.getListableName(), testAlarm.getListableName());


		initAlarm = new Alarm(null, "a");
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
	public void equalsTest() {
		Alarm alarm1 = new Alarm(null, "Title");
		alarm1.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm1.setOffsetDays(10);
		alarm1.setOffsetHours(5);
		alarm1.setOffsetMins(2);
		alarm1.setActive(false);
		alarm1.setVibrateOn(false);

		Alarm alarm2 = new Alarm(null, "Title");
		alarm2.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm2.setOffsetDays(10);
		alarm2.setOffsetHours(5);
		alarm2.setOffsetMins(2);
		alarm2.setActive(false);
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
	public void cloneIdentityTest() {
		Alarm alarm = new Alarm(null, "Title");
		alarm.setRepeatType(Alarm.REPEAT_OFFSET);
		alarm.setOffsetDays(10);
		alarm.setOffsetHours(5);
		alarm.setOffsetMins(2);
		alarm.setActive(false);
		alarm.setVibrateOn(false);

		Object clone = alarm.clone();
		assertEquals(alarm, clone);
	}

	/**
	 * Tests whether clones of an Alarm change each others' fields or not. Requires an instrumented
	 * test to test for context or ringtone uri.
	 */
	@Test
	public void cloneMutableTest() {
		Alarm alarm = new Alarm(null, "Title");
		alarm.setRepeatType(Alarm.REPEAT_DAY_WEEKLY);

		boolean[] days = alarm.getRepeatDays();
		days[0] = false;
		days[2] = false;
		days[3] = false;
		days[5] = false;

		alarm.setActive(false);
		alarm.setVibrateOn(false);

		Alarm clone = (Alarm) alarm.clone();

		clone.setListableName("Hello?");

		days = clone.getRepeatDays();
		days[0] = true;
		days[2] = true;
		days[3] = true;
		days[5] = true;

		clone.setAlarmTimeMillis(alarm.getAlarmTimeMillis() + 1);
		clone.setActive(true);

		assertNotEquals(alarm.getListableName(), clone.getListableName());
		assertNotEquals(alarm.getAlarmTimeCalendar(), clone.getAlarmTimeCalendar());
		assertFalse(Arrays.equals(alarm.getRepeatDays(), clone.getRepeatDays()));
		assertFalse(alarm.isActive());
	}

	/**
	 * Tests the capabilities of compareTo
	 */
	@Test
	public void testCompareTo() {
		Alarm a = new Alarm(null, "Test");
		a.setRepeatType(Alarm.REPEAT_ONCE_REL);

		Listable b = a.clone();
		assertEquals(0, a.compareTo(b));

		b = new AlarmGroup("ZZZ");
		assertTrue(a.compareTo(b) > 0);

		b = new Alarm(null, "Zest");
		assertTrue(a.compareTo(b) < 0);
	}

	/**
	 * Tests the updateRingTime() method, specifically for the REPEAT_ONCE_ABS repeat type.
	 */
	@Test
	public void testURTOnceAbs() {
		Alarm a = new Alarm(null, "alarm");
		a.setRepeatType(Alarm.REPEAT_ONCE_ABS);
		Calendar aCalendar;
		Calendar currTime = Calendar.getInstance();


		// hasn't rung yet
		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 25);
		aCalendar.set(Calendar.HOUR_OF_DAY, 21);
		aCalendar.set(Calendar.MINUTE, 59);
		currTime.set(Calendar.DATE, 25);
		currTime.set(Calendar.HOUR_OF_DAY, 20);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(currTime.get(Calendar.DATE), aCalendar.get(Calendar.DATE));
		assertEquals(21, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, aCalendar.get(Calendar.MINUTE));


		// update to today
		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 1);
		aCalendar.set(Calendar.HOUR_OF_DAY, 22);
		aCalendar.set(Calendar.MINUTE, 34);
		currTime.set(Calendar.DATE, 3);
		currTime.set(Calendar.HOUR_OF_DAY, 20);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(3, aCalendar.get(Calendar.DATE));
		assertEquals(22, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(34, aCalendar.get(Calendar.MINUTE));
		assertEquals(0, aCalendar.get(Calendar.SECOND));
		assertEquals(0, aCalendar.get(Calendar.MILLISECOND));


		// update to the next day
		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 3);
		aCalendar.set(Calendar.HOUR_OF_DAY, 15);
		aCalendar.set(Calendar.MINUTE, 30);
		currTime.set(Calendar.DATE, 3);
		currTime.set(Calendar.HOUR_OF_DAY, 20);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(4, aCalendar.get(Calendar.DATE));
		assertEquals(15, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(30, aCalendar.get(Calendar.MINUTE));
		assertEquals(0, aCalendar.get(Calendar.SECOND));
		assertEquals(0, aCalendar.get(Calendar.MILLISECOND));


		// update to the next day (which happens to be next month)
		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.MONTH, Calendar.JULY);
		aCalendar.set(Calendar.DATE, 31);
		aCalendar.set(Calendar.HOUR_OF_DAY, 8);
		aCalendar.set(Calendar.MINUTE, 45);
		currTime.set(Calendar.MONTH, Calendar.JULY);
		currTime.set(Calendar.DATE, 31);
		currTime.set(Calendar.HOUR_OF_DAY, 10);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.AUGUST, aCalendar.get(Calendar.MONTH));
		assertEquals(1, aCalendar.get(Calendar.DATE));
		assertEquals(8, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, aCalendar.get(Calendar.MINUTE));
		assertEquals(0, aCalendar.get(Calendar.SECOND));
		assertEquals(0, aCalendar.get(Calendar.MILLISECOND));


		// update to the next day (which happens to be next year)
		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.YEAR, 1999);
		aCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
		aCalendar.set(Calendar.DATE, 31);
		aCalendar.set(Calendar.HOUR_OF_DAY, 10);
		aCalendar.set(Calendar.MINUTE, 59);
		currTime.set(Calendar.YEAR, 1999);
		currTime.set(Calendar.MONTH, Calendar.DECEMBER);
		currTime.set(Calendar.DATE, 31);
		currTime.set(Calendar.HOUR_OF_DAY, 22);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(2000, aCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, aCalendar.get(Calendar.MONTH));
		assertEquals(1, aCalendar.get(Calendar.DATE));
		assertEquals(10, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, aCalendar.get(Calendar.MINUTE));
		assertEquals(0, aCalendar.get(Calendar.SECOND));
		assertEquals(0, aCalendar.get(Calendar.MILLISECOND));
	}

	/**
	 * Tests the updateRingTime() method, specifically for the REPEAT_ONCE_REL repeat type.
	 */
	@Test
	public void testURTOnceRel() {
		Alarm a = new Alarm(null, "alarm");
		a.setRepeatType(Alarm.REPEAT_ONCE_REL);
		Calendar aCalendar;
		Calendar currTime = Calendar.getInstance();


		// hasn't rung yet
		a.setOffsetDays(0);
		a.setOffsetHours(1);
		a.setOffsetMins(0);

		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 25);
		aCalendar.set(Calendar.HOUR_OF_DAY, 21);
		aCalendar.set(Calendar.MINUTE, 59);
		currTime.set(Calendar.DATE, 25);
		currTime.set(Calendar.HOUR_OF_DAY, 20);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(currTime.get(Calendar.DATE), aCalendar.get(Calendar.DATE));
		assertEquals(21, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, aCalendar.get(Calendar.MINUTE));


		// update by only minutes
		a.setOffsetDays(0);
		a.setOffsetHours(0);
		a.setOffsetMins(15);

		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 25);
		aCalendar.set(Calendar.HOUR_OF_DAY, 15);
		currTime.set(Calendar.DATE, 25);
		currTime.set(Calendar.HOUR_OF_DAY, 20);
		currTime.set(Calendar.MINUTE, 32);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(currTime.get(Calendar.DATE), aCalendar.get(Calendar.DATE));
		assertEquals(20, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(47, aCalendar.get(Calendar.MINUTE));
		assertEquals(currTime.get(Calendar.SECOND), aCalendar.get(Calendar.SECOND));
		assertEquals(currTime.get(Calendar.MILLISECOND), aCalendar.get(Calendar.MILLISECOND));


		// update by only minutes (to the next hour)
		a.setOffsetDays(0);
		a.setOffsetHours(0);
		a.setOffsetMins(50);

		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 12);
		aCalendar.set(Calendar.HOUR_OF_DAY, 8);
		currTime.set(Calendar.DATE, 12);
		currTime.set(Calendar.HOUR_OF_DAY, 10);
		currTime.set(Calendar.MINUTE, 45);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(currTime.get(Calendar.DATE), aCalendar.get(Calendar.DATE));
		assertEquals(11, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(35, aCalendar.get(Calendar.MINUTE));
		assertEquals(currTime.get(Calendar.SECOND), aCalendar.get(Calendar.SECOND));
		assertEquals(currTime.get(Calendar.MILLISECOND), aCalendar.get(Calendar.MILLISECOND));


		// update by hours
		a.setOffsetDays(0);
		a.setOffsetHours(3);
		a.setOffsetMins(0);

		aCalendar = a.getAlarmTimeCalendar();
		aCalendar.set(Calendar.DATE, 15);
		aCalendar.set(Calendar.HOUR_OF_DAY, 10);
		currTime.set(Calendar.DATE, 15);
		currTime.set(Calendar.HOUR_OF_DAY, 14);
		currTime.set(Calendar.MINUTE, 52);
		a.updateRingTime(currTime);

		aCalendar = a.getAlarmTimeCalendar();
		assertEquals(currTime.get(Calendar.YEAR), aCalendar.get(Calendar.YEAR));
		assertEquals(currTime.get(Calendar.MONTH), aCalendar.get(Calendar.MONTH));
		assertEquals(currTime.get(Calendar.DATE), aCalendar.get(Calendar.DATE));
		assertEquals(17, aCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(52, aCalendar.get(Calendar.MINUTE));
		assertEquals(currTime.get(Calendar.SECOND), aCalendar.get(Calendar.SECOND));
		assertEquals(currTime.get(Calendar.MILLISECOND), aCalendar.get(Calendar.MILLISECOND));

		// TODO: update by hours (to the next day)
		// update by days
		// update by hours and minutes
		// update by days and minutes
		// update by days and hours
		// update by days, hours, and minutes
	}
}
