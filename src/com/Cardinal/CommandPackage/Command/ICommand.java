package com.Cardinal.CommandPackage.Command;

import com.Cardinal.CommandPackage.Command.Category.DefaultCategories;
import com.Cardinal.CommandPackage.Command.Category.ICategory;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Concurrent.WaitingEventHandler;
import com.Cardinal.CommandPackage.Impl.CommandClient.CommandClientBuilder;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * An template for Discord bot commands.
 * 
 * @author Cardinal System
 * @see CommandRegistry
 */
public interface ICommand {

	/**
	 * Gets the command name. This is the name that will be matched with the command
	 * and used to invoke it. For example, if your command prefix is <i>$</i>, and
	 * your command name is <i>ban</i>, then the command will be invoked when a user
	 * types {@code $ban} .
	 * 
	 * @return the command name.
	 */
	public String getName();

	/**
	 * Gets this command's category. This method is generally used by help commands
	 * for sorting command lists.
	 * 
	 * @return this command's category.
	 * @see DefaultCategories
	 */
	public ICategory getCategory();

	/**
	 * Gets the argument types for the command parameters. If an argument is
	 * optional, use {@link ArgumentTypes#optional()}. If your command takes special
	 * arguments that cannot be processed by this library, then make sure to include
	 * {@link ArgumentTypes#CUSTOM} at the end of this array. If the command does
	 * not take any arguments, this method can return null.<br>
	 * <br>
	 * NOTE: {@link ArgumentTypes#CUSTOM} is not used for custom arguments. If you
	 * want your command to take a special/custom argument, then use
	 * {@link ArgumentTypes#STRING}, and parse the argument yourself in
	 * {@link ICommand#execute(MessageReceivedEvent, CommandRegistry, String, Object...)}.<br>
	 * <br>
	 * NOTE: If you use {@link ArgumentTypes#CUSTOM}, your command will not show any
	 * arguments on the default help command unless it extends
	 * {@link ManuallyProcessedCommand}.<br>
	 * <br>
	 * NOTE: Array and optional types must be the <i>last</i> arguments in the
	 * arguments array. Each command may only have one optional argument. Command
	 * may not have both array and optional arguments.
	 * 
	 * 
	 * @return the argument types, or null.
	 */
	public ArgumentTypes[] getArgumentTypes();

	/**
	 * Gets the name of each argument in order. This method is generally used for
	 * making help commands. It is also used when a user issues this command with
	 * the wrong arguments. If your command does not take any arguments, then this
	 * can return null.
	 * 
	 * @return the argument names, or null.
	 */
	public String[] getArgumentNames();

	/**
	 * Gets the description of this command. This method is generally used for
	 * making help commands. If you do not have a help command, then this can return
	 * null.
	 * 
	 * @return the command description, or null.
	 */
	public String getDescription();

	/**
	 * Executes this command. {@link ICommand#getArgumentTypes()} will be invoked on
	 * the command instance before this method is called. If it returns an array
	 * with at least one element, then the message which triggered the command will
	 * be processed into an array of objects such that each element will have the
	 * data type defined in the corresponding argument-types array, and will passed
	 * to this method upon invocation. In other words, you don't have to manually
	 * divide the message and parse the various arguments. All you to do is cast
	 * each element from the arguments array passed to this method.<br>
	 * <br>
	 * NOTE: It is highly discouraged to use {@link Thread#sleep(long)},
	 * {@link Object#wait()}, or any other form of blocking in this method. Threads
	 * which block longer than than the timeout specified in
	 * {@link CommandClientBuilder#withThreadTimeout(long)} will cause the
	 * {@link WaitingEventHandler} assigned to that event to execute.
	 * 
	 * @param event     the event that triggered this command. This is provided for
	 *                  commands that need more than just arguments to execute.
	 * @param registry  the command registry which this command is registered in.
	 *                  This parameter is generally used by help commands.
	 * @param prefix    The command prefix the guild which the event originated
	 *                  from.
	 * @param arguments the user-provided arguments, or null.
	 * @throws Exception Any unhandled exceptions will be caught by the executor and
	 *                   handled properly.
	 */
	public void execute(MessageReceivedEvent event, CommandRegistry registry, String prefix, Object... arguments)
			throws Exception;
}
