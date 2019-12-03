package com.Cardinal.CommandPackage.Handle.Properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Guild;

public class GuildAliasHandler {

	public static Map<String, String> getAliasMappings(Guild guild) {
		return (Map<String, String>) PropertiesHandler.<Map<String, String>>getGuildProperty(guild,
				GuildProperties.COMMAND_ALIASES);
	}

	public static String getCommand(Guild guild, String alias) {
		if (guild == null) {
			return null;
		}
		Map<String, String> map = getAliasMappings(guild);
		if (map != null)
			return getAliasMappings(guild).get(alias.toLowerCase());
		return null;
	}

	public static List<String> getAliases(Guild guild, String command) {
		if (guild == null) {
			return new ArrayList<String>();
		}
		Map<String, String> map = getAliasMappings(guild);
		if (map != null) {
			return getAliasMappings(guild).entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(command))
					.map(e -> e.getKey()).collect(Collectors.toList());
		} else {
			return new ArrayList<String>();
		}
	}

	public static void addAliases(Guild guild, String command, String... aliases) {
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

	public static String[] removeAliases(Guild guild, String... aliases) {
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
