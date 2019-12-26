package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.util.concurrent.LinkedBlockingQueue;

import com.Cardinal.CommandPackage.Handle.Command.CommandRegistry;
import com.Cardinal.CommandPackage.Impl.CommandClient;

/**
 * A thread executor used for executing
 * {@linkplain MessageReceivedEventHandler}s. Really, this should only be used
 * by the command pakcage's build-in event adapter.
 * 
 * @author Cardinal System
 *
 */
public class EventHandlerPool extends Thread implements ExecutionListener {

	private CommandRegistry registry;
	private LinkedBlockingQueue<MessageReceivedEventHandler> queue = new LinkedBlockingQueue<MessageReceivedEventHandler>();
	private boolean run = true, block = false;
	private int running = 0, max;
	private Object lock = new Object();
	public long startTime = 0;

	public EventHandlerPool(CommandRegistry registry, int maxThreadPoolSize) {
		this.registry = registry;
		max = maxThreadPoolSize;
		setName("EventHandlerPool:" + hashCode());
	}

	public boolean isDraining() {
		return block;
	}

	public void add(MessageReceivedEventHandler handler) {
		queue.add(handler);
	}

	public void requestStop() {
		run = false;
	}

	@Override
	public synchronized void start() {
		startTime = System.nanoTime();
		super.start();
	}

	@Override
	public void run() {
		while (run) {
			if (running >= max) {
				block = true;
				try {
					drain();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				block = false;
			}
			try {
				MessageReceivedEventHandler handler = queue.take();

				if (!handler.isReady())
					handler.ready(registry);
				handler.start();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public synchronized void drain() throws InterruptedException {
		if (running == 0)
			return;
		CommandClient.LOGGER.fine("Draining...");
		synchronized (lock) {
			lock.wait();
		}
	}

	@Override
	public void executionPerformed(ConcurrentEventHandler context, boolean postExecution) {
		if (postExecution) {
			running--;
		} else {
			running++;
		}
		if (running == 0) {
			lock.notifyAll();
		}
	}
}
