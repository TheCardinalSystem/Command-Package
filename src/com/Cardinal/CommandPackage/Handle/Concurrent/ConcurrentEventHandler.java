package com.Cardinal.CommandPackage.Handle.Concurrent;

/**
 * A Discord event handler thread template used to track thread running times
 * and to prevent {@linkplain OutOfMemoryError}s.
 * 
 * @author Cardinal System
 *
 */
public abstract class ConcurrentEventHandler extends Thread {

	public long startTime = 0;

	public ConcurrentEventHandler() {
		super();
		setName("ConcurrentEventHandler:" + hashCode());
	}

	public ConcurrentEventHandler(Runnable target, String name) {
		super(target, name);
	}

	public ConcurrentEventHandler(Runnable target) {
		super(target);
		setName("ConcurrentEventHandler:" + hashCode());
	}

	public ConcurrentEventHandler(String name) {
		super(name);
	}

	public ConcurrentEventHandler(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
	}

	public ConcurrentEventHandler(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	public ConcurrentEventHandler(ThreadGroup group, Runnable target) {
		super(group, target);
		setName("ConcurrentEventHandler:" + hashCode());
	}

	public ConcurrentEventHandler(ThreadGroup group, String name) {
		super(group, name);
	}

	@Override
	public synchronized void start() {
		startTime = System.nanoTime();
		super.start();
	}

	/**
	 * Adds an {@link ExecutionListener} to this thread to be invoked when the
	 * thread starts/stops. These handlers must be manually invoked in your
	 * {@link Thread#run()} implementation.
	 * 
	 * @param listener the listener.
	 */
	public abstract void addExecutionListener(ExecutionListener listener);

}
