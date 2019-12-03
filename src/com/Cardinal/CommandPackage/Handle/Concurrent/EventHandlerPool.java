package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.util.concurrent.LinkedBlockingQueue;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;

public class EventHandlerPool extends Thread {

	private CommandRegistry registry;
	private LinkedBlockingQueue<MessageReceivedEventHandler> queue = new LinkedBlockingQueue<MessageReceivedEventHandler>();
	private boolean run = true;

	public EventHandlerPool(CommandRegistry registry) {
		this.registry = registry;
		setName("EventHandlerPool " + hashCode());
	}

	public void add(MessageReceivedEventHandler handler) {
		queue.add(handler);
	}

	@Override
	public void run() {
		while (run) {
			MessageReceivedEventHandler handler;
			try {
				handler = queue.take();
				if (!handler.isReady())
					handler.ready(registry);
				handler.start();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
