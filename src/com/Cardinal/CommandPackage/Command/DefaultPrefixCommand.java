package com.Cardinal.CommandPackage.Command;

import java.util.EnumSet;

import com.Cardinal.CommandPackage.Command.Category.DefaultCategories;
import com.Cardinal.CommandPackage.Command.Category.ICategory;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Properties.GuildProperties;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A handy prefix command. This command must be added manually.
 * 
 * @author Cardinal System
 *
 */
public class DefaultPrefixCommand implements ICommand {

	@Override
	public String getName() {
		return "setPrefix";
	}

	@Override
	public ICategory getCategory() {
		return DefaultCategories.MANAGEMENT;
	}

	@Override
	public EnumSet<Permission> getPermissions() {
		return EnumSet.of(Permission.MESSAGE_ADD_REACTION);
	}

	@Override
	public ArgumentTypes[] getArgumentTypes() {
		return new ArgumentTypes[] { ArgumentTypes.STRING };
	}

	@Override
	public String[] getArgumentNames() {
		return new String[] { "prefix" };
	}

	@Override
	public String getDescription() {
		return "Set's the command prefix. This will be a character sequence that will precede every command.";
	}

	@Override
	public void execute(MessageReceivedEvent event, CommandRegistry registry, String prefix, Object... arguments) {
		String newPrefix = (String) arguments[0];
		PropertiesHandler.setGuildProperty(event.getGuild(), GuildProperties.PREFIX, newPrefix);
		event.getMessage().addReaction("\u2705").queue();
	}

}
