package com.Cardinal.CommandPackage.Util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;

/**
 * This class is used by the {@link MessageReceivedEventHandler} thread to parse
 * command arguments.
 * 
 * @author Cardinal System
 *
 */
public class ArgumentUtils {

	private static final Pattern USER_MENTION = Pattern.compile("(?<=(@|!))\\d+(?=\\>)"),
			CHANNEL_MENTION = Pattern.compile("(?<=#)\\d+(?=\\>)"),
			ROLE_MENTION = Pattern.compile("(?<=(\\@|\\&))\\d+(?=\\>)");

	public static String[] getSimilarSpellings(CommandRegistry registry, String command) {
		Object[][] spells = registry.getCommandNames().stream()
				.map(s -> new Object[] { s, LevenshteinDistance.getDefaultInstance().apply(s, command) })
				.sorted((o1, o2) -> Integer.compare((int) o1[1], (int) o2[1])).toArray(Object[][]::new);
		int average;
		if (spells.length < 5) {
			average = (int) spells[spells.length][1];
		} else {
			average = ((int) spells[0][1] + (int) spells[spells.length - 1][1]) / 2;
		}
		return Arrays.stream(spells).filter(t -> (int) t[1] <= average).map(o -> (String) o[0]).toArray(String[]::new);
	}

	public static long parseUserMention(String argument) {
		Matcher m = USER_MENTION.matcher(argument);
		if (m.find()) {
			return Long.parseLong(m.group());
		} else {
			return Long.parseLong(argument);
		}
	}

	public static long parseChannelMention(String argument) {
		Matcher m = CHANNEL_MENTION.matcher(argument);
		m.find();
		return Long.parseLong(m.group());
	}

	public static long parseRoleMention(String argument) {
		Matcher m = ROLE_MENTION.matcher(argument);
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
