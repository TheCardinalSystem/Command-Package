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

import com.Cardinal.CommandPackage.Handle.Concurrent.OutputManager;
import com.Cardinal.CommandPackage.Handle.Concurrent.OutputTask;
import com.Cardinal.CommandPackage.Impl.CommandClient.CommandClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

public class PropertiesHandler {

	private static Gson GSON;
	private static GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
	private static File WORKING_DIRECTORY = new File(new File(System.getProperty("user.home")), ".Cardinal"),
			GUILD_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Guilds"),
			USER_PROP_DIR = new File(WORKING_DIRECTORY, "Properties/Users"),
			BOT_PROP_FILE = new File(WORKING_DIRECTORY, "Properties/Bot.json");

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
	 * @param type
	 *            type
	 * @param typeAdapter
	 *            typeAdapter
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public static void registerTypeAdapter(Type type, Object typeAdapter) {
		builder.registerTypeAdapter(type, typeAdapter);
	}

	public static void init(File workingDirectory) {

		GSON = builder.create();
		builder = null;

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
			}
		}

	}

	public static Gson getGson() {
		return GSON;
	}

	public static <T> T getBotProperty(String key, Type type) {
		return GSON.fromJson(bot_properties.get(key), type);
	}

	public static Set<String> getBotProperties() {
		return new HashSet<String>(bot_properties.keySet());
	}

	public static synchronized void setBotProperty(String key, Object value, Type type) {
		Object old = GSON.fromJson(bot_properties.get(key), type);
		bot_properties.add(key, GSON.toJsonTree(value, type));
		LISTENERS.forEach(p -> p.botPropertyChanged(key, old, value));
		String json = bot_properties.toString();
		OutputTask task = new OutputTask(BOT_PROP_FILE, json.getBytes(), false);
		try {
			OutputManager.queue(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

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

	public static File getBotPropertyFile() {
		return BOT_PROP_FILE;
	}

	public static synchronized void setGuildProperty(Guild guild, GuildProperties property, Object value) {
		setGuildProperty(guild, property.toString(), value, property.getType());
	}

	public static synchronized void setGuildProperty(Guild guild, String key, Object value, Type type) {
		long id = guild.getIdLong();
		final Object old;

		JsonObject props;
		if (GUILD_PROPERTIES.containsKey(id)) {
			props = GUILD_PROPERTIES.get(id).getAsJsonObject();
			old = GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key, GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			GUILD_PROPERTIES.put(id, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key, GSON.toJsonTree(value, type));
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

	public static synchronized <T> T getGuildProperty(Guild guild, GuildProperties property) {
		return getGuildProperty(guild, property.toString(), property.getType());
	}

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

	public static Set<String> getGuildProperties(Guild guild) {
		return new HashSet<String>(GUILD_PROPERTIES.get(guild.getIdLong()).keySet());
	}

	public static synchronized void removeGuildProperty(Guild guild, GuildProperties property) {
		removeGuildProperty(guild, property.toString(), property.getType());
	}

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

	public static File getGuildPropertyFile(Guild guild) {
		return new File(GUILD_PROP_DIR, guild.getId() + ".json");
	}

	public static void deleteGuildProperties(Guild guild) {
		long id = guild.getIdLong();
		GUILD_PROPERTIES.remove(id);
		File dest = new File(GUILD_PROP_DIR, id + ".json");
		dest.delete();
	}

	public static synchronized void setUserProperty(User user, String key, Object value, Type type) {
		long id = user.getIdLong();
		final Object old;

		JsonObject props;
		if (USER_PROPERTIES.containsKey(id)) {
			props = USER_PROPERTIES.get(id);
			old = GSON.fromJson(props.get(key), type);
			if (value != null) {
				props.add(key, GSON.toJsonTree(value, type));
			} else {
				props.remove(key);
			}
			USER_PROPERTIES.put(id, props);
		} else if (value != null) {
			old = null;
			props = GSON.toJsonTree(new HashMap<String, Object>()).getAsJsonObject();
			props.add(key, GSON.toJsonTree(value));
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

	public static synchronized Object getUserProperty(User user, String key, Type type) {
		long id = user.getIdLong();
		if (USER_PROPERTIES.containsKey(id)) {
			JsonObject props = USER_PROPERTIES.get(id);
			if (props != null && props.has(key)) {
				return GSON.fromJson(props.get(key), type);
			}
		}
		return null;
	}

	public static Set<String> getUserProperties(User user) {
		return new HashSet<String>(USER_PROPERTIES.get(user.getIdLong()).keySet());
	}

	public static synchronized void removeUserProperty(User user, String key, Type type) {
		long id = user.getIdLong();
		final Object old;

		JsonObject props;
		props = USER_PROPERTIES.get(id);
		old = GSON.fromJson(props.remove(key), type);
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

	public static void deleteUserProperties(User user) {
		long id = user.getIdLong();
		USER_PROPERTIES.remove(id);
		File dest = new File(USER_PROP_DIR, id + ".json");
		dest.delete();
	}

	public static File getUserPropertyFile(User user) {
		return new File(USER_PROP_DIR, user.getId() + ".json");
	}

	public static void addChangeListener(PropertyChangeListener listener) {
		LISTENERS.add(listener);
	}

	public static void removeChangeListener(PropertyChangeListener listener) {
		LISTENERS.remove(listener);
	}

	public static List<PropertyChangeListener> getChangeListeners() {
		return LISTENERS;
	}
}
