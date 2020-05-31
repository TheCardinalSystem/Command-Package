package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.Cardinal.CommandPackage.Command.ArgumentTypes;
import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Command.ISpecialAccesCommand;
import com.Cardinal.CommandPackage.Command.Category.DefaultCategories;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Properties.GuildAliasHandler;
import com.Cardinal.CommandPackage.Handle.Properties.GuildPermissionsManager;
import com.Cardinal.CommandPackage.Handle.Properties.GuildProperties;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;
import com.Cardinal.CommandPackage.Impl.CommandClient;
import com.Cardinal.CommandPackage.Util.ArgumentUtils;
import com.Cardinal.CommandPackage.Util.ExceptionUtils;
import com.Cardinal.CommandPackage.Util.MarkdownUtils;
import com.Cardinal.CommandPackage.Util.NumberUtils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A thread used for handling {@linkplain MessageReceivedEvent}s. Really, this
 * should only be used by the command pakcage's build-in event adapter.
 * 
 * @author Cardinal System
 *
 */
public class MessageReceivedEventHandler extends Thread {

	private MessageReceivedEvent event;
	private CommandRegistry registry;
	private BiConsumer<Exception, MessageReceivedEvent> biConsumer = null;
	public long startTime = 0;
	/**
	 * That's one big ass variable name.
	 */
	private static final Pattern NUMBER_FORMAT_EXCEPTION_EXTRACTION_PATTERN = Pattern
			.compile("(?<=\\:\\s\\\").+(?=\\\"$)");

	public MessageReceivedEventHandler(MessageReceivedEvent event) {
		super("MessageReceivedEventHandler:" + event.getAuthor().getName() + ":" + event.hashCode());
		this.event = event;
	}

	public MessageReceivedEventHandler(MessageReceivedEvent event,
			BiConsumer<Exception, MessageReceivedEvent> errorHandler) {
		super("MessageReceivedEventHandler:" + event.getAuthor().getName() + ":" + event.hashCode());
		this.event = event;
		this.biConsumer = errorHandler;
	}

	public MessageReceivedEvent getEvent() {
		return event;
	}

	public boolean isReady() {
		return registry != null;
	}

	void ready(CommandRegistry registry) {
		this.registry = registry;
	}

	@Override
	public synchronized void start() {
		startTime = System.nanoTime();
		super.start();
	}

	@Override
	public void run() {
		if (registry == null)
			throw new IllegalStateException("Handler not ready.");
		if (event instanceof MessageReceivedEvent) {
			synchronized (event) {
				MessageReceivedEvent wrappedEvent = (MessageReceivedEvent) event;
				setName("MessageHandler:" + ((MessageReceivedEvent) event).getMessageId());

				boolean isPrivate = event.getChannelType().equals(ChannelType.PRIVATE);
				if ((event.getChannelType().equals(ChannelType.TEXT) || isPrivate)
						&& checkChannel(event.getChannel(), isPrivate ? null : event.getGuild(), event.getAuthor())) {
					String prefix = getPrefix(wrappedEvent);

					String message = wrappedEvent.getMessage().getContentRaw();
					if (message.equalsIgnoreCase("prefix")) {
						wrappedEvent.getChannel().sendMessage("Command prefix: " + MarkdownUtils.code(prefix)).queue();
						message = null;
					} else {
						message = verifyMessage(message, prefix);

						if (message != null) {
							String[] parts = breakUpMessage(message, prefix);

							ICommand command = registry.getCommand(parts[0]);
							if (command == null) {
								String com = GuildAliasHandler.getCommand(wrappedEvent.getGuild(), parts[0]);
								if (com != null)
									command = registry.getCommand(com);
							}

							if (command != null) {
								if (canAcess(wrappedEvent, command, registry)) {
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
														errorSyst(event, e);
													}
												} else {
													errorSyst(event, e);
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
														errorSyst(event, e);
													}
												} else {
													errorSyst(event, e);
												}
											}
										} else {
											errorLength(command, wrappedEvent.getChannel());
										}
									} else {
										try {
											command.execute(wrappedEvent, registry, prefix);
										} catch (Exception e) {
											errorSyst(event, e);
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

	private boolean checkChannel(MessageChannel channel, Guild guild, User user) {
		String botChannel = channel.getType().equals(ChannelType.PRIVATE) ? channel.getId()
				: PropertiesHandler.<String>getGuildProperty(guild, GuildProperties.BOT_CHANNEL);

		if (!user.isBot() && (botChannel == null || botChannel.equals(channel.getId())
				|| CommandClient.DEVELOPER_IDS.contains(event.getAuthor().getId()))) {
			return true;
		}

		if (channel.getType().equals(ChannelType.PRIVATE)) {
			return true;
		}

		List<String> unboundUsers = PropertiesHandler.getGuildProperty(guild, GuildProperties.UNBOUND_USERS);
		return unboundUsers != null && unboundUsers.contains(user.getId());
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
					} else if (types[i].equals(ArgumentTypes.ROLE_MENTION)) {
						throw new IllegalArgumentException(part + " is not a valid role.");
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
			try {
				String[] array = getArrayArgument(parts, index);
				if (types[index - 1].equals(ArgumentTypes.INT_ARRAY)) {
					args[index - 1] = Arrays.stream(array).mapToInt(Integer::parseInt).toArray();
				} else if (types[index - 1].equals(ArgumentTypes.DOUBLE_ARRAY)) {
					args[index - 1] = Arrays.stream(array).mapToDouble(Double::parseDouble).toArray();
				} else if (types[index - 1].equals(ArgumentTypes.LONG_ARRAY)) {
					args[index - 1] = Arrays.stream(array).mapToLong(Long::parseLong).toArray();
				} else {
					args[index - 1] = array;
				}
			} catch (NumberFormatException e) {
				Matcher m = NUMBER_FORMAT_EXCEPTION_EXTRACTION_PATTERN.matcher(e.getMessage());
				m.find();
				throw new IllegalArgumentException(String.valueOf(index),
						new IllegalArgumentException(MarkdownUtils.code(m.group()) + " is not a valid "
								+ types[index - 1].toString().replaceAll("_", " ").toLowerCase() + ".", e));
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
		message = message.replaceAll("(^\\s+|(?<=\\s{1})\\s+)", "").replaceAll("(?<=\\<\\@)!", "");
		String lowerCase = message.toLowerCase();
		try {
			while (Character.isWhitespace(message.charAt(0)))
				message = message.substring(1);

			while (Character.isWhitespace(message.charAt(prefix.length())))
				message = message.substring(0, prefix.length()) + message.substring(prefix.length() + 1);
		} catch (StringIndexOutOfBoundsException e) {
		}

		if (lowerCase.startsWith(prefix)) {
			// Makes sure the prefix is not the only thing in the message.
			if (message.replaceAll("\\s+", "").equalsIgnoreCase(prefix)) {
				return null;
			}
			return message;
		} else if (lowerCase.equals("prefix")) {
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

	private void errorAcc(MessageChannel channel, User user) {
		channel.sendMessage(":warning: " + user.getAsMention() + " you do not have access to that command.").queue();
	}

	private void errorCom(MessageChannel channel, CommandRegistry registry, String command) {
		String[] similarCommands = ArgumentUtils.getSimilarSpellings(registry, command);
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
		channel.sendMessage(NumberUtils.toOrdinal(index + 1) + " argument: " + e.getMessage()).queue();
	}

	private void errorSyst(MessageReceivedEvent event, Exception e) {
		if (biConsumer != null) {
			biConsumer.accept(e, event);
		} else {
			ExceptionUtils.sendSystemException(e, event.getChannel());
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
		} else if (type.equals(ArgumentTypes.STRING_ARRAY) || type.equals(ArgumentTypes.INT_ARRAY)
				|| type.equals(ArgumentTypes.LONG_ARRAY) || type.equals(ArgumentTypes.DOUBLE_ARRAY)) {
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

		} else if (type.equals(ArgumentTypes.ROLE_MENTION)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((Role) null);
			return type.isOptional() ? Optional.ofNullable(jda.getRoleById(ArgumentUtils.parseRoleMention(part)))
					: jda.getRoleById(ArgumentUtils.parseRoleMention(part));
		} else if (type.equals(ArgumentTypes.URL)) {
			if (part == null || part.isEmpty())
				return Optional.ofNullable((URL) null);
			return type.isOptional() ? Optional.ofNullable(new URL(part)) : new URL(part);
		} else {
			throw new TypeNotPresentException(type.toString(),
					new IllegalArgumentException(type.toString() + " is not a valid type."));
		}
	}

	private boolean canAcess(MessageReceivedEvent event, ICommand command, CommandRegistry registry) {
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

		return GuildPermissionsManager.getManager(event.getGuild(), registry).canAccess(event.getAuthor(), command)
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
