package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.util.Set;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;

public class WaitingEventHandler extends Thread {

	private Event event;
	private Set<EventListener> listeners;

	public WaitingEventHandler(Event event, Set<EventListener> listeners) {
		this.event = event;
		this.listeners = listeners;
	}

	@Override
	public void run() {
		synchronized (event) {
			try {
				event.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		for (EventListener listener : listeners) {
			listener.onEvent(event);
		}
	}

}
