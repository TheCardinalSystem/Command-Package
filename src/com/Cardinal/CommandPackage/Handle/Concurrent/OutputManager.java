package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class OutputManager extends Thread {

	private static boolean run = true;
	private static final LinkedBlockingQueue<OutputTask> TASKS = new LinkedBlockingQueue<OutputTask>();

	static {
		new OutputManager().start();
	}

	public static void queue(OutputTask task) throws InterruptedException {
		TASKS.put(task);
	}

	public static void close() {
		run = false;
	}

	@Override
	public void run() {
		while (run) {
			if (!TASKS.isEmpty()) {
				OutputTask task = TASKS.poll();
				try {
					process(task);
				} catch (IOException e) {
					e.printStackTrace();
				}
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