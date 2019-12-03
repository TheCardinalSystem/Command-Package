package com.Cardinal.CommandPackage.Util;

import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;

/**
 * This class provides methods to cleanly format text with Markdown. It is by
 * the {@link MessageReceivedEventHandler} thread and is recommended for to be used by command
 * package implementations as well.
 * 
 * @author Cardinal System
 *
 */
public class MarkdownUtils {
	public static String italics(String s) {
		return "*" + s + "*";
	}

	public static String bold(String s) {
		return "**" + s + "**";
	}

	public static String boldItalics(String s) {
		return "***" + s + "***";
	}

	public static String underline(String s) {
		return "__" + s + "__";

	}

	public static String boldUnderline(String s) {
		return bold("__" + s + "__");

	}

	public static String boldItalicsUnderline(String s) {
		return boldItalics(underline(s));

	}

	public static String strikethrough(String s) {
		return "~~" + s + "~~";
	}

	public static String boldStrikethrough(String s) {
		return "~~" + bold(s) + "~~";
	}

	public static String boldItalicsStrikethrough(String s) {
		return strikethrough(boldItalics(s));
	}

	public static String boldItalicsUnderlineStrikethrough(String s) {
		return strikethrough(boldItalicsUnderline(s));
	}

	public static String code(String s) {
		return "`" + s + "`";
	}

	public static String codeBox(String s) {
		return "```" + s + "```";
	}

	public static String codeBox(String s, String format) {
		return "```" + format + "\n" + s + "```";
	}
}
