package com.Cardinal.CommandPackage.Impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import com.Cardinal.CommandPackage.Command.ICommand;
import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Event.EventAdapter;
import com.Cardinal.CommandPackage.Handle.Properties.GuildProperties;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class CommandClient {

	public static Logger LOGGER = Logger.getLogger("CommandPackage");
	public static String DEFAULT_PREFIX;
	public static List<String> DEVELOPER_IDS;

	private JDA jda;
	private JDABuilder builder;
	private CommandRegistry registry;
	private EventAdapter adapter;

	static {
		System.err.println(
				"---Command-Package---\n(https://github.com/TheCardinalSystem/Command-Package)\nAuthor: Cardinal System\nVersion: "
						+ Updater.CURRENT_VERSION
						+ "\nCredits: Powered by JDA (https://github.com/DV8FromTheWorld/JDA)\n- Apache Commons Lang (https://commons.apache.org/proper/commons-lang/)\n- Apache Commons Text (https://commons.apache.org/proper/commons-text/)\n- Gson (https://github.com/google/gson)\n- JSoup (https://jsoup.org/)\n---Command-Package---");

		try {
			String url = Updater.checkForUpdates();
			if (url != null) {
				LOGGER.log(Level.INFO, "\n\tAn update is available. Please download the update from: " + url + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
			String[] developers, boolean logMessages, Collection<GatewayIntent> intents)
			throws LoginException, InterruptedException {
		PropertiesHandler.init(workingDirectory);

		builder = intents.isEmpty() ? JDABuilder.createDefault(token) : JDABuilder.create(token, intents);
		jda = builder.build().awaitReady();
		DEVELOPER_IDS = Arrays.asList(developers);
		DEFAULT_PREFIX = jda.getSelfUser().getAsMention().replaceFirst("!", "");
		this.registry = registry;
		adapter = errorHandler == null ? new EventAdapter(registry, maxThreadPoolSize, timeout, logMessages, listeners)
				: new EventAdapter(registry, errorHandler, maxThreadPoolSize, timeout, logMessages, listeners);
		jda.addEventListener(adapter);
	}

	/**
	 * Gets this bot's {@link JDA} instance.
	 * 
	 * @return the JDA.
	 */
	public JDA getJDA() {
		return jda;
	}

	/**
	 * Get's this bot's {@link CommandRegistry} instance.
	 * 
	 * @return the registry.
	 */
	public CommandRegistry getRegistry() {
		return registry;
	}

	/**
	 * Generates an invite link for this bot by combining the required permissions
	 * for all the registered commands.
	 * 
	 * @return an invite link.
	 * @throws MalformedURLException don't worry, this is never thrown
	 */
	public URL generateInviteLink() throws MalformedURLException {
		Optional<EnumSet<Permission>> optional = registry.getCommands().stream().map(ICommand::getPermissions)
				.reduce((t, u) -> Stream.concat(t.stream(), u.stream())
						.collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class))));

		EnumSet<Permission> set = optional.isPresent() ? optional.get() : EnumSet.noneOf(Permission.class);

		Permission[] botPerms = PropertiesHandler.getBotProperty(PropertiesHandler.BOT_PERMISSIONS_PROPERTY,
				Permission[].class);
		if (botPerms != null) {
			set.addAll(Arrays.asList(botPerms));
		}
		return new URL("https://discordapp.com/oauth2/authorize?client_id=" + jda.getSelfUser().getId() + "&scope=bot"
				+ (set.isEmpty() ? "" : "&permissions=" + Permission.getRaw(set)));
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
	 * Dispatches the given command in the given channel as the bot user.
	 * 
	 * @param command the command to dispatch (with any arguments)
	 * @param channel the channel
	 */
	public void dispatchCommand(String command, MessageChannel channel) {
		if (channel instanceof TextChannel) {
			String prefix;
			if (!channel.getType().equals(ChannelType.PRIVATE)) {
				String obj = PropertiesHandler.<String>getGuildProperty(((TextChannel) channel).getGuild(),
						GuildProperties.PREFIX);
				prefix = obj == null ? CommandClient.DEFAULT_PREFIX : obj;
			} else {
				prefix = "";
			}
			command = prefix.toLowerCase() + command;
		}
		channel.sendMessage(command).queue();
	}

	/**
	 * Shuts down this bot. If 'now' is true, this shutdown instantly and will also
	 * cancel all queued {@link net.dv8tion.jda.api.requests.RestAction
	 * RestActions}.
	 * 
	 * @param now   now
	 * @param block tells the thread whether or not to wait until the shutdown is
	 *              complete.
	 * @throws InterruptedException if the thread wait times out.
	 */
	public void shutdown(boolean now, boolean block) throws InterruptedException {
		Object threadLock;
		Thread thread = null;
		if (block) {
			threadLock = new Object();
			thread = new Thread(() -> {
				synchronized (threadLock) {
					try {
						threadLock.wait(8000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
			adapter.addShutdownConsumer(e -> {
				synchronized (threadLock) {
					threadLock.notifyAll();
				}
			});
		}

		if (now)
			jda.shutdownNow();
		else
			jda.shutdown();

		if (thread != null)
			thread.join(5000);
	}

	/**
	 * Shuts down the internal {@link JDA} instance, if it is not already shutdown,
	 * and then starts it back up again. This method blocks until the process is
	 * complete.
	 * 
	 * @param now if true, this will shutdown the JDA instance instantly and will
	 *            also cancel all queued
	 *            {@link net.dv8tion.jda.api.requests.RestAction RestActions}.
	 * @throws InterruptedException If this thread wait times out.
	 * @throws LoginException       If the provided token is invalid.
	 */
	public void reboot(boolean now) throws LoginException, InterruptedException {
		/*
		 * Object threadLock = new Object(); Thread thread = new Thread(() -> { if
		 * (!jda.getStatus().equals(Status.SHUTDOWN) &&
		 * !jda.getStatus().equals(Status.SHUTTING_DOWN)) { shutdown(now, false); }
		 * synchronized (threadLock) { try { threadLock.wait(); } catch
		 * (InterruptedException e) { e.printStackTrace(); } } }); thread.start();
		 * adapter.addShutdownConsumer(t -> { while
		 * (!thread.getState().equals(State.WAITING)) try { Thread.sleep(1); } catch
		 * (InterruptedException e) { e.printStackTrace(); } synchronized (threadLock) {
		 * threadLock.notifyAll(); } }); try { thread.join(); } catch
		 * (InterruptedException e) { e.printStackTrace(); }
		 */

		if (!jda.getStatus().equals(Status.SHUTDOWN) && !jda.getStatus().equals(Status.SHUTTING_DOWN)) {
			shutdown(now, true);
		}
		jda = builder.build().awaitReady();
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
		private boolean logMessages = false;
		private Set<GatewayIntent> intents = new HashSet<GatewayIntent>();

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
		 * Adds a {@link GatewayIntent} to be added to the bot intents once it is
		 * constructed.
		 * 
		 * @param intent an intent.
		 * @return this, for chaining.
		 */
		public CommandClientBuilder withIntent(GatewayIntent intent) {
			intents.add(intent);
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

		/**
		 * If true, all messages sent by this bot will be logged in
		 * {@link CommandClient#LOGGER}.
		 * 
		 * @param toggle true/false
		 * @return this, for chaining.
		 */
		public CommandClientBuilder logBotMessages(boolean toggle) {
			logMessages = toggle;
			return this;
		}

		public CommandClient build() throws IllegalStateException, LoginException, InterruptedException {
			if (token == null)
				throw new IllegalStateException("Cannot build without a token.");
			if (registry == null)
				throw new IllegalStateException("Cannot build without a command registry.");

			return new CommandClient(token, registry, directory, maxThreadPoolSize, timeout,
					listeners.toArray(new EventListener[listeners.size()]), consumer,
					devs.toArray(new String[devs.size()]), logMessages, intents);
		}
	}

}
