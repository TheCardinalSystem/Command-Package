package com.Cardinal.CommandPackage.Util;

import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;

/**
 * This class is used by the {@link MessageReceivedEventHandler} thread to handle argument
 * indices.
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

}
