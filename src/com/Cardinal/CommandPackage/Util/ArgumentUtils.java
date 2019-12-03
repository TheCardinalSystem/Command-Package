package com.Cardinal.CommandPackage.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;

/**
 * This class is used by the {@link MessageReceivedEventHandler} thread to parse
 * command arguments.
 * 
 * @author Cardinal System
 *
 */
public class ArgumentUtils {

	public static long parseUserMention(String argument) {
		Matcher m = Pattern.compile("(?<=(@|!))\\d+(?=\\>)").matcher(argument);
		if (m.find()) {
			return Long.parseLong(m.group());
		} else {
			return Long.parseLong(argument);
		}
	}

	public static long parseChannelMention(String argument) {
		Matcher m = Pattern.compile("(?<=#)\\d+(?=\\>)").matcher(argument);
		m.find();
		return Long.parseLong(m.group());
	}

	public static boolean parseBoolean(String s) {
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on")) {
			return true;
		} else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("off")) {
			return false;
		} else {
			throw new IllegalArgumentException("\"" + s + "\" does not match type: boolean");
		}
	}
}
