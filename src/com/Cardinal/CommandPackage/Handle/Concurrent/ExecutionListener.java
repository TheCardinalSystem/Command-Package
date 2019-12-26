package com.Cardinal.CommandPackage.Handle.Concurrent;

/**
 * A template for a listener which is notified when a
 * {@link ConcurrentEventHandler} starts/stops execution.
 * 
 * @author Cardinal System
 *
 */
public interface ExecutionListener {

	/**
	 * Invoked when the {@linkplain ConcurrentEventHandler}(s) which this listener
	 * is attached to starts/stops.
	 * 
	 * @param context       the handler which started/stopped.
	 * @param postExecution used to indicate whether the thread has stopped or not.
	 */
	public void executionPerformed(ConcurrentEventHandler context, boolean postExecution);

}
