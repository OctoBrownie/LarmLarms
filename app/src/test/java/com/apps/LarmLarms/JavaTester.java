package com.apps.LarmLarms;

import org.junit.Test;

import java.util.Arrays;

/**
 * Not for any particular purpose, just a temporary class for testing random java things.
 */

public class JavaTester {
	@Test
	public void generalTester() {
		boolean[] repeatDays = {true, true, false, false, true, false, true};
		System.out.println(Arrays.toString(repeatDays));
		System.out.println(String.valueOf(repeatDays));
		System.out.println(repeatDays.toString());
		System.out.println(repeatDays[0]);
	}
}
