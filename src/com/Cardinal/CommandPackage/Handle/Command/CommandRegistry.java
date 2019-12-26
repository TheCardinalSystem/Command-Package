package com.Cardinal.CommandPackage.Handle.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.Cardinal.CommandPackage.Command.ArgumentTypes;
import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Impl.CommandClient;

/**
 * A class used for mapping {@link ICommand} objects.
 * 
 * @author Cardinal System
 *
 */
public class CommandRegistry {

	private Map<String, ICommand> commands;
	private Map<String, String> aliases;

	/**
	 * Constructs a new {@linkplain CommandRegistry} with no mappings.
	 */
	public CommandRegistry() {
		commands = new HashMap<String, ICommand>();
		aliases = new HashMap<String, String>();
	}

	/**
	 * Constructs a new {@linkplain CommandRegistry} and maps the given commands.
	 * 
	 * @param commands the commands to map.
	 * @throws CommandRegisterException thrown if there an problem registering any
	 *                                  commands.
	 */
	public CommandRegistry(ICommand... commands) throws CommandRegisterException {
		this.commands = new HashMap<String, ICommand>();
		aliases = new HashMap<String, String>();
		registerCommands(commands);
	}

	/**
	 * Maps the given command into the registry.
	 * 
	 * @param command the command to map/register.
	 * @throws CommandRegisterException Thrown (1) If a command with the same name
	 *                                  is already mapped, (2) if the command has
	 *                                  more than one array argument type, (3) if
	 *                                  the command has more than one optional
	 *                                  argument, or (4) if the command has both
	 *                                  optional and array argument types.
	 * @see CommandRegistry#unregisterCommand(ICommand)
	 */
	public synchronized void registerCommand(ICommand command) throws CommandRegisterException {
		String name = command.getName().toLowerCase();
		if (commands.containsKey(name))
			throw new CommandRegisterException(command,
					new IllegalArgumentException(name + " is already a registered command."));

		if (command.getArgumentTypes() != null) {
			int ars = 0, ops = 0, arOps = 0;
			boolean cust = false;
			for (ArgumentTypes type : command.getArgumentTypes()) {
				if (type.equals(ArgumentTypes.STRING_ARRAY)) {
					ars++;
					if (type.isOptional()) {
						arOps++;
					}
				}
				if (type.isOptional()) {
					ops++;
				}
				if (type.equals(ArgumentTypes.CUSTOM)) {
					cust = true;
					break;
				}
			}

			if ((ars > 1 || ops > 1 || (ars >= 1 && ops >= 1 && !(ars == ops && ops == arOps))) && !cust) {
				throw new CommandRegisterException(command, new IllegalArgumentException(
						ars + " array argument types and " + ops + " optional arguments."));
			}
		}

		commands.put(name, command);
		CommandClient.LOGGER.info("Registered command \"" + name + "\"");
	}

	/**
	 * Maps the given commands into the registry.
	 * 
	 * @param commands the commands to map/register.
	 * 
	 * @throws CommandRegisterException thrown if there an problem registering the
	 *                                  command.
	 * @see CommandRegistry#registerCommand(ICommand)
	 * @see CommandRegistry#unregisterCommands(ICommand...)
	 */
	public synchronized void registerCommands(ICommand... commands) throws CommandRegisterException {
		for (ICommand c : commands) {
			registerCommand(c);
		}
	}

	/**
	 * Registers an alias for the given command. This will allows user to use the
	 * given alias to invoke the given command.
	 * 
	 * @param command the command.
	 * @param alias   the alias.
	 * @throws CommandRegisterException thrown if the alias is already registered or
	 *                                  if it matches a command name.
	 * @see CommandRegistry#registerAlias(String, String)
	 */
	public synchronized void registerAlias(ICommand command, String alias) throws CommandRegisterException {
		if (aliases.containsKey(alias)) {
			throw new CommandRegisterException(command,
					new IllegalArgumentException("Alias \"" + alias + "\" is already registered."));
		}
		if (commands.containsKey(alias)) {
			throw new CommandRegisterException(command,
					new IllegalArgumentException("Alias \"" + alias + "\" is the name of a registered command."));
		}
		aliases.put(alias.toLowerCase(), command.getName().toLowerCase());
		CommandClient.LOGGER.info("Registered alias \"" + alias + "\" for command: " + command.getName());
	}

	/**
	 * Registers an alias for the given command. This will allow users to use the
	 * given alias to invoke the given command.
	 * 
	 * @param command the command name.
	 * @param alias   the alias.
	 * @throws CommandRegisterException thrown if the alias is already registered or
	 *                                  if there no command with the given name.
	 * @see CommandRegistry#registerAlias(ICommand, String)
	 */
	public synchronized void registerAlias(String command, String alias) throws CommandRegisterException {
		ICommand com = commands.get(command);
		if (com == null) {
			throw new CommandRegisterException(alias, true,
					new NullPointerException("No entry for command: " + command));
		}
		registerAlias(com, alias);
	}

	/**
	 * Registers alias for the given command. This will allow users to use any of
	 * the given aliases to invoke the given command.
	 * 
	 * @param command the command.
	 * @param aliases the aliases.
	 * @throws CommandRegisterException thrown if any of the given aliases are
	 *                                  already registered.
	 */
	public synchronized void registerAliases(ICommand command, String... aliases) throws CommandRegisterException {
		for (String alias : aliases) {
			registerAlias(command, alias);
		}
	}

	/**
	 * Registers alias for the given command. This will allow users to use any of
	 * the given aliases to invoke the given command.
	 * 
	 * @param command the command name.
	 * @param aliases the aliases.
	 * @throws CommandRegisterException thrown if (1) any of the given aliases are
	 *                                  already registered or (2) if there is no
	 *                                  command with the given name.
	 */
	public synchronized void registerAliases(String command, String... aliases) throws CommandRegisterException {
		for (String alias : aliases) {
			registerAlias(command, alias);
		}
	}

	/**
	 * Unregisters the given command alias.
	 * 
	 * @param alias the alias.
	 * @return the command name previously associated with the given alias.
	 */
	public synchronized String unregisterAlias(String alias) {
		return aliases.remove(alias.toLowerCase());
	}

	/**
	 * Unregisters the given command aliases.
	 * 
	 * @param aliases the aliases.
	 * @return the aliases that were not already registered.
	 */
	public synchronized String[] unregisterAliases(String... aliases) {
		List<String> failed = new ArrayList<String>();
		for (String s : aliases) {
			String command = unregisterAlias(s);
			if (command == null) {
				failed.add(command);
			}
		}
		return failed.toArray(new String[failed.size()]);
	}

	/**
	 * Removes the given command from the registry.
	 * 
	 * @param command the command to remove.
	 * @return the command removed.
	 * @see CommandRegistry#unregisterCommand(String)
	 * @see CommandRegistry#registerCommand(ICommand)
	 */
	public synchronized ICommand unregisterCommand(ICommand command) {
		return unregisterCommand(command.getName());
	}

	/**
	 * Removes the command with the given name from the registry.
	 * 
	 * @param name the name of the command to remove.
	 * @return the command removed.
	 * @see CommandRegistry#unregisterCommand(ICommand)
	 * @see CommandRegistry#registerCommand(ICommand)
	 */
	public synchronized ICommand unregisterCommand(String name) {
		ICommand command = commands.remove(name.toLowerCase());
		if (command != null) {
			CommandClient.LOGGER.info("Unregistered command: " + command.getName());
		}
		return command;
	}

	/**
	 * Removes the given commands from the registry.
	 * 
	 * @param commands the commands to remove.
	 * @return the commands that were not already registered.
	 * @see CommandRegistry#unregisterCommand(ICommand)
	 * @see CommandRegistry#unregisterCommand(String)
	 * @see CommandRegistry#registerCommands(ICommand...)
	 */
	public synchronized ICommand[] unregisterCommands(ICommand... commands) {
		List<ICommand> failed = new ArrayList<ICommand>();
		for (ICommand c : commands) {
			ICommand command = unregisterCommand(c);
			if (command == null) {
				failed.add(command);
			}
		}
		return failed.toArray(new ICommand[failed.size()]);
	}

	/**
	 * Removes the commands with the given names from the registry.
	 * 
	 * @param names the names of the commands to remove.
	 * @return the command names that were not already registered.
	 * @see CommandRegistry#unregisterCommand(String)
	 * @see CommandRegistry#registerCommands(ICommand...)
	 */
	public synchronized String[] unregisterCommands(String... names) {
		List<String> failed = new ArrayList<String>();
		for (String s : names) {
			ICommand command = unregisterCommand(s);
			if (command == null) {
				failed.add(s);
			}
		}
		return failed.toArray(new String[failed.size()]);
	}

	/**
	 * Gets the command with the given name. If there is no command with a matching
	 * name, then the aliases mappings will be checked for a match. If there no
	 * matching alias, then this will return null.
	 * 
	 * @param name the name of the command.
	 * @return the command with the given name, or null.
	 * @see CommandRegistry#registerCommand(ICommand)
	 * @see CommandRegistry#unregisterCommand(String)
	 */
	public synchronized ICommand getCommand(String name) {
		if (commands.containsKey(name.toLowerCase())) {
			return commands.get(name.toLowerCase());
		} else if (aliases.containsKey(name.toLowerCase())) {
			return commands.get(aliases.get(name.toLowerCase()));
		} else {
			return null;
		}
	}

	/**
	 * Gets the commands with the given names. If any of the names are not in the
	 * command mappings, then those indices will be null
	 * 
	 * @param names the names of the commands.
	 * @return an array of commands.
	 */
	public synchronized ICommand[] getCommands(String... names) {
		ICommand[] commands = new ICommand[names.length];
		for (int i = 0; i < commands.length; i++) {
			commands[i] = this.commands.get(names[i]);
		}
		return commands;
	}

	/**
	 * Gets all the registered commands.
	 * 
	 * @return the registered commands.
	 */
	public synchronized Collection<ICommand> getCommands() {
		return commands.values();
	}

	/**
	 * Gets the names of all the registered commands.
	 * 
	 * @return the names.
	 */
	public synchronized Set<String> getCommandNames() {
		return commands.keySet();
	}

	/**
	 * Gets all the aliases associated with the given command.
	 * 
	 * @param command the command.
	 * @return the aliases.
	 */
	public synchronized Set<String> getAliases(ICommand command) {
		return getAliases(command.getName());
	}

	/**
	 * Gets all the aliases associated with the given command.
	 * 
	 * @param command the command name.
	 * @return the aliases.
	 */
	public synchronized Set<String> getAliases(String command) {
		return aliases.entrySet().stream().filter(e -> e.getValue().equals(command.toLowerCase())).map(e -> e.getKey())
				.collect(Collectors.toSet());
	}

	/**
	 * Gets a read-only mapping of command aliases.
	 * 
	 * @return the command aliases mappings.
	 */
	public synchronized Map<String, String> getAliasesMapping() {
		return Collections.unmodifiableMap(aliases);
	}
}
