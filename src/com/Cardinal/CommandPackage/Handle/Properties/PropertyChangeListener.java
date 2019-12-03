package com.Cardinal.CommandPackage.Handle.Properties;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

public interface PropertyChangeListener {

	public void guildPropertyChanged(Guild guild, String propertyName, Object oldValue, Object newValue);

	public void userPropertyChanged(User user, String propertyName, Object oldValue, Object newValue);
	
	public void botPropertyChanged(String propertyName, Object oldValue, Object newValue);

}
