package com.Cardinal.CommandPackage.Handle.Properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Handle.Concurrent.OutputManager;
import com.Cardinal.CommandPackage.Handle.Concurrent.OutputTask;
import com.Cardinal.CommandPackage.Impl.CommandClient;
import com.Cardinal.CommandPackage.Impl.CommandClient.CommandClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

/**
 * A class used for saving, loading, and accessing configurations for the bot
 * and specific users and guilds.
 * 
 * @author Cardinal System
 *
 */
public class PropertiesHandler {

	private static Gson GSON;
	private static GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
	private static File WORKING_DIRECTORY = new File(new File(System.getProperty("user.home")), ".Cardinal"),
			GUILD_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Guilds"),
			USER_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Users"),
			BOT_PROP_FILE = new File(WORKING_DIRECTORY, "Properties/Bot.json");

	/**
	 * This bot property key is connected to an array of {@linkplain Permission}s
	 * that the bot requires to operate. This is used to generate an invite link for
	 * the bot. If your bot needs permissions to operate certain <i>commands</i>,
	 * look at {@link ICommand#getPermissions()}.
	 */
	public static final String BOT_PERMISSIONS_PROPERTY = "permissionsNeeded";

	private static JsonObject bot_properties;
	private static final Map<Long, JsonObject> GUILD_PROPERTIES = new HashMap<Long, JsonObject>(),
			USER_PROPERTIES = new HashMap<Long, JsonObject>();
	// private static final ScheduledExecutorService SERVICE =
	// Executors.newScheduledThreadPool(1);
	private static final List<PropertyChangeListener> LISTENERS = new ArrayList<PropertyChangeListener>();

	/**
	 * Registers a type adapter for the Gson. This method only works before
	 * {@link PropertiesHandler#init(File)} is invoked, which subsequently happens
	 * after {@link CommandClientBuilder#build()} is invoked.
	 * 
	 * @param type        type
	 * @param typeAdapter typeAdapter
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public static void registerTypeAdapter(Type type, Object typeAdapter) {
		builder.registerTypeAdapter(type, typeAdapter);
	}

	/**
	 * Sets up this handler's environment and loads all configurations. Leave
	 * innovation of this method to the {@link CommandClient}.
	 * 
	 * @param workingDirectory the root directory of this program's workplace.
	 */
	public static void init(File workingDirectory) {

		GSON = builder.create();

		if (workingDirectory != null) {
			WORKING_DIRECTORY = workingDirectory;
			GUILD_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Guilds");
			USER_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Users");
			BOT_PROP_FILE = new File(WORKING_DIRECTORY, "Properties/Bot.json");
		}

		File[] files = GUILD_PROP_DIR.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
		if (files != null) {
			for (File file : files) {
				long id = Long.parseLong(file.getName().replaceAll("\\D+", ""));
				try {
					GUILD_PROPERTIES.put(id, GSON.fromJson(new FileReader(file), JsonObject.class));
				} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		files = USER_PROP_DIR.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
		if (files != null) {
			for (File file : files) {
				long id = Long.parseLong(file.getName().replaceAll("\\D+", ""));
				try {
					USER_PROPERTIES.put(id, GSON.fromJson(new FileReader(file), JsonObject.class));
				} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		if (BOT_PROP_FILE.exists()) {
			try {
				bot_properties = GSON.fromJson(new FileReader(BOT_PROP_FILE), JsonObject.class);
			} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
				e.printStackTrace();
				bot_properties = new JsonObject();
			}
		} else {
			bot_properties = new JsonObject();
		}

		if (!bot_properties.has(BOT_PERMISSIONS_PROPERTY)) {
			setBotProperty(BOT_PERMISSIONS_PROPERTY,
					new Permission[] { Permission.MESSAGE_READ, Permission.MESSAGE_WRITE,
							Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION,
							Permission.MESSAGE_MANAGE },
					Permission[].class);
		}
	}

	/**
	 * Get's the {@link Gson} instance this class is using to serialize/deserialize
	 * configurations.
	 * 
	 * @return the Gson instance.
	 */
	public static Gson getGson() {
		return GSON;
	}

	/**
	 * Used to determine whether the bot configuration has the given property
	 * present.
	 * 
	 * @param key the property name.
	 * @return <b>true</b> : the bot config has the given property.<br>
	 *         <b>false</b> : the bot config does not have the given property.
	 */
	public static boolean hasBotProperty(String key) {
		return bot_properties.has(key);
	}

	/**
	 * Gets the given property from the bot configuration, parsing it to the given
	 * type.
	 * 
	 * @param <T>  I forgot why I put this here...
	 * @param key  the property name.
	 * @param type the property type.
	 * @return the deserialized property.
	 */
	public static <T> T getBotProperty(String key, Type type) {
		return GSON.fromJson(bot_properties.get(key), type);
	}

	/**
	 * Gets a set of bot property names.
	 * 
	 * @return the set.
	 */
	public static Set<String> getBotProperties() {
		return new HashSet<String>(bot_properties.keySet());
	}

	/**
	 * Sets the given bot property to the given value, using the given type
	 * parameter to parse the old value associated with this property (so it can
	 * passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param key   the property name.
	 * @param value the property value.
	 * @param type  the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void setBotProperty(String key, Object value, Type type) {

		Object old = GSON.fromJson(bot_properties.get(key), type);
		bot_properties.add(key,
				type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
						: GSON.toJsonTree(value, type));
		LISTENERS.forEach(p -> p.botPropertyChanged(key, old, value));
		String json = bot_properties.toString();
		OutputTask task = new OutputTask(BOT_PROP_FILE, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remove the given bot property, using the given type parameter to parse the
	 * old value associated with this property (so it can passed to the
	 * {@linkplain PropertyChangeListener}s).
	 * 
	 * @param key  the property name.
	 * @param type the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void removeBotProperty(String key, Type type) {
		Object old = GSON.fromJson(bot_properties.remove(key), type);
		LISTENERS.forEach(p -> p.botPropertyChanged(key, old, null));
		String json = bot_properties.toString();
		OutputTask task = new OutputTask(BOT_PROP_FILE, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the configuration file where bot properties are saved.
	 * 
	 * @return the config file.
	 */
	public static File getBotPropertyFile() {
		return BOT_PROP_FILE;
	}

	/**
	 * Determines whether the given guild's configuration has the given property
	 * present.
	 * 
	 * @param guild the guild.
	 * @param key   the property name
	 * @return <b>true</b> : the guild config has the given property.<br>
	 *         <b>false</b> : the guild config does not have the given property.
	 */
	public static boolean hasGuildProperty(Guild guild, String key) {
		return GUILD_PROPERTIES.containsKey(guild.getIdLong()) ? GUILD_PROPERTIES.get(guild.getIdLong()).has(key)
				: false;
	}

	/**
	 * Determines whether the given guild's configuration has the given property
	 * present.
	 * 
	 * @param guildID the guild's ID.
	 * @param key     the property name
	 * @return <b>true</b> : the guild config has the given property.<br>
	 *         <b>false</b> : the guild config does not have the given property.
	 */
	public static boolean hasGuildProperty(long guildID, String key) {
		return GUILD_PROPERTIES.containsKey(guildID) ? GUILD_PROPERTIES.get(guildID).has(key) : false;
	}

	/**
	 * Sets the given property for the given guild to the given value.
	 * 
	 * @param guild    the guild.
	 * @param property the property.
	 * @param value    the value.
	 */
	public static synchronized void setGuildProperty(Guild guild, GuildProperties property, Object value) {
		setGuildProperty(guild, property.toString(), value, property.getType());
	}

	/**
	 * Sets the given property for the given guild to the given value.
	 * 
	 * @param guildID  the guild's ID.
	 * @param property the property.
	 * @param value    the value.
	 */
	public static synchronized void setGuildProperty(long guildID, GuildProperties property, Object value) {
		setGuildProperty(guildID, property.toString(), value, property.getType());
	}

	/**
	 * Sets the given property for the given guild to the given value, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param guild the guild.
	 * @param key   the property name.
	 * @param value the property value.
	 * @param type  the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void setGuildProperty(Guild guild, String key, Object value, Type type) {
		long id = guild.getIdLong();
		final Object old;

		JsonObject props;
		if (GUILD_PROPERTIES.containsKey(id)) {
			props = GUILD_PROPERTIES.get(id).getAsJsonObject();
			old = type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
					: GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key,
						type.equals(JsonElement.class) && value instanceof String
								? JsonParser.parseString((String) value)
								: GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			GUILD_PROPERTIES.put(id, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key,
					type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
							: GSON.toJsonTree(value, type));
			GUILD_PROPERTIES.put(id, props);
		} else {
			old = null;
		}
		LISTENERS.forEach(p -> p.guildPropertyChanged(guild, key, old, value));
		String json = GUILD_PROPERTIES.get(id).toString();
		File dest = new File(GUILD_PROP_DIR, id + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the given property for the given guild to the given value, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param guildID the guild's ID.
	 * @param key     the property name.
	 * @param value   the property value.
	 * @param type    the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void setGuildProperty(long guildID, String key, Object value, Type type) {
		final Object old;

		JsonObject props;
		if (GUILD_PROPERTIES.containsKey(guildID)) {
			props = GUILD_PROPERTIES.get(guildID).getAsJsonObject();
			old = type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
					: GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key,
						type.equals(JsonElement.class) && value instanceof String
								? JsonParser.parseString((String) value)
								: GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			GUILD_PROPERTIES.put(guildID, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key,
					type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
							: GSON.toJsonTree(value, type));
			GUILD_PROPERTIES.put(guildID, props);
		} else {
			old = null;
		}
		LISTENERS.forEach(p -> p.guildPropertyChanged(guildID, key, old, value));
		String json = GUILD_PROPERTIES.get(guildID).toString();
		File dest = new File(GUILD_PROP_DIR, guildID + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the value of the given property for the given guild.
	 * 
	 * @param <T>      I forgot why I put this here...
	 * @param guild    the guild.
	 * @param property the property.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getGuildProperty(Guild guild, GuildProperties property) {
		return getGuildProperty(guild, property.toString(), property.getType());
	}

	/**
	 * Gets the value of the given property for the given guild.
	 * 
	 * @param <T>      I forgot why I put this here...
	 * @param guildID  the guild's ID.
	 * @param property the property.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getGuildProperty(long guildID, GuildProperties property) {
		return getGuildProperty(guildID, property.toString(), property.getType());
	}

	/**
	 * Gets the value of the given property for the given guild, using the given
	 * type parameter to deserialize it.
	 * 
	 * @param <T>   I forgot why I put this here...
	 * @param guild the guild.
	 * @param key   the property name.
	 * @param type  the property type.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getGuildProperty(Guild guild, String key, Type type) {
		long id = guild.getIdLong();
		if (GUILD_PROPERTIES.containsKey(id)) {
			JsonObject props = GUILD_PROPERTIES.get(id);
			if (props != null && props.has(key)) {
				return GSON.fromJson(props.get(key), type);
			}
		}
		return null;
	}

	/**
	 * Gets the value of the given property for the given guild, using the given
	 * type parameter to deserialize it.
	 * 
	 * @param <T>     I forgot why I put this here...
	 * @param guildID the guild's ID.
	 * @param key     the property name.
	 * @param type    the property type.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getGuildProperty(long guildID, String key, Type type) {
		if (GUILD_PROPERTIES.containsKey(guildID)) {
			JsonObject props = GUILD_PROPERTIES.get(guildID);
			if (props != null && props.has(key)) {
				return GSON.fromJson(props.get(key), type);
			}
		}
		return null;
	}

	/**
	 * Gets a set of property names present in the configurations for the given
	 * guild.
	 * 
	 * @param guild the guild.
	 * @return the set.
	 */
	public static Set<String> getGuildProperties(Guild guild) {
		return new HashSet<String>(GUILD_PROPERTIES.get(guild.getIdLong()).keySet());
	}

	/**
	 * Gets a set of property names present in the configurations for the given
	 * guild.
	 * 
	 * @param guildID the guild's ID.
	 * @return the set.
	 */
	public static Set<String> getGuildProperties(long guildID) {
		return new HashSet<String>(GUILD_PROPERTIES.get(guildID).keySet());
	}

	/**
	 * Removes the given property from the given guild's configuration.
	 * 
	 * @param guild    the guild.
	 * @param property the property.
	 */
	public static synchronized void removeGuildProperty(Guild guild, GuildProperties property) {
		removeGuildProperty(guild, property.toString(), property.getType());
	}

	/**
	 * Removes the given property from the given guild's configuration.
	 * 
	 * @param guildID  the guild's ID.
	 * @param property the property.
	 */
	public static synchronized void removeGuildProperty(long guildID, GuildProperties property) {
		removeGuildProperty(guildID, property.toString(), property.getType());
	}

	/**
	 * Removes the given property from the given guild's configuration, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param guild the guild.
	 * @param key   the property name.
	 * @param type  the property type.
	 */
	public static synchronized void removeGuildProperty(Guild guild, String key, Type type) {
		long id = guild.getIdLong();
		final Object old;

		JsonObject props;
		props = GUILD_PROPERTIES.get(id);
		old = GSON.fromJson(props.remove(key), type);
		GUILD_PROPERTIES.put(id, props);
		LISTENERS.forEach(p -> p.guildPropertyChanged(guild, key, old, null));
		String json = GUILD_PROPERTIES.get(id).toString();
		File dest = new File(GUILD_PROP_DIR, id + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes the given property from the given guild's configuration, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param guildID the guild's ID.
	 * @param key     the property name.
	 * @param type    the property type.
	 */
	public static synchronized void removeGuildProperty(long guildID, String key, Type type) {
		final Object old;

		JsonObject props;
		props = GUILD_PROPERTIES.get(guildID);
		old = GSON.fromJson(props.remove(key), type);
		GUILD_PROPERTIES.put(guildID, props);
		LISTENERS.forEach(p -> p.guildPropertyChanged(guildID, key, old, null));
		String json = GUILD_PROPERTIES.get(guildID).toString();
		File dest = new File(GUILD_PROP_DIR, guildID + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the configuration file for the given guild.
	 * 
	 * @param guild the guild.
	 * @return the file.
	 */
	public static File getGuildPropertyFile(Guild guild) {
		return new File(GUILD_PROP_DIR, guild.getId() + ".json");
	}

	/**
	 * Gets the configuration file for the given guild.
	 * 
	 * @param guildID the guild's ID.
	 * @return the file.
	 */
	public static File getGuildPropertyFile(long guildID) {
		return new File(GUILD_PROP_DIR, guildID + ".json");
	}

	/**
	 * Deletes the configuration file for the given guild.
	 * 
	 * @param guild the guild.
	 */
	public static void deleteGuildProperties(Guild guild) {
		long id = guild.getIdLong();
		GUILD_PROPERTIES.remove(id);
		File dest = new File(GUILD_PROP_DIR, id + ".json");
		dest.delete();
	}

	/**
	 * Deletes the configuration file for the given guild.
	 * 
	 * @param guildID the guild's ID.
	 */
	public static void deleteGuildProperties(long guildID) {
		GUILD_PROPERTIES.remove(guildID);
		File dest = new File(GUILD_PROP_DIR, guildID + ".json");
		dest.delete();
	}

	/**
	 * Get's the ID's of all users who have saved properties.
	 * 
	 * @return a set of user ID's.
	 */
	public static Set<Long> getUsersProperties() {
		return USER_PROPERTIES.keySet();
	}

	/**
	 * Determines whether the given user's configuration has the given property
	 * present.
	 * 
	 * @param user the user.
	 * @param key  the property name
	 * @return <b>true</b> : the user config has the given property.<br>
	 *         <b>false</b> : the user config does not have the given property.
	 */
	public static boolean hasUserProperty(User user, String key) {
		return USER_PROPERTIES.containsKey(user.getIdLong()) ? USER_PROPERTIES.get(user.getIdLong()).has(key) : false;
	}

	/**
	 * Determines whether the given user's configuration has the given property
	 * present.
	 * 
	 * @param userID the user's ID.
	 * @param key    the property name
	 * @return <b>true</b> : the user config has the given property.<br>
	 *         <b>false</b> : the user config does not have the given property.
	 */
	public static boolean hasUserProperty(long userID, String key) {
		return USER_PROPERTIES.containsKey(userID) ? USER_PROPERTIES.get(userID).has(key) : false;
	}

	/**
	 * Sets the given property for the given user to the given value, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param user  the user.
	 * @param key   the property name.
	 * @param value the property value.
	 * @param type  the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void setUserProperty(User user, String key, Object value, Type type) {
		long id = user.getIdLong();
		final Object old;

		JsonObject props;
		if (USER_PROPERTIES.containsKey(id)) {
			props = USER_PROPERTIES.get(id);
			old = GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key,
						type.equals(JsonElement.class) && value instanceof String
								? JsonParser.parseString((String) value)
								: GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			USER_PROPERTIES.put(id, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key,
					type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
							: GSON.toJsonTree(value));
			USER_PROPERTIES.put(id, props);
		} else {
			old = null;
		}
		LISTENERS.forEach(p -> p.userPropertyChanged(user, key, old, value));
		String json = USER_PROPERTIES.get(id).toString();
		File dest = new File(USER_PROP_DIR, id + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the given property for the given user to the given value, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param userID the user's ID.
	 * @param key    the property name.
	 * @param value  the property value.
	 * @param type   the property type (When in doubt, use {@link JsonObject}).
	 */
	public static synchronized void setUserProperty(long userID, String key, Object value, Type type) {
		final Object old;

		JsonObject props;
		if (USER_PROPERTIES.containsKey(userID)) {
			props = USER_PROPERTIES.get(userID);
			old = GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key,
						type.equals(JsonElement.class) && value instanceof String
								? JsonParser.parseString((String) value)
								: GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			USER_PROPERTIES.put(userID, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key,
					type.equals(JsonElement.class) && value instanceof String ? JsonParser.parseString((String) value)
							: GSON.toJsonTree(value));
			USER_PROPERTIES.put(userID, props);
		} else {
			old = null;
		}
		LISTENERS.forEach(p -> p.userPropertyChanged(userID, key, old, value));
		String json = USER_PROPERTIES.get(userID).toString();
		File dest = new File(USER_PROP_DIR, userID + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the value of the given property for the given user, using the given type
	 * parameter to deserialize it.
	 * 
	 * @param <T>  I forgot why I put this here...
	 * @param user the user.
	 * @param key  the property name.
	 * @param type the property type.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getUserProperty(User user, String key, Type type) {
		long id = user.getIdLong();
		if (USER_PROPERTIES.containsKey(id)) {
			JsonObject props = USER_PROPERTIES.get(id);
			if (props != null && props.has(key)) {
				return GSON.fromJson(props.get(key), type);
			}
		}
		return null;
	}

	/**
	 * Gets the value of the given property for the given user, using the given type
	 * parameter to deserialize it.
	 * 
	 * @param <T>    I forgot why I put this here...
	 * @param userID the user's ID.
	 * @param key    the property name.
	 * @param type   the property type.
	 * @return the deserialized value.
	 */
	public static synchronized <T> T getUserProperty(long userID, String key, Type type) {
		if (USER_PROPERTIES.containsKey(userID)) {
			JsonObject props = USER_PROPERTIES.get(userID);
			if (props != null && props.has(key)) {
				return GSON.fromJson(props.get(key), type);
			}
		}
		return null;
	}

	/**
	 * Gets a set of property names present in the given user's configuration.
	 * 
	 * @param user the user.
	 * @return the set.
	 */
	public static Set<String> getUserProperties(User user) {
		return new HashSet<String>(USER_PROPERTIES.get(user.getIdLong()).keySet());
	}

	/**
	 * Gets a set of property names present in the given user's configuration.
	 * 
	 * @param userID the user's ID.
	 * @return the set.
	 */
	public static Set<String> getUserProperties(long userID) {
		return new HashSet<String>(USER_PROPERTIES.get(userID).keySet());
	}

	/**
	 * Removes the given property from the given user's configuration, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param user the user.
	 * @param key  the property name.
	 * @param type the property type.
	 */
	public static synchronized void removeUserProperty(User user, String key, Type type) {
		long id = user.getIdLong();
		final Object old;

		JsonObject props;
		props = USER_PROPERTIES.get(id);
		JsonElement el = props.remove(key);
		if (el == null) {
			return;
		}
		old = GSON.fromJson(el, type);
		USER_PROPERTIES.put(id, props);
		LISTENERS.forEach(p -> p.userPropertyChanged(user, key, old, null));
		String json = USER_PROPERTIES.get(id).toString();
		File dest = new File(USER_PROP_DIR, id + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes the given property from the given user's configuration, using the
	 * given type parameter to parse the old value associated with this property (so
	 * it can passed to the {@linkplain PropertyChangeListener}s).
	 * 
	 * @param userID the user's ID.
	 * @param key    the property name.
	 * @param type   the property type.
	 */
	public static synchronized void removeUserProperty(long userID, String key, Type type) {
		final Object old;

		JsonObject props;
		props = USER_PROPERTIES.get(userID);
		JsonElement el = props.remove(key);
		if (el == null) {
			return;
		}
		old = GSON.fromJson(el, type);
		USER_PROPERTIES.put(userID, props);
		LISTENERS.forEach(p -> p.userPropertyChanged(userID, key, old, null));
		String json = USER_PROPERTIES.get(userID).toString();
		File dest = new File(USER_PROP_DIR, userID + ".json");
		OutputTask task = new OutputTask(dest, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes the configuration file for the givne user.
	 * 
	 * @param user the user.
	 */
	public static void deleteUserProperties(User user) {
		long id = user.getIdLong();
		USER_PROPERTIES.remove(id);
		File dest = new File(USER_PROP_DIR, id + ".json");
		dest.delete();
	}

	/**
	 * Deletes the configuration file for the givne user.
	 * 
	 * @param userID the user's ID.
	 */
	public static void deleteUserProperties(long userID) {
		USER_PROPERTIES.remove(userID);
		File dest = new File(USER_PROP_DIR, userID + ".json");
		dest.delete();
	}

	/**
	 * Gets the configuration file for the given user.
	 * 
	 * @param user the user.
	 * @return the file.
	 */
	public static File getUserPropertyFile(User user) {
		return new File(USER_PROP_DIR, user.getId() + ".json");
	}

	/**
	 * Gets the configuration file for the given user.
	 * 
	 * @param userID the user's ID.
	 * @return the file.
	 */
	public static File getUserPropertyFile(long userID) {
		return new File(USER_PROP_DIR, userID + ".json");
	}

	/**
	 * Adds the given {@link PropertyChangeListener} to this properties handler.
	 * 
	 * @param listener the listener.
	 */
	public static void addChangeListener(PropertyChangeListener listener) {
		LISTENERS.add(listener);
	}

	/**
	 * Removes the given {@link PropertyChangeListener} to this properties handler.
	 * 
	 * @param listener the listener.
	 */
	public static void removeChangeListener(PropertyChangeListener listener) {
		LISTENERS.remove(listener);
	}

	/**
	 * Gets a list of this handler's {@linkplain PropertyChangeListener}s.
	 * 
	 * @return the listeners.
	 */
	public static List<PropertyChangeListener> getChangeListeners() {
		return LISTENERS;
	}
}
