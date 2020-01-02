package com.Cardinal.CommandPackage.Util;

import net.dv8tion.jda.api.entities.Message;

public class MessageUtils {

	public static boolean isCommand(Message message, String prefix) {
		String content = message.getContentRaw().replaceAll("(^\\s+|(?<=\\s{1})\\s+)", "").replaceAll("(?<=\\<\\@)!",
				"");
		String lowerCase = content.toLowerCase();
		try {
			while (Character.isWhitespace(content.charAt(0)))
				content = content.substring(1);

			while (Character.isWhitespace(content.charAt(prefix.length())))
				content = content.substring(0, prefix.length()) + content.substring(prefix.length() + 1);
		} catch (StringIndexOutOfBoundsException e) {
		}

		return lowerCase.startsWith(prefix);
	}

}
