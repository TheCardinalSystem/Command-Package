package com.Cardinal.CommandPackage.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used by something :eyes:
 * 
 * @author Cardinal System
 *
 */
public class StringUtils {

	/**
	 * Compares two strings alphabetically/numerically from left to right.
	 * 
	 * @param x the first string.
	 * @param y the second string.
	 * @return the value {@code 0} if {@code x == y}; a value less than {@code 0} if
	 *         {@code x < y}; and a value greater than {@code 0} if {@code x > y}
	 */
	public static int compare(String x, String y) {
		x = x.toLowerCase();
		y = y.toLowerCase();
		int i = Character.compare(x.charAt(0), y.charAt(0));
		if (x.length() == 1 || y.length() == 1) {
			return i;
		}
		if (i == 0) {
			return compare(x.substring(1), x.substring(1));
		}
		return i;
	}

	/**
	 * Splits the given string into substrings of the given size.
	 * 
	 * @param string        the string.
	 * @param partitionSize the size.
	 * @return the substrings.
	 */
	public static List<String> split(String string, int partitionSize) {
		List<String> parts = new ArrayList<String>();
		int len = string.length();
		for (int i = 0; i < len; i += partitionSize) {
			parts.add(string.substring(i, Math.min(len, i + partitionSize)));
		}
		return parts;
	}
}
