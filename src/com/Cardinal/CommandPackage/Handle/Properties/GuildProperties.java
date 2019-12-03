package com.Cardinal.CommandPackage.Handle.Properties;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * An enumeration of common properties that are different for each guild. The
 * command package uses all these properties when processing events.
 * 
 * @author Cardinal System
 * @see PropertiesHandler
 */
public enum GuildProperties {

	/**
	 * Each guild can set a unique command prefix for that guild. This property
	 * marks the chosen prefix. Saved as a string.
	 */
	PREFIX("prefix", String.class),
	/**
	 * Each guild can set unique command aliases. This property marks the command
	 * aliases. Saved as a map with the structure:<br>
	 * <br>
	 * <code>{"Alias": "Command"}</code><br>
	 * <br>
	 * Here is an example of an alias map serialized as a json object:
	 * 
	 * <pre>
	"aliases": {
		"h": "help",
		"k": "kick",
		"b": "ban"
	}
	 * </pre>
	 */
	COMMAND_ALIASES("aliases", Map.class),
	/**
	 * Guilds have the option to set a bot channel. The bot will only listen in the
	 * bot channel. This is a channel ID, and is saved as a string.
	 */
	BOT_CHANNEL("channel", String.class);

	private String s;
	private Type type;

	GuildProperties(String s, Type type) {
		this.s = s;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return s;
	}

	public static Optional<GuildProperties> get(String propertyName) {
		return Arrays.stream(GuildProperties.values()).filter(p -> p.toString().equalsIgnoreCase(propertyName))
				.findAny();
	}

	public static boolean isGuildProperty(String propertyName) {
		return Arrays.stream(GuildProperties.values()).map(g -> g.toString())
				.anyMatch(p -> p.equalsIgnoreCase(propertyName));

	}
}
