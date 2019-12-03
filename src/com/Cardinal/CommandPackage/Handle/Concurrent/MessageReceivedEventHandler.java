package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.Cardinal.CommandPackage.Command.ArgumentTypes;
import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Command.ISpecialAccesCommand;
import com.Cardinal.CommandPackage.Command.Category.DefaultCategories;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Properties.GuildAliasHandler;
import com.Cardinal.CommandPackage.Handle.Properties.GuildProperties;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;
import com.Cardinal.CommandPackage.Impl.CommandClient;
import com.Cardinal.CommandPackage.Util.ArgumentUtils;
import com.Cardinal.CommandPackage.Util.MarkdownUtils;
import com.Cardinal.CommandPackage.Util.NumberUtils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MessageReceivedEventHandler extends Thread {

	private Event event;
	private CommandRegistry registry;
	/**
	 * That's one big ass variable name.
	 */
	private static final Pattern NUMBER_FORMAT_EXCEPTION_EXTRACTION_PATTERN = Pattern
			.compile("(?<=\\:\\s\\\").+(?=\\\"$)");
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");

	public MessageReceivedEventHandler(Event event) {
		super("MessageReceivedEventHandler: Event" + event.hashCode());
		this.event = event;
	}

	public boolean isReady() {
		return registry != null;
	}

	void ready(CommandRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void run() {
		if (registry == null)
			throw new IllegalStateException("Handler not ready.");
		if (event instanceof MessageReceivedEvent) {
			synchronized (event) {
				MessageReceivedEvent wrappedEvent = (MessageReceivedEvent) event;
				setName("MessageHandler:" + ((MessageReceivedEvent) event).getMessageId());

				String botChannel = wrappedEvent.getChannel().getType().equals(ChannelType.PRIVATE)
						? wrappedEvent.getChannel().getId()
						: PropertiesHandler.<String>getGuildProperty(wrappedEvent.getGuild(),
								GuildProperties.BOT_CHANNEL);

				// Make sure the event author is not a bot.
				if (!wrappedEvent.getAuthor().isBot()
						&& (botChannel == null || botChannel.equals(wrappedEvent.getChannel().getId()))) {

					String prefix = getPrefix(wrappedEvent);
					String message = verifyMessage(wrappedEvent.getMessage().getContentRaw(), prefix);

					if (message != null) {
						if (message.equals("prefix")) {
							wrappedEvent.getChannel().sendMessage("Command prefix: " + MarkdownUtils.code(prefix))
									.queue();
							message = null;
						} else {

							String[] parts = breakUpMessage(message, prefix);

							ICommand command = registry.getCommand(parts[0]);
							if (command == null) {
								String com = GuildAliasHandler.getCommand(wrappedEvent.getGuild(), parts[0]);
								if (com != null)
									command = registry.getCommand(com);
							}

							if (command != null) {
								if (canAcess(wrappedEvent, command)) {
									ArgumentTypes[] types = command.getArgumentTypes();
									if (!(types == null || types.length == 0)) {
										if (!(parts.length - 1 < types.length) || hasOptional(types)) {
											Object[] args = null;
											try {
												args = processArguments(command, parts, wrappedEvent.getTextChannel(),
														wrappedEvent.getJDA());
											} catch (Exception e) {
												if (e instanceof IllegalArgumentException) {
													try {
														errorArg(wrappedEvent.getChannel(), e.getCause(),
																Integer.parseInt(e.getMessage()));
													} catch (NumberFormatException e1) {
														errorSyst(wrappedEvent.getChannel(), e);
													}
												} else {
													errorSyst(wrappedEvent.getChannel(), e);
												}
											}
											try {
												if (args != null) {
													command.execute(wrappedEvent, registry, prefix, args);
												}
											} catch (Exception e) {
												if (e instanceof IllegalArgumentException) {
													try {
														errorArg(wrappedEvent.getChannel(), e.getCause(),
																Integer.parseInt(e.getMessage()));
													} catch (NumberFormatException e1) {
														errorSyst(wrappedEvent.getChannel(), e);
													}
												} else {
													errorSyst(wrappedEvent.getChannel(), e);
												}
											}
										} else {
											errorLength(command, wrappedEvent.getChannel());
										}
									} else {
										try {
											command.execute(wrappedEvent, registry, prefix);
										} catch (Exception e) {
											errorSyst(wrappedEvent.getChannel(), e);
										}
									}
								} else {
									errorAcc(wrappedEvent.getChannel(), wrappedEvent.getAuthor());
								}
							} else {
								errorCom(wrappedEvent.getChannel(), registry, parts[0]);
							}
						}
					}
				}
			}
		}
		synchronized (event) {
			event.notifyAll();
		}
	}

	private Object[] processArguments(ICommand command, String[] parts, TextChannel channel, JDA jda) {
		ArgumentTypes[] types = command.getArgumentTypes();

		Object[] args = new Object[types.length];

		int index = -1;
		for (int i = 0; i < args.length; i++) {

			if (i + 1 > parts.length - 1) {
				if (types[i].isOptional()) {
					try {
						args[i] = parsePart(types[i], null, jda);
					} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | TypeNotPresentException
							| MalformedURLException | NullPointerException e) {
						e.printStackTrace();
					}
					break;
				} else {
					errorLength(command, channel);
					return null;
				}
			}

			String part = parts[i + 1];

			try {
				args[i] = parsePart(types[i], part, jda);
				if (args[i] == null) {
					if (types[i].equals(ArgumentTypes.CHANNEL_MENTION)) {
						throw new IllegalArgumentException(part + " is not a valid channel.");
					} else if (types[i].equals(ArgumentTypes.USER_MENTION)) {
						throw new IllegalArgumentException(part + " is not a valid user.");
					}
				}
			} catch (RuntimeException | MalformedURLException e) {
				if (e instanceof ArrayIndexOutOfBoundsException) {
					index = i + 1;
					break;
				} else if (e instanceof NumberFormatException) {
					Matcher m = NUMBER_FORMAT_EXCEPTION_EXTRACTION_PATTERN.matcher(e.getMessage());
					m.find();
					throw new IllegalArgumentException(String.valueOf(i),
							new IllegalArgumentException(MarkdownUtils.code(m.group()) + " is not a valid "
									+ types[i].toString().replaceAll("_", " ").toLowerCase() + ".", e));
				} else if (e instanceof MalformedURLException) {
					throw new IllegalArgumentException(String.valueOf(i),
							new IllegalArgumentException(MarkdownUtils.code(part) + " is not a valid URL.", e));
				} else {
					throw new IllegalArgumentException(String.valueOf(i), e);
				}
			}
		}
		if (index > -1) {
			String[] array = getArrayArgument(parts, index);
			if (types[index - 1].equals(ArgumentTypes.INT_ARRAY)) {
				args[index - 1] = Arrays.stream(array).mapToInt(Integer::parseInt).toArray();
			} else {
				args[index - 1] = array;
			}
		}

		return args;
	}

	private String[] getArrayArgument(String[] parts, int index) {
		return Arrays.copyOfRange(parts, index, parts.length);
	}

	private boolean hasOptional(ArgumentTypes[] types) {
		return Arrays.stream(types).anyMatch(t -> t.isOptional());
	}

	private String[] breakUpMessage(String message, String prefix) {
		message = message.substring(prefix.length()).replaceAll("^\\s+", "");
		return message.split("\\s");
	}

	private static String verifyMessage(String message, String prefix) {
		// Check that the message starts with the prefix.
		message = message.replaceAll("(^\\s+|(?<=\\s{1})\\s+)", "");
		try {
			while (Character.isWhitespace(message.charAt(0)))
				message = message.substring(1);

			while (Character.isWhitespace(message.charAt(prefix.length())))
				message = message.substring(0, prefix.length()) + message.substring(prefix.length() + 1);
		} catch (StringIndexOutOfBoundsException e) {
		}

		if (message.toLowerCase().startsWith(prefix)) {
			// Makes sure the prefix is not the only thing in the message.
			if (message.replaceAll("\\s+", "").equalsIgnoreCase(prefix)) {
				return null;
			}
			return message;
		} else if (message.toLowerCase().equals("prefix")) {
			return message;
		}
		return null;
	}

	private String getPrefix(MessageReceivedEvent event) {
		String prefix;
		if (!event.getChannelType().equals(ChannelType.PRIVATE)) {
			String obj = PropertiesHandler.<String>getGuildProperty(event.getGuild(), GuildProperties.PREFIX);
			prefix = obj == null ? CommandClient.DEFAULT_PREFIX : obj;
		} else {
			prefix = "";
		}
		return prefix.toLowerCase();
	}

	private String[] getSimilarSpellings(CommandRegistry registry, String command) {
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

	private void errorAcc(MessageChannel channel, User user) {
		channel.sendMessage(":warning: " + user.getAsMention() + " you do not have access to that command.").queue();
	}

	private void errorCom(MessageChannel channel, CommandRegistry registry, String command) {
		String[] similarCommands = getSimilarSpellings(registry, command);
		if (similarCommands.length > 5) {
			similarCommands = Arrays.copyOfRange(similarCommands, 0, 4);
		}
		channel.sendMessage(
				MarkdownUtils.code(command) + " is not a valid command.\nDid you mean "
						+ Arrays.toString(similarCommands)
								.replaceAll("((?<=\\[)\\w{0}|\\w{0}(?=\\])|\\w{0}(?=,)|(?<=\\s)\\w{0})", "`")
						+ "?")
				.queue();
	}

	private void errorLength(ICommand command, MessageChannel channel) {
		channel.sendMessage(
				"This command takes " + MarkdownUtils.code(String.valueOf(command.getArgumentTypes().length))
						+ " arguments." + MarkdownUtils.codeBox(Arrays.stream(command.getArgumentNames())
								.map(s -> "<" + s + ">").collect(Collectors.joining(" "))))
				.queue();
	}

	private void errorArg(MessageChannel channel, Throwable e, int index) {
		channel.sendMessage(NumberUtils.toOrdinal(index) + " argument: " + e.getMessage()).queue();
	}

	private void errorSyst(MessageChannel channel, Exception e) {
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
						"errLog" + LocalDateTime.now().format(FORMATTER) + ".log");
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

	private Object parsePart(ArgumentTypes type, String part, JDA jda) throws IllegalArgumentException,
			ArrayIndexOutOfBoundsException, TypeNotPresentException, MalformedURLException {

		if (!(type.equals(ArgumentTypes.URL) || type.equals(ArgumentTypes.STRING_ARRAY)
				|| type.equals(ArgumentTypes.STRING) || part == null)) {
			part = part.toLowerCase();
		}
		if (type.equals(ArgumentTypes.BOOLEAN)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Boolean) null);
			return type.isOptional() ? Optional.ofNullable(ArgumentUtils.parseBoolean(part))
					: ArgumentUtils.parseBoolean(part);
		} else if (type.equals(ArgumentTypes.INTEGER)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Integer) null);
			return type.isOptional() ? Optional.ofNullable(Integer.parseInt(part)) : Integer.parseInt(part);
		} else if (type.equals(ArgumentTypes.LONG)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Long) null);
			return type.isOptional() ? Optional.ofNullable(Long.parseLong(part)) : Long.parseLong(part);
		} else if (type.equals(ArgumentTypes.CHARACTER)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Character) null);
			return type.isOptional() ? Optional.ofNullable(parseCharacter(part)) : parseCharacter(part);
		} else if (type.equals(ArgumentTypes.DOUBLE)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Double) null);
			return type.isOptional() ? Optional.ofNullable(Double.parseDouble(part)) : Double.parseDouble(part);
		} else if (type.equals(ArgumentTypes.FLOAT)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Float) null);
			return type.isOptional() ? Optional.ofNullable(Float.parseFloat(part)) : Float.parseFloat(part);
		} else if (type.equals(ArgumentTypes.STRING)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable(part);
			return type.isOptional() ? Optional.ofNullable(part) : part;
		} else if (type.equals(ArgumentTypes.STRING_ARRAY) || type.equals(ArgumentTypes.INT_ARRAY)) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (type.equals(ArgumentTypes.USER_MENTION)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((User) null);
			return type.isOptional() ? Optional.ofNullable(jda.getUserById(ArgumentUtils.parseUserMention(part)))
					: jda.getUserById(ArgumentUtils.parseUserMention(part));
		} else if (type.equals(ArgumentTypes.CHANNEL_MENTION)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((TextChannel) null);
			return type.isOptional()
					? Optional.ofNullable(jda.getTextChannelById(ArgumentUtils.parseChannelMention(part)))
					: jda.getTextChannelById(ArgumentUtils.parseChannelMention(part));

		} else if (type.equals(ArgumentTypes.URL)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((URL) null);
			return type.isOptional() ? Optional.ofNullable(new URL(part)) : new URL(part);
		} else {
			throw new TypeNotPresentException(type.toString(),
					new IllegalArgumentException(type.toString() + " is not a valid type."));
		}
	}

	private boolean canAcess(MessageReceivedEvent event, ICommand command) {
		if (CommandClient.DEVELOPER_IDS.contains(event.getAuthor().getId())) {
			return true;
		}
		if (event.getChannelType().equals(ChannelType.PRIVATE)) {
			if (command.getCategory().equals(DefaultCategories.DEVELOPER)) {
				return false;
			} else {
				return true;
			}
		}
		return command.getCategory().canAccess(((MessageReceivedEvent) event).getMember())
				|| command instanceof ISpecialAccesCommand;
	}

	private char parseCharacter(String s) throws IllegalArgumentException {
		if (s.length() == 1) {
			return s.charAt(0);
		} else {
			throw new IllegalArgumentException("\"" + s + "\" does not match type: character");
		}
	}
}
