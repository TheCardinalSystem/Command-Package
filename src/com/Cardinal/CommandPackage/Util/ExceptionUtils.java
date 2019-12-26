package com.Cardinal.CommandPackage.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * A class of utility methods used for handling exceptions,
 * 
 * @author Cardinal System
 *
 */
public class ExceptionUtils {

	public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");

	/**
	 * Prints the given exception's stacktrace and sends it to the given channel.
	 * 
	 * @param e       the exception.
	 * @param channel the channel.
	 */
	public static void sendSystemException(Exception e, MessageChannel channel) {
		e.printStackTrace();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		String trace = os.toString();
		try {
			channel.sendMessage("A system error occurred. See details below:" + MarkdownUtils.codeBox(trace, "Java"))
					.queue();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
			try {
				File log = new File(System.getProperty("user.dir"),
						"errLog" + LocalDateTime.now().format(DEFAULT_TIME_FORMATTER) + ".log");
				FileOutputStream stream = new FileOutputStream(log);
				stream.write(trace.getBytes());
				stream.flush();
				stream.close();
				channel.sendFile(log, new MessageBuilder().append("A system error occurred. See attached log.").build())
						.queue(s -> log.delete());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

}
