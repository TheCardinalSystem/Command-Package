package com.Cardinal.CommandPackage.Handle.Command;

import com.Cardinal.CommandPackage.Command.ICommand;

/**
 * A listener class that allows other classes to listen for command registry
 * events.
 * 
 * @author Cardinal System
 *
 */
public abstract class CommandRegisterListener {

	/**
	 * Executed when a command is registered.
	 * 
	 * @param command the command
	 */
	public abstract void commandRegistered(ICommand command);

	/**
	 * Executed when a command alias is registered.
	 * 
	 * @param alias   the alias
	 * @param command the command
	 */
	public abstract void aliasRegistered(String alias, ICommand command);

	/**
	 * Executed when a command is unregistered.
	 * 
	 * @param command the command
	 */
	public abstract void commandUnregistered(ICommand command);


	/**
	 * Executed when a command alias is unregistered.
	 * 
	 * @param alias   the alias
	 * @param command the command
	 */
	public abstract void aliasUnregistered(String alias, ICommand command);

}
