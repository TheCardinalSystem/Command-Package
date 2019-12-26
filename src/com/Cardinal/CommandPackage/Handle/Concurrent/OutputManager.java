package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A thread executor used for handling {@linkplain OutputTask}s.
 * 
 * @author Cardinal System
 *
 */
public class OutputManager extends Thread {

	private static boolean run = true;
	private static final LinkedBlockingQueue<OutputTask> TASKS = new LinkedBlockingQueue<OutputTask>();
	public long startTime = 0;

	static {
		new OutputManager().start();
	}

	private OutputManager() {
		super("OutputManager:Waiting");
	}

	/**
	 * Queues the specified {@link OutputTask} at the tail of this executor's queue,
	 * waiting if necessary for space to become available.
	 * 
	 * @param task the task.
	 * @throws InterruptedException if interrupted while waiting
	 */
	public static void queue(OutputTask task) throws InterruptedException {
		TASKS.put(task);
	}

	public static void close() {
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
			try {
				OutputTask task = TASKS.take();
				setName("OutputManager:" + task.getDestination().getName());
				process(task);
				if (TASKS.isEmpty()) {
					setName("OutputManager:Waiting");
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		TASKS.forEach(t -> {
			try {
				process(t);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private static synchronized void process(OutputTask task) throws IOException {
		File f = task.getDestination();
		if (!f.exists()) {
			f.getParentFile().mkdirs();
		}

		FileOutputStream stream = new FileOutputStream(f, task.shouldAppend());
		stream.write(task.getData());
		stream.flush();
		stream.close();
	}

}