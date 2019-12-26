package com.Cardinal.CommandPackage.Handle.Command;

import com.Cardinal.CommandPackage.Command.ICommand;

/**
 * An exception used to indicate a problem with registering a command.
 * 
 * @author Cardinal System
 *
 */
public class CommandRegisterException extends Exception {

	private static final long serialVersionUID = 1L;

	public CommandRegisterException(ICommand command) {
		super("Failed to register command: " + command.getName());
	}

	public CommandRegisterException(String command) {
		super("Failed to register command: " + command);
	}

	public CommandRegisterException(ICommand command, Throwable cause) {
		super("Failed to register command: " + command.getName(), cause);
	}

	public CommandRegisterException(String command, Throwable cause) {
		super("Failed to register command: " + command, cause);
	}

	public CommandRegisterException(String command, boolean alias) {
		super(alias ? "Failed to register alias: " + command : "Failed to register command: " + command);
	}

	public CommandRegisterException(String command, boolean alias, Throwable cause) {
		super(alias ? "Failed to register alias: " + command : "Failed to register command: " + command, cause);
	}
}
