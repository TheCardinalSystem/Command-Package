package com.Cardinal.CommandPackage.Handle.Properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Guild;

/**
 * A properties class used for managing guild-specific command aliases.
 * 
 * @author Cardinal System
 *
 */
public class GuildAliasHandler {

	public static Map<String, String> getAliasMappings(Guild guild) {
		return (Map<String, String>) PropertiesHandler.<Map<String, String>>getGuildProperty(guild,
				GuildProperties.COMMAND_ALIASES);
	}

	/**
	 * Get's the command name associated with the given the alias for the given
	 * guild.
	 * 
	 * @param guild the guild.
	 * @param alias the alias.
	 * @return the command name, or <b>null</b> if none is associated with the given
	 *         alias.
	 */
	public static String getCommand(Guild guild, String alias) {
		if (guild == null) {
			return null;
		}
		Map<String, String> map = getAliasMappings(guild);
		if (map != null)
			return map.get(alias.toLowerCase());
		return null;
	}

	/**
	 * Get's all the aliases associated with the given command name for this guild.
	 * 
	 * @param guild   the guild.
	 * @param command the command name.
	 * @return a list of aliases, or an empty list is none are present.
	 */
	public static List<String> getAliases(Guild guild, String command) {
		if (guild == null) {
			return new ArrayList<String>();
		}
		Map<String, String> map = getAliasMappings(guild);
		if (map != null) {
			return map.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(command)).map(e -> e.getKey())
					.collect(Collectors.toList());
		} else {
			return new ArrayList<String>();
		}
	}

	/**
	 * Maps the given aliases to the given command name for the given guild.
	 * 
	 * @param guild   the guild
	 * @param command the command name.
	 * @param aliases the aliases.
	 */
	public static synchronized void addAliases(Guild guild, String command, String... aliases) {
		Map<String, String> map = getAliasMappings(guild);
		if (map == null)
			map = new HashMap<String, String>();
		for (String s : aliases) {
			s = s.toLowerCase();
			if (map.containsKey(s))
				throw new IllegalArgumentException("Alias already registered: " + s);
			map.put(s, command);
		}

		PropertiesHandler.setGuildProperty(guild, GuildProperties.COMMAND_ALIASES, map);
	}

	/**
	 * Removes the given alias mappings from this given guild's aliases.
	 * 
	 * @param guild   the guild.
	 * @param aliases the aliases.
	 * @return an array of aliases that could be removed, or an empty array if all
	 *         were removed successfully.
	 */
	public static synchronized String[] removeAliases(Guild guild, String... aliases) {
		Map<String, String> map = getAliasMappings(guild);
		if (map == null)
			return new String[0];
		List<String> failed = new ArrayList<String>();
		for (String s : aliases) {
			s = s.toLowerCase();
			String com = map.remove(s);
			if (com == null) {
				failed.add(s);
			}
		}
		PropertiesHandler.setGuildProperty(guild, GuildProperties.COMMAND_ALIASES, map);
		return failed.toArray(new String[failed.size()]);
	}

}
