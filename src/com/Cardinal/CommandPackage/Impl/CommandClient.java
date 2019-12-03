package com.Cardinal.CommandPackage.Impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Event.EventAdapter;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.hooks.EventListener;

public class CommandClient {

	public static String DEFAULT_PREFIX;
	public static List<String> DEVELOPER_IDS;

	private JDA jda;
	private CommandRegistry registry;
	private EventAdapter adapter;

	private CommandClient(String token, CommandRegistry registry, File workingDirectory, EventListener[] listeners,
			String[] developers) throws LoginException, InterruptedException {
		PropertiesHandler.init(workingDirectory);

		jda = new JDABuilder().setToken(token).build().awaitReady();
		DEVELOPER_IDS = Arrays.asList(developers);
		DEFAULT_PREFIX = jda.getSelfUser().getAsMention();
		this.registry = registry;
		adapter = new EventAdapter(registry, listeners);
		jda.addEventListener(adapter);
		jda.addEventListener((Object[]) listeners);
	}

	public JDA getJDA() {
		return jda;
	}

	public void changeCommandRegistry(CommandRegistry registry) {
		this.registry = registry;
		adapter.updateRegistry(this.registry);
	}

	public void addEventListener(EventListener listener) {
		this.adapter.addListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		this.adapter.removeListener(listener);
	}

	public static class CommandClientBuilder {
		private String token;
		private List<String> devs = new ArrayList<String>();
		private List<EventListener> listeners = new ArrayList<EventListener>();
		private CommandRegistry registry;
		private File directory;

		public CommandClientBuilder withToken(String token) {
			this.token = token;
			return this;
		}

		public CommandClientBuilder withRegistry(CommandRegistry registry) {
			this.registry = registry;
			return this;
		}

		public CommandClientBuilder withDeveloper(String id) {
			devs.add(id);
			return this;
		}

		public CommandClientBuilder withListener(EventListener listener) {
			listeners.add(listener);
			return this;
		}

		public CommandClientBuilder withWorkingDirectory(File directory) {
			this.directory = directory;
			return this;
		}

		public CommandClient build() throws IllegalStateException, LoginException, InterruptedException {
			if (token == null)
				throw new IllegalStateException("Cannot build without a token.");
			if (registry == null)
				throw new IllegalStateException("Cannot build without a command registry.");

			return new CommandClient(token, registry, directory, listeners.toArray(new EventListener[listeners.size()]),
					devs.toArray(new String[devs.size()]));
		}
	}

}
