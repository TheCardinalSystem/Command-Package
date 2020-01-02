package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.util.Set;

import com.Cardinal.CommandPackage.Impl.CommandClient.CommandClientBuilder;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

/**
 * A thread which waits for an event to be processed before passing it on to the
 * {@linkplain EventListener}s specified in
 * {@link CommandClientBuilder#withListener(EventListener)}. This class is used
 * to ensure that the command package can properly handle events before user
 * implementations change the messages, channels, guilds, etc. involved in the
 * events.
 * 
 * @author Cardinal System
 *
 */
public class WaitingEventHandler extends Thread {

	private GenericEvent event;
	private Set<EventListener> listeners;
	private long timeout;
	public long startTime;

	public WaitingEventHandler(GenericEvent event, long timeout, Set<EventListener> listeners) {
		super("WaitingEventHandler:" + event.getClass().getSimpleName() + ":" + event.hashCode());
		this.event = event;
		this.listeners = listeners;
		this.timeout = timeout;
	}

	@Override
	public synchronized void start() {
		startTime = System.nanoTime();
		super.start();
	}

	@Override
	public void run() {
		synchronized (event) {
			try {
				event.wait(timeout);
			} catch (InterruptedException e) {
			}
		}

		for (EventListener listener : listeners) {
			listener.onEvent(event);
		}
	}

}
