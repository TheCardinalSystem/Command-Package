package com.Cardinal.CommandPackage.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.Cardinal.CommandPackage.Command.Category.DefaultCategories;
import com.Cardinal.CommandPackage.Command.Category.ICategory;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Properties.GuildAliasHandler;
import com.Cardinal.CommandPackage.Impl.CommandClient;
import com.Cardinal.CommandPackage.Util.ArgumentUtils;
import com.Cardinal.CommandPackage.Util.ColorUtils;
import com.Cardinal.CommandPackage.Util.MarkdownUtils;
import com.Cardinal.CommandPackage.Util.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A handy help command. This command must be added manually.
 * 
 * @author Cardinal System
 *
 */
public class DefaultHelpCommand implements ICommand {

	private String support;

	public DefaultHelpCommand() {

	}

	/**
	 * Constructs this help command with a support URL. This is often an invite link
	 * to a Discord server.
	 * 
	 * @param supportDiscord the support URL.
	 */
	public DefaultHelpCommand(String supportDiscord) {
		this.support = supportDiscord;
	}

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public ICategory getCategory() {
		return DefaultCategories.SUPPORT;
	}

	@Override
	public ArgumentTypes[] getArgumentTypes() {
		return new ArgumentTypes[] { ArgumentTypes.STRING.optional() };
	}

	@Override
	public String[] getArgumentNames() {
		return new String[] { "Command/Category" };
	}

	@Override
	public String getDescription() {
		return "Sends a help page for the given command/category, or, if not specified, a list of commands and command syntaxes.";
	}

	@Override
	public void execute(MessageReceivedEvent event, CommandRegistry registry, String prefix, Object... arguments) {
		@SuppressWarnings("unchecked")
		Optional<String> optional = (Optional<String>) arguments[0];

		EmbedBuilder builder = new EmbedBuilder();
		List<MessageEmbed> embeds = new ArrayList<MessageEmbed>();
		boolean build = true, privateChannel = true;

		builder.setColor(ColorUtils.getRandomColor());

		if (optional.isPresent()) {
			String comm = optional.get();
			ICommand command = registry.getCommand(comm);
			if (command == null) {
				String com = GuildAliasHandler.getCommand(event.getGuild(), comm);
				if (com != null)
					command = registry.getCommand(com);
			}

			if (command != null) {
				Set<String> aliases = registry.getAliases(command);
				if (aliases == null) {
					aliases = new HashSet<String>();
				}
				aliases.addAll(GuildAliasHandler.getAliases(event.getGuild(), command.getName()));

				builder.setTitle(command.getName());
				builder.setDescription(command.getDescription());
				String args = convertArguments(command.getArgumentTypes(), command.getArgumentNames());
				builder.addField("Aliases",
						Arrays.toString(aliases.stream().map(s -> prefix + s).toArray(String[]::new))
								.replaceAll("((?<=\\[)\\w{0}|\\w{0}(?=\\])|\\w{0}(?=,)|(?<=\\s)\\w{0})", "`"),
						true);
				builder.addField("Arguments:", args.isEmpty() ? "None" : args, true);
				builder.addField("Access:",
						MarkdownUtils
								.code(org.apache.commons.lang3.StringUtils
										.capitalize(command.getCategory().getName().toLowerCase()))
								+ " : " + MarkdownUtils.code(String.valueOf(command.getCategory().getLevel())) + "\n"
								+ MarkdownUtils.code(event.getAuthor().getName()) + " : "
								+ MarkdownUtils.code(String.valueOf(canAcess(event, command.getCategory()))),
						true);

				privateChannel = false;
			} else {
				List<ICommand> commands = new ArrayList<ICommand>();
				for (ICommand c : registry.getCommands()) {
					if (c.getCategory().getName().equalsIgnoreCase(comm)) {
						commands.add(c);
					}
				}

				if (commands.isEmpty()) {
					comm = comm.toLowerCase();
					for (ICommand c : registry.getCommands()) {
						if (c.getCategory().getName().toLowerCase().contains(comm)) {
							commands.add(c);
						}
					}
				}

				if (commands.isEmpty()) {
					String[] similarCommands = ArgumentUtils.getSimilarSpellings(registry, comm);
					if (similarCommands.length > 5) {
						similarCommands = Arrays.copyOfRange(similarCommands, 0, 4);
					}
					event.getChannel()
							.sendMessage(MarkdownUtils.code(comm) + " is not a valid command.\nDid you mean "
									+ Arrays.toString(similarCommands).replaceAll(
											"((?<=\\[)\\w{0}|\\w{0}(?=\\])|\\w{0}(?=,)|(?<=\\s)\\w{0})", "`")
									+ "?")
							.queue();
					return;
				} else {
					HashMap<ICategory, List<ICommand>> map = new HashMap<ICategory, List<ICommand>>();
					map.put(commands.get(0).getCategory(), commands);
					build = buildHelp(builder, registry, event.getGuild(), prefix,
							"List of " + org.apache.commons.lang3.StringUtils.capitalize(comm) + " Commands", embeds,
							map);

					privateChannel = false;
				}
			}
		} else {
			HashMap<ICategory, List<ICommand>> map = new HashMap<ICategory, List<ICommand>>();
			for (ICommand c : registry.getCommands()) {
				ICategory cat = c.getCategory();
				if (cat != null) {
					if (!canAcess(event, cat))
						continue;
					if (map.containsKey(cat)) {
						map.get(cat).add(c);
					} else {
						map.put(cat, new ArrayList<ICommand>(Arrays.asList(new ICommand[] { c })));
					}
				}
			}
			// |\n----------------------------------------------------------------------------------------
			build = buildHelp(builder, registry, event.getGuild(), prefix, "List of Commands.", embeds, map);
			privateChannel = true;
		}

		if (support != null && !support.isEmpty())
			builder.addField("Further Support", support, false);

		if (build) {
			embeds.add(builder.build());
		}
		MessageChannel channel = privateChannel ? event.getAuthor().openPrivateChannel().complete()
				: event.getChannel();
		for (MessageEmbed embed : embeds) {
			channel.sendMessage(embed).queue();
		}
		if (!event.getChannel().getType().equals(ChannelType.PRIVATE) && privateChannel)
			event.getChannel().sendMessage("Direct Message Sent :mailbox_with_mail:").queue();
	}

	private boolean buildHelp(EmbedBuilder builder, CommandRegistry registry, Guild guild, String prefix, String title,
			List<MessageEmbed> embeds, HashMap<ICategory, List<ICommand>> map) {
		boolean build = true;
		List<Field> fields = new ArrayList<Field>();
		List<Entry<ICategory, List<ICommand>>> list = map.entrySet().stream()
				.sorted((e1, e2) -> Integer.compare(e1.getKey().getLevel(), e2.getKey().getLevel()))
				.collect(Collectors.toList());

		for (int i = 0; i < list.size(); i++) {
			Entry<ICategory, List<ICommand>> entry = list.get(i);
			List<String> parts = new ArrayList<String>();
			for (ICommand c : entry.getValue().stream()
					.sorted((o1, o2) -> StringUtils.compare(o1.getName(), o2.getName())).collect(Collectors.toList())) {
				Set<String> aliases = registry.getAliases(c);
				if (aliases == null) {
					aliases = new HashSet<String>();
				}
				if (guild != null) {
					aliases.addAll(GuildAliasHandler.getAliases(guild, c.getName()));
				}

				String part;
				if (aliases.isEmpty()) {
					part = "\n\n"
							+ MarkdownUtils.bold(prefix + c.getName() + " "
									+ convertArguments(c.getArgumentTypes(), c.getArgumentNames()) + ":")
							+ "\n\t" + c.getDescription();
				} else {
					part = "\n\n"
							+ MarkdownUtils.bold(prefix + c.getName() + " "
									+ convertArguments(c.getArgumentTypes(), c.getArgumentNames()) + ":")
							+ "\nAliases: "
							+ Arrays.toString(aliases.stream().map(s -> prefix + s).toArray(String[]::new))
									.replaceAll("((?<=\\[)\\w{0}|\\w{0}(?=\\])|\\w{0}(?=,)|(?<=\\s)\\w{0})", "`")
							+ "\n" + c.getDescription();
				}
				parts.add(part);
			}
			String value = "--------------------------------------------------------------------------------";

			boolean cont = false;
			for (int j = 0; j < parts.size(); j++) {
				if (value.length() + parts.get(j).length() + 82 > 1024) {
					value += "\n\n--------------------------------------------------------------------------------";
					Field f = new Field(org.apache.commons.lang3.StringUtils.capitalize(entry.getKey().getName())
							+ (cont ? " (" + MarkdownUtils.italics("Continued") + ")" : ""), value, false);
					fields.add(f);
					value = "--------------------------------------------------------------------------------"
							+ parts.get(j);
					cont = true;
				} else {
					value += parts.get(j);
				}
			}

			value += "\n\n--------------------------------------------------------------------------------";
			Field f = new Field(org.apache.commons.lang3.StringUtils.capitalize(entry.getKey().getName())
					+ (cont ? " (" + MarkdownUtils.italics("Continued") + ")" : ""), value, false);
			fields.add(f);
		}

		builder.setTitle(MarkdownUtils.boldUnderline("List of Commands"));
		builder.setDescription("|\n--------------------------------------------------------------------------------");

		for (int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			int length = f.getValue().length() + f.getName().length();
			if (builder.length() + length >= MessageEmbed.EMBED_MAX_LENGTH_BOT) {
				embeds.add(builder.build());
				if (i == fields.size() - 1) {
					build = false;
				} else {
					builder.clearFields();
					builder.setTitle(
							MarkdownUtils.bold("List of Commands") + " " + MarkdownUtils.italics("(Continued)"));
				}
			} else {
				builder.addField(f);
			}
		}
		return build;
	}

	private String convertArguments(ArgumentTypes[] types, String[] names) {
		if (types != null && types.length > 0) {
			String temp = "";
			for (int i = 0; i < types.length; i++) {
				if (types[i].equals(ArgumentTypes.CUSTOM))
					break;
				temp += "<" /* (i == 0 ? "<**`" : "\n<**`") */ + typeToString(types[i]) + ": "
						+ MarkdownUtils.code(names[i]) + (types[i].isOptional() ? "(Optional)" : "") + ">";
			}
			return temp;
		}
		return "";
	}

	private String typeToString(ArgumentTypes type) {

		String name = type.equals(ArgumentTypes.CHANNEL_MENTION) ? "Channel"
				: type.equals(ArgumentTypes.USER_MENTION) ? "User"
						: type.equals(ArgumentTypes.STRING_ARRAY) ? "Array" : type.toString().toLowerCase();
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private boolean canAcess(MessageReceivedEvent event, ICategory category) {
		if (CommandClient.DEVELOPER_IDS.contains(event.getAuthor().getId())) {
			return true;
		}
		if (event.getChannelType().equals(ChannelType.PRIVATE)) {
			if (category.equals(DefaultCategories.DEVELOPER)) {
				return false;
			} else {
				return true;
			}
		}
		return category.canAccess(event.getMember());
	}
}
