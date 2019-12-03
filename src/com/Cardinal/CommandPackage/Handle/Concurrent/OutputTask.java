package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.io.File;

public class OutputTask {
	private byte[] data;
	private File destination;
	private boolean append;

	public OutputTask(File destination, byte[] data, boolean append) {
		super();
		this.data = data;
		this.destination = destination;
		this.append = append;
	}

	public byte[] getData() {
		return data;
	}

	public File getDestination() {
		return destination;
	}

	public boolean shouldAppend() {
		return append;
	}
}
