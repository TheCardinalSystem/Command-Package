package com.Cardinal.CommandPackage.Command;

import com.Cardinal.CommandPackage.Command.Category.ICategory;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A subinterface of {@link ICommand} which defines a command of special access.
 * Special access commands are executed even if
 * {@linkplain ICategory#canAccess(Member)} returns false. The
 * {@linkplain ICommand#execute(MessageReceivedEvent, CommandRegistry, String, Object...)}
 * method should handle access to these commands.
 * 
 * @author Cardinal System
 *
 */
public interface ISpecialAccesCommand extends ICommand {

}
