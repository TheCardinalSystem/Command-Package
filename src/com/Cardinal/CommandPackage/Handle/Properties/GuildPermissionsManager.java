package com.Cardinal.CommandPackage.Handle.Properties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

/**
 * A class used for handling guild-specific permissions (more specifically,
 * permissions related to commands).
 * 
 * @author Cardinal System
 *
 */
public class GuildPermissionsManager {

	/**
	 * A map off {@linkplain GuildPermissionsManager}s for each guild.
	 */
	private static final Map<String, GuildPermissionsManager> GUILD_PERMISSIONS = new HashMap<String, GuildPermissionsManager>();

	private Guild guild;
	private Map<String, Set<ICommand>> userAccess = new HashMap<String, Set<ICommand>>();
	private CommandRegistry registry;

	/**
	 * Constructs a {@link GuildPermissionsManager} for the given guild. The
	 * {@link CommandRegistry} argument is necessary for loading permissions
	 * configurations.
	 * 
	 * @param guild    the guild.
	 * @param registry the registry.
	 */
	private GuildPermissionsManager(Guild guild, CommandRegistry registry) {
		this.guild = guild;
		this.registry = registry;
	}

	/**
	 * Get's the list of commands which the given user has special access to in this
	 * guild.
	 * 
	 * @param user the user.
	 * @return the commands.
	 */
	public Set<ICommand> getAccess(User user) {
		return userAccess.get(user.getId());
	}

	/**
	 * Gives the given user access to the given command in this guild.
	 * 
	 * @param user    the user.
	 * @param command the command.
	 */
	public void addAccess(User user, ICommand command) {
		String id = user.getId();
		if (userAccess.containsKey(id)) {
			userAccess.get(id).add(command);
		} else {
			userAccess.put(id, new HashSet<ICommand>(Arrays.asList(command)));
		}
		save();
	}

	/**
	 * Removes access to the given command from the given user in this guild.
	 * 
	 * @param user    the user.
	 * @param command the command.
	 */
	public void removeAccess(User user, ICommand command) {
		String id = user.getId();
		if (userAccess.containsKey(id)) {
			userAccess.get(id).remove(command);
			save();
		}
	}

	/**
	 * Determines whether the given user can access the given command in this guild.
	 * 
	 * @param user    the user.
	 * @param command the command.
	 * @return <b>true</b> : the user can access the command<br>
	 *         <b>false</b> : the user cannot access the command
	 */
	public boolean canAccess(User user, ICommand command) {
		String id = user.getId();
		if (userAccess.containsKey(id)) {
			return userAccess.get(id).stream().anyMatch(c -> c.getName().equalsIgnoreCase(command.getName()));
		}

		return command.getCategory().canAccess(guild.getMember(user));
	}

	private void save() {
		PropertiesHandler.setGuildProperty(guild, GuildProperties.USER_ACCESS, userAccess);
	}

	private GuildPermissionsManager load() {
		Map<String, Set<String>> map = PropertiesHandler.getGuildProperty(guild, GuildProperties.USER_ACCESS);
		if (map != null) {
			for (String id : map.keySet()) {
				Set<ICommand> commands = new HashSet<ICommand>();
				for (String comm : map.get(id)) {
					ICommand command = registry.getCommand(comm);
					if (command == null) {
						String com = GuildAliasHandler.getCommand(guild, comm);
						if (com != null)
							command = registry.getCommand(com);
					}
					if (command != null) {
						commands.add(command);
					}
				}
				if (!commands.isEmpty()) {
					userAccess.put(id, commands);
				}
			}
		}
		return this;
	}

	/**
	 * Gets the {@link GuildPermissionsManager} for the given guild. he
	 * {@link CommandRegistry} argument is necessary for loading permissions
	 * configurations.
	 * 
	 * @param guild    the guild.
	 * @param registry the registry.
	 * @return the permissions manager.
	 */
	public static GuildPermissionsManager getManager(Guild guild, CommandRegistry registry) {
		String id = guild.getId();
		if (!GUILD_PERMISSIONS.containsKey(id)) {
			GuildPermissionsManager manager = new GuildPermissionsManager(guild, registry).load();
			GUILD_PERMISSIONS.put(id, manager);
			return manager;
		}
		return GUILD_PERMISSIONS.get(id);
	}

}
