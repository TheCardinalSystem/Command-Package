package com.Cardinal.CommandPackage.Handle.Properties;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

/**
 * A template for a listener which notified when a bot, guild, or user property
 * changes.
 * 
 * @author Cardinal System
 *
 */
public interface PropertyChangeListener {

	public void guildPropertyChanged(Guild guild, String propertyName, Object oldValue, Object newValue);

	public void guildPropertyChanged(long guildID, String propertyName, Object oldValue, Object newValue);

	public void userPropertyChanged(User user, String propertyName, Object oldValue, Object newValue);

	public void userPropertyChanged(long userID, String propertyName, Object oldValue, Object newValue);

	public void botPropertyChanged(String propertyName, Object oldValue, Object newValue);

}
