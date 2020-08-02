package com.Cardinal.CommandPackage.Handle.Event;

import java.lang.Thread.State;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Concurrent.EventHandlerPool;
import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;
import com.Cardinal.CommandPackage.Handle.Concurrent.WaitingEventHandler;
import com.Cardinal.CommandPackage.Handle.Entity.ListEmbedManager;
import com.Cardinal.CommandPackage.Handle.Properties.GuildProperties;
import com.Cardinal.CommandPackage.Handle.Properties.PropertiesHandler;
import com.Cardinal.CommandPackage.Util.ReactionUtils;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;

/**
 * A class used for directing JDA events to their proper handlers.
 * 
 * @author Cardinal System
 *
 */
public class EventAdapter implements EventListener {

	private CommandRegistry registry;
	private EventHandlerPool pool;
	private Set<EventListener> listeners = new HashSet<EventListener>();
	private ArrayDeque<GenericEvent> deque = new ArrayDeque<GenericEvent>();
	private int poolSize;
	private long timeout;
	private BiConsumer<Exception, MessageReceivedEvent> errorHandler = null;
	private boolean logMessages;

	public EventAdapter(CommandRegistry registry, int maxThreadPoolSize, long listenerThreadTimeout,
			boolean logMessages, EventListener... listeners) {
		this.registry = registry;
		this.poolSize = maxThreadPoolSize / 2;
		this.timeout = listenerThreadTimeout;
		this.listeners.addAll(Arrays.asList(listeners));
		this.logMessages = logMessages;
		pool = new EventHandlerPool(this.registry, poolSize);
	}

	public EventAdapter(CommandRegistry registry, BiConsumer<Exception, MessageReceivedEvent> errorHandler,
			int maxThreadPoolSize, long listenerThreadTimeout, boolean logMessages, EventListener... listeners) {
		this.errorHandler = errorHandler;
		this.registry = registry;
		this.poolSize = maxThreadPoolSize / 2;
		this.timeout = listenerThreadTimeout;
		this.listeners.addAll(Arrays.asList(listeners));
		this.logMessages = logMessages;
		pool = new EventHandlerPool(this.registry, poolSize);
	}

	public void addListener(EventListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(EventListener listener) {
		listeners.remove(listener);
	}

	public synchronized void updateRegistry(CommandRegistry registry) {
		this.registry = registry;
	}

	public Set<EventListener> getEventListeners() {
		return Collections.unmodifiableSet(listeners);
	}

	public void onMessageEvent(MessageReceivedEvent event) {
		pool.add(errorHandler == null ? new MessageReceivedEventHandler(event, logMessages)
				: new MessageReceivedEventHandler(event, logMessages, errorHandler));
		if (pool.getState().equals(State.NEW))
			pool.start();
	}

	public void onReactionAddEvent(MessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			String id = event.getMessageId();
			if (ListEmbedManager.isListEmbed(id)) {
				event.getChannel().retrieveMessageById(id).queue(message -> {
					ListEmbedManager.changeEmbed(message,
							ReactionUtils.getListEmbedOption(event.getReactionEmote().getName()));
				});
			}
		}

		synchronized (event) {
			event.notifyAll();
		}
	}

	public void onTextChannelDeletionEvent(TextChannelDeleteEvent event) {
		String id = PropertiesHandler.getGuildProperty(event.getGuild(), GuildProperties.BOT_CHANNEL);
		if (id != null) {
			if (event.getChannel().getId().equalsIgnoreCase(id)) {
				PropertiesHandler.removeGuildProperty(event.getGuild(), GuildProperties.BOT_CHANNEL);
				TextChannel defaultChan = event.getGuild().getDefaultChannel();
				TextChannel systemChan = event.getGuild().getSystemChannel();
				if (defaultChan.canTalk()) {
					defaultChan.sendMessage(event.getGuild().getOwner().getAsMention() + " The bot channel "
							+ event.getChannel().getName() + " was deleted. I am now listening on all channels.")
							.queue();
				} else if (systemChan.canTalk()) {
					systemChan.sendMessage(event.getGuild().getOwner().getAsMention() + " The bot channel "
							+ event.getChannel().getName() + " was deleted. I am now listening on all channels.")
							.queue();
				} else {
					Optional<TextChannel> c = event.getGuild().getTextChannels().stream().filter(TextChannel::canTalk)
							.findAny();
					if (c.isPresent()) {
						c.get().sendMessage(event.getGuild().getOwner().getAsMention() + " The bot channel "
								+ event.getChannel().getName() + " was deleted. I am now listening on all channels.")
								.queue();
					}
				}
			}
		}

		synchronized (event) {
			event.notifyAll();
		}
	}

	@Override
	public void onEvent(GenericEvent event) {
		if (pool.isDraining()) {
			deque.add(event);
			return;
		} else {
			if (!deque.isEmpty()) {
				drainDeque();
			} else {
				queueEvent(event);
			}
		}
	}

	private WaitingEventHandler startWaitingHandler(GenericEvent event) {
		WaitingEventHandler handler = new WaitingEventHandler(event, timeout, listeners);
		handler.start();
		return handler;
	}

	private void queueEvent(GenericEvent event) {
		WaitingEventHandler handler = startWaitingHandler(event);

		if (event instanceof MessageReceivedEvent) {
			onMessageEvent((MessageReceivedEvent) event);
		} else if (event instanceof MessageReactionAddEvent) {
			onReactionAddEvent((MessageReactionAddEvent) event);
		} else if (event instanceof TextChannelDeleteEvent) {
			onTextChannelDeletionEvent((TextChannelDeleteEvent) event);
		} else {
			handler.interrupt();
		}
	}

	private void drainDeque() {
		for (int i = 0; i < poolSize; i++) {
			GenericEvent event = deque.poll();
			if (event == null) {
				break;
			} else {
				queueEvent(event);
			}
		}

	}

}
