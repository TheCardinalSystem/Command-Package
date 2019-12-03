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
import com.Cardinal.CommandPackage.Handle.Command.CommandRegisterException;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Properties.GuildAliasHandler;
import com.Cardinal.CommandPackage.Impl.CommandClient;
import com.Cardinal.CommandPackage.Util.ColorUtils;
import com.Cardinal.CommandPackage.Util.MarkdownUtils;
import com.Cardinal.CommandPackage.Util.StringUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class DefaultHelpCommand implements ICommand {

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
		return new String[] { "Command" };
	}

	@Override
	public String getDescription() {
		return "Sends the syntax for the given command, or, if no command is specified, a list of commands and command syntaxes.";
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
				throw new IllegalArgumentException("1", new CommandRegisterException(
						"There is no command registered under the name " + MarkdownUtils.code(comm)));
			}
			builder.setTitle(command.getName());
			builder.setDescription(command.getDescription());
			builder.addField("Arguments:", convertArguments(command.getArgumentTypes(), command.getArgumentNames()),
					true);
			builder.addField("Access:",
					MarkdownUtils
							.code(org.apache.commons.lang3.StringUtils
									.capitalize(command.getCategory().getName().toLowerCase()))
							+ " : " + MarkdownUtils.code(String.valueOf(command.getCategory().getLevel())) + "\n"
							+ MarkdownUtils.code(event.getAuthor().getName()) + " : "
							+ MarkdownUtils.code(String.valueOf(command.getCategory().canAccess(event.getMember()))),
					true);

			privateChannel = false;
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
			List<Field> fields = new ArrayList<Field>();
			List<Entry<ICategory, List<ICommand>>> list = map.entrySet().stream()
					.sorted((e1, e2) -> Integer.compare(e1.getKey().getLevel(), e2.getKey().getLevel()))
					.collect(Collectors.toList());

			for (int i = 0; i < list.size(); i++) {
				Entry<ICategory, List<ICommand>> entry = list.get(i);
				List<String> parts = new ArrayList<String>();
				for (ICommand c : entry.getValue().stream()
						.sorted((o1, o2) -> StringUtils.compare(o1.getName(), o2.getName()))
						.collect(Collectors.toList())) {
					Set<String> aliases = registry.getAliases(c);
					if (aliases == null) {
						aliases = new HashSet<String>();
					}
					if (aliases.isEmpty()) {
						aliases.addAll(GuildAliasHandler.getAliases(event.getGuild(), c.getName()));
					}
					String part;
					if (aliases.isEmpty()) {
						part = "\n\n"
								+ MarkdownUtils.bold(prefix + c.getName() + " "
										+ convertArguments(c.getArgumentTypes(), c.getArgumentNames()) + ":")
								+ "\n\t" + c.getDescription();
					} else {
						part = "\n\n"
								+ MarkdownUtils.boldUnderline(prefix + c.getName() + " "
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
						if (i < list.size() - 1)
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
			builder.setDescription(
					"|\n--------------------------------------------------------------------------------");

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
		}

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

	private String convertArguments(ArgumentTypes[] types, String[] names) {
		if (types != null && types.length > 0) {
			String temp = "";
			for (int i = 0; i < types.length; i++) {
				if (types[i].equals(ArgumentTypes.CUSTOM))
					break;
				temp += "<" /* (i == 0 ? "<**`" : "\n<**`") */ + typeToString(types[i]) + ": " + names[i]
						+ (types[i].isOptional() ? "(Optional)" : "") + ">";
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
		return category.canAccess(((MessageReceivedEvent) event).getMember());
	}
}
