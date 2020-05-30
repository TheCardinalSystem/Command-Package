package com.Cardinal.CommandPackage.Command;

/**
 * A template for Discord bot commands which are not processed by the default
 * command processor.
 * 
 * @author Cardinal System
 *
 */
public abstract class ManuallyProcessedCommand implements ICommand {

	@Override
	public ArgumentTypes[] getArgumentTypes() {
		return new ArgumentTypes[] { ArgumentTypes.CUSTOM };
	}

	/**
	 * Gets the argument types for the command parameters. The best way to implement
	 * this method is to use {@link ArgumentTypes#toString()}, as most of your
	 * arguments should already exist in the {@linkplain ArgumentTypes} class.
	 * 
	 * @return a string array of the argument types for this command.
	 */
	public abstract String[] getArgumentTypesString();

}
