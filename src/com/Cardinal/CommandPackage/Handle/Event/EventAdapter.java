package com.Cardinal.CommandPackage.Handle.Event;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Handle.Concurrent.EventHandlerPool;
import com.Cardinal.CommandPackage.Handle.Concurrent.MessageReceivedEventHandler;
import com.Cardinal.CommandPackage.Handle.Concurrent.WaitingEventHandler;
import com.Cardinal.CommandPackage.Handle.Entity.ListEmbedManager;
import com.Cardinal.CommandPackage.Util.ReactionUtils;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class EventAdapter implements EventListener {

	private CommandRegistry registry;
	private EventHandlerPool pool;
	private Set<EventListener> listeners = new HashSet<EventListener>();

	public EventAdapter(CommandRegistry registry, EventListener... listeners) {
		this.registry = registry;
		this.listeners.addAll(Arrays.asList(listeners));
		pool = new EventHandlerPool(this.registry);
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

	public void onMessageEvent(MessageReceivedEvent event) {
		pool.add(new MessageReceivedEventHandler(event));
		if (pool.getState().equals(State.NEW))
			pool.start();
	}

	public void onReactionAddEvent(MessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			String id = event.getMessageId();
			if (ListEmbedManager.isListEmbed(id)) {
				event.getChannel().getMessageById(id).queue(message -> {
					ListEmbedManager.changeEmbed(message,
							ReactionUtils.getListEmbedOption(event.getReactionEmote().getName()));
				});
			}
		}

		synchronized (event) {
			event.notifyAll();
		}
	}

	@Override
	public void onEvent(Event event) {
		WaitingEventHandler handler = new WaitingEventHandler(event, listeners);
		handler.start();

		if (event instanceof MessageReceivedEvent) {
			onMessageEvent((MessageReceivedEvent) event);
		} else if (event instanceof MessageReactionAddEvent) {
			onReactionAddEvent((MessageReactionAddEvent) event);
		} else {
			synchronized (event) {
				event.notifyAll();
			}
		}
	}

}
