/**
 * 
 */
package org.jenkinsci.plugins.sqlplus.script.runner;

/**
 * {@link SQLPlusRunner} exception.
 */
public class SQLPlusRunnerException extends Exception {

	/*
	 * serial UID.
	 */
	private static final long serialVersionUID = -8584046202815156649L;

	/**
	 * Default constructor.
	 */
	public SQLPlusRunnerException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Constructor with message.
	 * @param message message
	 */
	public SQLPlusRunnerException(String message) {
		super(message);
	}

	/**
	 * Constructor with cause.
	 * @param cause cause
	 */
	public SQLPlusRunnerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor with message and cause.
	 * @param message message
	 * @param cause cause
	 */
	public SQLPlusRunnerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor with message, cause and flags.
	 * @param message message
	 * @param cause cause
	 * @param enableSuppression enable suppression flag
	 * @param writableStackTrace writable stack trace flag
	 */
	public SQLPlusRunnerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
