package com.Cardinal.CommandPackage.Handle.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.Cardinal.CommandPackage.Entity.ListEmbed;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;

public class ListEmbedManager {

	/*
	 * Instance Code
	 */

	/**
	 * Keys are user IDs.
	 */
	private Map<String, ListEmbed> userEmbeds = new HashMap<String, ListEmbed>();
	/**
	 * Keys are message IDs, values are user IDs.
	 */
	private Map<String, String> messageUser = new HashMap<String, String>();

	private ListEmbedManager() {
	}

	private void addEmbed(String userID, ListEmbed embed) {
		userEmbeds.put(userID, embed);
	}

	private void addMessage(String messageID, String userID) {
		messageUser.put(messageID, userID);
	}

	public ListEmbed get(Message message) {
		return messageUser.containsKey(message.getId()) ? userEmbeds.get(messageUser.get(message.getId())) : null;
	}

	public ListEmbed get(String messageID) {
		return messageUser.containsKey(messageID) ? userEmbeds.get(messageUser.get(messageID)) : null;
	}

	public ListEmbed get(User user) {
		return userEmbeds.get(user.getId());
	}

	public ListEmbed get(String userID, boolean unusedFlag) {
		return userEmbeds.get(userID);
	}

	public Set<String> getMessageIDs() {
		return messageUser.keySet();
	}

	public Set<String> getUserIDs() {
		return userEmbeds.keySet();
	}

	public boolean contains(Message message) {
		return messageUser.containsKey(message.getId());
	}

	public boolean contains(String messageID) {
		return messageUser.containsKey(messageID);
	}

	/*
	 * Static Code
	 */

	/**
	 * Keys are guild IDs
	 */
	private static Map<String, ListEmbedManager> embedManagers = new HashMap<String, ListEmbedManager>();

	public static boolean isListEmbed(Message message) {
		return embedManagers.get(message.getGuild().getId()).contains(message);
	}

	public static boolean isListEmbed(String messageID, String guildID) {
		return embedManagers.get(guildID).contains(messageID);
	}

	public static boolean isListEmbed(String messageID) {
		return embedManagers.values().stream().anyMatch(e -> e.contains(messageID));
	}

	public static void sendEmbed(TextChannel channel, User source, ListEmbed embed) {
		ListEmbedManager manager;
		if (embedManagers.containsKey(channel.getGuild().getId())) {
			manager = embedManagers.get(channel.getGuild().getId());
		} else {
			manager = new ListEmbedManager();
			embedManagers.put(channel.getGuild().getId(), manager);
		}

		manager.addEmbed(source.getId(), embed);
		RestAction<Message> action = channel.sendMessage(embed.first());

		if (embed.isMultiPaged()) {
			action.queue(message -> {
				message.addReaction("\u23EA").queue(a -> message.addReaction("\u2B05")
						.queue(b -> message.addReaction("\u27A1").queue(c -> message.addReaction("\u23E9").queue())));
				manager.addMessage(message.getId(), source.getId());
			});
		} else {
			action.queue();
		}
	}

	public static void changeEmbed(Message message, int option) {
		ListEmbed embed = embedManagers.get(message.getGuild().getId()).get(message);

		if (option == 0) {
			message.editMessage(embed.first()).queue();
		} else if (option == 1) {
			message.editMessage(embed.previous()).queue();
		} else if (option == 2) {
			message.editMessage(embed.next()).queue();
		} else if (option == 3) {
			message.editMessage(embed.last()).queue();
		}

	}

	public static ListEmbedManager getManager(Guild guild) {
		return embedManagers.get(guild.getId());
	}
}