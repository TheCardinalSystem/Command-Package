package com.Cardinal.CommandPackage.Util;

import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;

/**
 * This class is used by the {@link MessageReceivedEventHandler} thread to
 * handle argument indices.
 * 
 * @author Cardinal System
 *
 */
public class NumberUtils {

	public static String toOrdinal(int i) {
		String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
		switch (i % 100) {
		case 11:
		case 12:
		case 13:
			return i + "th";
		default:
			return i + sufixes[i % 10];

		}
	}

	public static String humanReadableByteCountSI(long bytes) {
		String s = bytes < 0 ? "-" : "";
		long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		return b < 1000L ? bytes + " B"
				: b < 999_950L ? String.format("%s%.1f kB", s, b / 1e3)
						: (b /= 1000) < 999_950L ? String.format("%s%.1f MB", s, b / 1e3)
								: (b /= 1000) < 999_950L ? String.format("%s%.1f GB", s, b / 1e3)
										: (b /= 1000) < 999_950L ? String.format("%s%.1f TB", s, b / 1e3)
												: (b /= 1000) < 999_950L ? String.format("%s%.1f PB", s, b / 1e3)
														: String.format("%s%.1f EB", s, b / 1e6);
	}

	public static String nanosToString(long nanos) {
		double value;
		if ((value = nanos / (6 * Math.pow(10, 10))) >= 1) {
			return value + "Mins";
		} else if ((value = nanos / Math.pow(10, 9)) >= 1) {
			return value + "Secs";
		} else if ((value = nanos / Math.pow(10, 6)) >= 1) {
			return value + "Milis";
		}
		return nanos + "Nanos";
	}

	public static String milisToString(long milis) {
		double value;
		if ((value = milis / (6 * Math.pow(10, 4))) >= 1) {
			return value + "Mins";
		} else if ((value = milis / Math.pow(10, 3)) >= 1) {
			return value + "Secs";
		}
		return milis + "Milis";
	}

}
