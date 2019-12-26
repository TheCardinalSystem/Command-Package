package com.Cardinal.CommandPackage.Handle.Concurrent;

import java.io.File;

/**
 * A class used to construct object representations of file writing/appending
 * tasks, which are handled concurrently.
 * 
 * @author Cardinal System
 * @see OutputManager
 */
public class OutputTask {
	private byte[] data;
	private File destination;
	private boolean append;

	/**
	 * Constructs a new {@link OutputTask}.
	 * 
	 * @param destination the file to write to.
	 * @param data        the data to write.
	 * @param append      used to determine whether or not to overwrite the file.
	 */
	public OutputTask(File destination, byte[] data, boolean append) {
		super();
		this.data = data;
		this.destination = destination;
		this.append = append;
	}

	/**
	 * Get's the file where this task's data needs to be written.
	 * 
	 * @return the file.
	 */
	public File getDestination() {
		return destination;
	}

	/**
	 * Get's the data that needs to be written.
	 * 
	 * @return the data.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Indicates whether or not the destination file should be appended or
	 * overwritten.
	 * 
	 * @return <b>true</b> : the file data should be appended.<br>
	 *         <b>false</b> : the file's data should overwritten.
	 */
	public boolean shouldAppend() {
		return append;
	}
}
