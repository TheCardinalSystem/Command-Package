package com.Cardinal.CommandPackage.Impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Event.EventAdapter;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class CommandClient {

	public static Logger LOGGER = Logger.getLogger("CommandPackage");
	public static String DEFAULT_PREFIX;
	public static List<String> DEVELOPER_IDS;

	private JDA jda;
	private CommandRegistry registry;
	private EventAdapter adapter;

	/**
	 * Constructs a {@link CommandClient} with the given parameters.
	 * 
	 * @param token             the bot OAuth2 token.
	 * @param registry          the {@link CommandRegistry} which this bot will be
	 *                          using for commands.
	 * @param workingDirectory  the directory which this bot will be working out of.
	 * @param maxThreadPoolSize The maximum number of the threads that can be
	 *                          running at any given time.
	 * @param timeout           The default timeout for threads waiting for events
	 *                          to be processed.
	 * @param listeners         any {@linkplain EventListener}s which are to process
	 *                          events *after* they have been processed by the
	 *                          CommandPackage.
	 * @param errorHandler      a {@link BiConsumer} used for handling exceptions
	 *                          related to {@linkplain MessageReceivedEvent}s.
	 * @param developers        The user IDs of any developers working on this bot.
	 * @throws LoginException       If the provided token is invalid.
	 * @throws InterruptedException from {@link JDA#awaitReady()}
	 */
	private CommandClient(String token, CommandRegistry registry, File workingDirectory, int maxThreadPoolSize,
			long timeout, EventListener[] listeners, BiConsumer<Exception, MessageReceivedEvent> errorHandler,
			String[] developers) throws LoginException, InterruptedException {
		PropertiesHandler.init(workingDirectory);

		jda = new JDABuilder().setToken(token).build().awaitReady();
		DEVELOPER_IDS = Arrays.asList(developers);
		DEFAULT_PREFIX = jda.getSelfUser().getAsMention().replaceFirst("!", "");
		this.registry = registry;
		adapter = errorHandler == null ? new EventAdapter(registry, maxThreadPoolSize, timeout, listeners)
				: new EventAdapter(registry, errorHandler, maxThreadPoolSize, timeout, listeners);
		jda.addEventListener(adapter);
		jda.addEventListener((Object[]) listeners);
	}

	/**
	 * Gets this bot's JDA instance.
	 * 
	 * @return the JDA.
	 */
	public JDA getJDA() {
		return jda;
	}

	/**
	 * Changes the command registry which this bot uses for commands.
	 * 
	 * @param registry the registry.
	 */
	public void changeCommandRegistry(CommandRegistry registry) {
		this.registry = registry;
		adapter.updateRegistry(this.registry);
	}

	/**
	 * Adds an event listener to this bot.
	 * 
	 * @param listener a listener.
	 */
	public void addEventListener(EventListener listener) {
		this.adapter.addListener(listener);
	}

	/**
	 * Removes an event listener to this bot.
	 * 
	 * @param listener a listener.
	 */
	public void removeEventListener(EventListener listener) {
		this.adapter.removeListener(listener);
	}

	/**
	 * A class used for building a {@link CommandClient} instance with chaining.
	 * 
	 * @author Cardinal System
	 *
	 */
	public static class CommandClientBuilder {
		private String token;
		private List<String> devs = new ArrayList<String>();
		private List<EventListener> listeners = new ArrayList<EventListener>();
		private CommandRegistry registry;
		private File directory;
		private BiConsumer<Exception, MessageReceivedEvent> consumer;
		/**
		 * The maximum number of the threads that can be running at any given time.
		 */
		private int maxThreadPoolSize = 30;
		/**
		 * The default timeout for threads waiting for events to be processed.
		 */
		private long timeout = 10000;

		/**
		 * Sets the bot token for this bot.
		 * 
		 * @param token the token.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withToken(String token) {
			this.token = token;
			return this;
		}

		/**
		 * Sets the maximum size for thread executor pools (used to prevent
		 * {@linkplain OutOfMemoryError}s).
		 * 
		 * @param size the size.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withMaxThreadPoolSize(int size) {
			this.maxThreadPoolSize = size;
			return this;
		}

		/**
		 * Sets the timeout for threads waiting for events to be processed. If this is
		 * not invoked, the timeout will be set to default 10 seconds.
		 * 
		 * @param timeout the timeout, in miliseconds.
		 * @return the {@link CommandClientBuilder}, for chaining.
		 */
		public CommandClientBuilder withThreadTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Sets the command registry which this bot will be using for commands.
		 * 
		 * @param registry the registry.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withRegistry(CommandRegistry registry) {
			this.registry = registry;
			return this;
		}

		/**
		 * Adds a developer ID to the list of developers. This is used for handling
		 * permissions.
		 * 
		 * @param id the developer ID.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withDeveloper(String id) {
			devs.add(id);
			return this;
		}

		/**
		 * Adds an {@link EventListener} to be added to the bot once it is constructed.
		 * 
		 * @param listener a listener.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withListener(EventListener listener) {
			listeners.add(listener);
			return this;
		}

		/**
		 * Sets the directory where the entire bot will work from.
		 * 
		 * @param directory the directory.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withWorkingDirectory(File directory) {
			this.directory = directory;
			return this;
		}

		/**
		 * Sets the error handler.
		 * 
		 * @param handler a {@link BiConsumer} used for handling exceptions related to
		 *                {@linkplain MessageReceivedEvent}s.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withErrorHandler(BiConsumer<Exception, MessageReceivedEvent> handler) {
			this.consumer = handler;
			return this;
		}

		public CommandClient build() throws IllegalStateException, LoginException, InterruptedException {
			if (token == null)
				throw new IllegalStateException("Cannot build without a token.");
			if (registry == null)
				throw new IllegalStateException("Cannot build without a command registry.");

			return new CommandClient(token, registry, directory, maxThreadPoolSize, timeout,
					listeners.toArray(new EventListener[listeners.size()]), consumer,
					devs.toArray(new String[devs.size()]));
		}
	}

}
